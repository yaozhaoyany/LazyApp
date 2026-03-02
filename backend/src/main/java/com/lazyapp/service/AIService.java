package com.lazyapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazyapp.model.*;
import com.lazyapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lazyapp.dto.DailyPlanResponse;
import com.lazyapp.dto.DeadlineConflict;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.model}")
    private String model;

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final AIPlanRepository aiPlanRepository;
    private final AIConversationRepository aiConversationRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 拆解任务：将一个模糊的大任务拆分为具体的子任务
     */
    @Transactional
    public List<SubTask> decomposeTask(Task task) {
        String prompt = buildDecomposePrompt(task);
        String response = callAI(prompt);

        // Save AI conversation
        saveConversation(task, "system", prompt);
        saveConversation(task, "assistant", response);

        // Parse and save sub-tasks
        return parseAndSaveSubTasks(task, response);
    }

    /**
     * AI 追问澄清：对信息不足的任务提出问题
     */
    @Transactional
    public String askClarification(Task task) {
        List<AIConversation> history = aiConversationRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());

        String prompt = buildClarificationPrompt(task, history);
        String response = callAI(prompt);

        saveConversation(task, "system", prompt);
        saveConversation(task, "assistant", response);

        return response;
    }

    /**
     * AI 针对子任务追问细化：给出具体执行步骤和建议
     */
    @Transactional
    public String clarifySubTask(SubTask subTask) {
        Task parentTask = subTask.getTask();
        String prompt = buildSubTaskClarifyPrompt(parentTask, subTask);
        String response = callAI(prompt);
        saveConversation(parentTask, "system", "[子任务细化] " + subTask.getTitle());
        saveConversation(parentTask, "assistant", response);
        return response;
    }

    /**
     * AI 子任务对话：支持用户输入具体问题进行多轮对话
     */
    @Transactional
    public String chatWithSubTask(SubTask subTask, String userMessage) {
        Task parentTask = subTask.getTask();
        
        // 保存用户消息
        saveConversation(parentTask, "user", userMessage);
        
        // 构建包含子任务上下文的 prompt
        String prompt = buildSubTaskChatPrompt(parentTask, subTask, userMessage);
        String response = callAI(prompt);
        
        // 保存 AI 回复
        saveConversation(parentTask, "assistant", response);
        
        return response;
    }

    /**
     * AI 回复用户的澄清答案
     */
    @Transactional
    public String respondToClarification(Task task, String userMessage) {
        saveConversation(task, "user", userMessage);

        List<AIConversation> history = aiConversationRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
        String prompt = buildFollowUpPrompt(task, history);
        String response = callAI(prompt);

        saveConversation(task, "assistant", response);
        return response;
    }

    /**
     * AI 每日排程：生成优先列表和日程表（可重入，每次覆盖当日计划）
     */
    @Transactional
    public DailyPlanResponse generateDailyPlan(Long userId, boolean skippedDay) {
        List<Task> activeTasks = taskRepository.findByUserIdAndStatusNotOrderByUrgencyDescDeadlineAsc(
                userId, TaskStatus.CANCELLED);

        List<DeadlineConflict> conflicts = new ArrayList<>();

        if (skippedDay) {
            conflicts = detectDeadlineConflicts(activeTasks);
        }

        String prompt = buildDailyPlanPrompt(activeTasks, skippedDay);
        String response = callAI(prompt);
        LocalDateTime now = LocalDateTime.now();

        // Upsert: overwrite today's plan if it already exists (re-entrant)
        User user = activeTasks.isEmpty() ? null : activeTasks.get(0).getUser();
        if (user != null) {
            List<AIPlan> existingPlans = aiPlanRepository.findByUserIdAndPlanDateOrderByGeneratedAtDesc(userId, LocalDate.now());
            if (!existingPlans.isEmpty()) {
                // Keep the first (newest), delete duplicates, then update
                AIPlan plan = existingPlans.get(0);
                if (existingPlans.size() > 1) {
                    log.info("清理 {} 条重复日程记录", existingPlans.size() - 1);
                    aiPlanRepository.deleteAll(existingPlans.subList(1, existingPlans.size()));
                }
                plan.setPlanJson(response);
                plan.setSkippedDay(skippedDay);
                aiPlanRepository.save(plan);
            } else {
                AIPlan plan = AIPlan.builder()
                        .user(user)
                        .planDate(LocalDate.now())
                        .planJson(response)
                        .skippedDay(skippedDay)
                        .build();
                aiPlanRepository.save(plan);
            }
        }

        return DailyPlanResponse.builder()
                .plan(response)
                .generatedAt(now)
                .skippedDay(skippedDay)
                .deadlineConflicts(conflicts)
                .build();
    }

    /**
     * 检测摸鱼时哪些任务的 deadline 已经无法再推迟
     */
    private List<DeadlineConflict> detectDeadlineConflicts(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        List<DeadlineConflict> conflicts = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getDeadline() != null && !task.getDeadline().isAfter(today)) {
                conflicts.add(DeadlineConflict.builder()
                        .taskId(task.getId())
                        .taskTitle(task.getTitle())
                        .deadline(task.getDeadline())
                        .reason(task.getDeadline().isEqual(today)
                                ? "今天是截止日，无法再推迟"
                                : "已过截止日期")
                        .build());
            }
        }
        return conflicts;
    }

    private String buildDecomposePrompt(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。请帮用户把以下任务拆解成具体的、可执行的子任务。\n\n");
        sb.append("任务标题: ").append(task.getTitle()).append("\n");
        if (task.getDescription() != null) {
            sb.append("任务描述: ").append(task.getDescription()).append("\n");
        }
        sb.append("紧急程度: ").append(task.getUrgency()).append("\n");
        if (task.getDeadline() != null) {
            sb.append("截止日期: ").append(task.getDeadline()).append("\n");
        }
        sb.append("\n请用JSON数组格式返回子任务列表，每个子任务包含：\n");
        sb.append("- title: 子任务标题\n");
        sb.append("- description: 具体行动说明\n");
        sb.append("- estimatedHours: 预计耗时（小时）\n");
        sb.append("- sortOrder: 建议执行顺序（从1开始）\n");
        sb.append("\n只返回JSON数组，不要其他内容。");
        return sb.toString();
    }

    private String buildClarificationPrompt(Task task, List<AIConversation> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。用户添加了一个任务，但信息可能不够具体。请提出1-3个关键问题来帮助细化任务。\n\n");
        sb.append("任务标题: ").append(task.getTitle()).append("\n");
        if (task.getDescription() != null) {
            sb.append("任务描述: ").append(task.getDescription()).append("\n");
        }
        sb.append("\n用中文提问，简洁友好。");
        return sb.toString();
    }

    private String buildFollowUpPrompt(Task task, List<AIConversation> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。以下是关于一个任务的对话记录，请根据用户的回复来决定：\n");
        sb.append("1. 如果信息已足够，回复 [READY] 并给出任务的完善描述\n");
        sb.append("2. 如果还需要更多信息，继续提问\n\n");
        sb.append("任务标题: ").append(task.getTitle()).append("\n\n");
        sb.append("对话记录:\n");
        for (AIConversation conv : history) {
            sb.append(conv.getRole()).append(": ").append(conv.getMessage()).append("\n");
        }
        return sb.toString();
    }

    private String buildSubTaskClarifyPrompt(Task parentTask, SubTask subTask) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。用户有一个大任务已被拆解为子任务，现在需要你针对其中一个子任务给出具体的执行指导。\n\n");
        sb.append("大任务: ").append(parentTask.getTitle()).append("\n");
        if (parentTask.getDescription() != null) {
            sb.append("大任务描述: ").append(parentTask.getDescription()).append("\n");
        }
        sb.append("\n当前子任务: ").append(subTask.getTitle()).append("\n");
        if (subTask.getDescription() != null) {
            sb.append("子任务描述: ").append(subTask.getDescription()).append("\n");
        }
        if (subTask.getEstimatedHours() != null) {
            sb.append("预计耗时: ").append(subTask.getEstimatedHours()).append("小时\n");
        }
        sb.append("\n请用中文给出：\n");
        sb.append("1. 这个子任务的具体执行步骤（3-5步）\n");
        sb.append("2. 可能遇到的困难和解决建议\n");
        sb.append("3. 推荐的工具或资源\n");
        sb.append("简洁实用，不要空话。");
        return sb.toString();
    }

    private String buildSubTaskChatPrompt(Task parentTask, SubTask subTask, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。用户正在执行一个子任务，需要你的帮助。\n\n");
        sb.append("大任务: ").append(parentTask.getTitle()).append("\n");
        if (parentTask.getDescription() != null) {
            sb.append("大任务描述: ").append(parentTask.getDescription()).append("\n");
        }
        sb.append("\n当前子任务: ").append(subTask.getTitle()).append("\n");
        if (subTask.getDescription() != null) {
            sb.append("子任务描述: ").append(subTask.getDescription()).append("\n");
        }
        if (subTask.getEstimatedHours() != null) {
            sb.append("预计耗时: ").append(subTask.getEstimatedHours()).append("小时\n");
        }
        
        // 获取相关对话历史（最近10条）
        List<AIConversation> history = aiConversationRepository.findByTaskIdOrderByCreatedAtAsc(parentTask.getId());
        if (!history.isEmpty()) {
            sb.append("\n对话记录:\n");
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                AIConversation conv = history.get(i);
                sb.append(conv.getRole()).append(": ").append(conv.getMessage()).append("\n");
            }
        }
        
        sb.append("\n用户当前问题: ").append(userMessage).append("\n\n");
        sb.append("请针对用户的问题，结合子任务的上下文，给出具体、实用的建议。用中文回复。");
        return sb.toString();
    }

    private String buildDailyPlanPrompt(List<Task> tasks, boolean skippedDay) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务管理秘书。请根据以下待办任务，生成今天的日程安排。\n\n");

        if (skippedDay) {
            sb.append("⚠️ 用户今天选择了「摸鱼一天」，请将所有任务推迟，重新规划后续日程。\n\n");
        }

        sb.append("当前待办任务:\n");
        for (Task task : tasks) {
            sb.append("- [").append(task.getUrgency()).append("] ").append(task.getTitle());
            if (task.getDeadline() != null) {
                sb.append(" (截止: ").append(task.getDeadline()).append(")");
            }
            sb.append(" [状态: ").append(task.getStatus()).append("]\n");

            if (task.getSubTasks() != null && !task.getSubTasks().isEmpty()) {
                for (SubTask st : task.getSubTasks()) {
                    sb.append("  └ ").append(st.getTitle())
                            .append(" (").append(st.getEstimatedHours()).append("h)")
                            .append(" [").append(st.getStatus()).append("]\n");
                }
            }
        }

        sb.append("\n请用中文生成一份结构清晰的今日日程，包括：\n");
        sb.append("1. 今日优先任务排序\n");
        sb.append("2. 建议的时间段安排\n");
        sb.append("3. 如果任务过多，标注哪些可以推迟\n");
        return sb.toString();
    }

    private String callAI(String prompt) {
        if ("your-api-key-here".equals(apiKey)) {
            log.warn("AI API Key 未配置，返回模拟数据");
            return getMockResponse(prompt);
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            // Build Gemini API request body
            String json = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 2048
                    )
            ));

            // Gemini API: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
            String url = baseUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonNode node = objectMapper.readTree(body);
                    // Gemini response: candidates[0].content.parts[0].text
                    String text = node.path("candidates").path(0)
                            .path("content").path("parts").path(0)
                            .path("text").asText();
                    if (text != null && !text.isEmpty()) {
                        return text;
                    }
                    log.warn("Gemini 返回内容为空, body: {}", body);
                    return getMockResponse(prompt);
                }
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.error("Gemini API 调用失败: {} - {}", response.code(), errorBody);
                return getMockResponse(prompt);
            }
        } catch (IOException e) {
            log.error("Gemini API 调用异常", e);
            return getMockResponse(prompt);
        }
    }

    private String getMockResponse(String prompt) {
        if (prompt.contains("当前子任务: ")) {
            return buildMockSubTaskClarifyResponse(prompt);
        }
        if (prompt.contains("拆解")) {
            return buildMockDecomposeResponse(prompt);
        }
        if (prompt.contains("日程")) {
            return buildMockDailyPlanResponse(prompt);
        }
        return "你好！我需要了解更多关于这个任务的信息。请问：\n1. 你希望达到什么样的目标？\n2. 你有多少时间可以投入？";
    }

    private String buildMockDecomposeResponse(String prompt) {
        // Extract task title from prompt for realistic mock
        String taskTitle = "任务";
        int idx = prompt.indexOf("任务标题: ");
        if (idx >= 0) {
            int end = prompt.indexOf("\n", idx);
            taskTitle = prompt.substring(idx + 6, end > idx ? end : idx + 20).trim();
        }
        return "[{\"title\":\"调研：" + taskTitle + "的具体目标和范围\",\"description\":\"花30分钟明确这件事到底要达到什么效果，列出关键成功标准\",\"estimatedHours\":0.5,\"sortOrder\":1},"
                + "{\"title\":\"制定行动计划\",\"description\":\"根据调研结果，把" + taskTitle + "拆分成每天可执行的小步骤\",\"estimatedHours\":1,\"sortOrder\":2},"
                + "{\"title\":\"执行第一个小步骤\",\"description\":\"按计划完成最简单的第一步，建立信心和动力\",\"estimatedHours\":2,\"sortOrder\":3}]";
    }

    private String buildMockSubTaskClarifyResponse(String prompt) {
        String subTaskTitle = "子任务";
        int idx = prompt.indexOf("当前子任务: ");
        if (idx >= 0) {
            int end = prompt.indexOf("\n", idx);
            subTaskTitle = prompt.substring(idx + 7, end > idx ? end : idx + 20).trim();
        }

        return "**「" + subTaskTitle + "」执行指导**\n\n"
                + "📌 **具体步骤：**\n"
                + "1. 花 10 分钟在网上搜索相关资料，收藏 3-5 个高质量参考\n"
                + "2. 用 15 分钟整理出关键要点，写成简单的清单\n"
                + "3. 根据清单制定最小可行动作，确保今天就能完成第一步\n"
                + "4. 执行第一步，完成后记录遇到的问题\n"
                + "5. 回顾总结，调整后续计划\n\n"
                + "⚠️ **可能遇到的困难：**\n"
                + "- 信息过多不知从何下手 → 设定 15 分钟时间限制，到时间就停\n"
                + "- 完美主义导致拖延 → 先完成 60 分版本，后续再优化\n\n"
                + "🔧 **推荐工具/资源：**\n"
                + "- 笔记整理：Notion / 飞书文档\n"
                + "- 时间管理：番茄钟（25分钟专注 + 5分钟休息）\n"
                + "- 进度追踪：就用 LazyApp 标记完成 ✅";
    }

    private String buildMockDailyPlanResponse(String prompt) {
        // Parse actual task names from the prompt to generate a realistic mock plan
        StringBuilder plan = new StringBuilder();
        boolean isSkipDay = prompt.contains("摸鱼");

        if (isSkipDay) {
            plan.append("🐟 **摸鱼日 — 任务已全部推迟**\n\n");
            plan.append("今天的任务已重新安排到后续日期。\n\n");
        }

        plan.append("📋 **").append(isSkipDay ? "明日起日程安排" : "今日日程安排").append("**\n\n");

        // Extract task lines from prompt
        List<String> taskLines = new ArrayList<>();
        for (String line : prompt.split("\n")) {
            if (line.startsWith("- [")) {
                taskLines.add(line);
            }
        }

        if (taskLines.isEmpty()) {
            plan.append("当前没有待办任务，享受自由的一天吧！🎉\n");
            return plan.toString();
        }

        // Separate by urgency for realistic scheduling
        List<String> urgent = new ArrayList<>();
        List<String> high = new ArrayList<>();
        List<String> medium = new ArrayList<>();
        List<String> low = new ArrayList<>();

        for (String line : taskLines) {
            String taskName = line.replaceAll("- \\[[A-Z]+\\] ", "").replaceAll(" \\(截止:.*?\\)", "").replaceAll(" \\[状态:.*?\\]", "").trim();
            if (line.contains("[URGENT]")) urgent.add(taskName);
            else if (line.contains("[HIGH]")) high.add(taskName);
            else if (line.contains("[MEDIUM]")) medium.add(taskName);
            else low.add(taskName);
        }

        int hour = isSkipDay ? 9 : 9;

        if (!urgent.isEmpty()) {
            plan.append("🔴 **紧急（必须今天完成）**\n");
            for (String t : urgent) {
                plan.append(String.format("⏰ %d:00-%d:30  %s\n", hour, hour, t));
                plan.append("   → 立即开始，不要犹豫\n");
                hour++;
            }
            plan.append("\n");
        }
        if (!high.isEmpty()) {
            plan.append("🟠 **高优先级**\n");
            for (String t : high) {
                plan.append(String.format("⏰ %d:00-%d:00  %s\n", hour, hour + 1, t));
                plan.append("   → 集中精力处理\n");
                hour += 1;
            }
            plan.append("\n");
        }
        if (!medium.isEmpty()) {
            if (hour >= 12 && hour < 14) hour = 14;
            plan.append("🟡 **中优先级**\n");
            for (String t : medium) {
                plan.append(String.format("⏰ %d:00-%d:30  %s\n", hour, hour, t));
                hour++;
            }
            plan.append("\n");
        }
        if (!low.isEmpty()) {
            plan.append("🔵 **低优先级（有空再做，可推迟）**\n");
            for (String t : low) {
                plan.append("   · ").append(t).append("\n");
            }
            plan.append("\n");
        }

        if (hour > 17) {
            plan.append("⚠️ 今日任务量较多，低优先级任务建议推迟到明天。\n");
        }

        plan.append("\n💡 提示：每完成一项就标记为已完成，保持节奏！");
        return plan.toString();
    }

    private List<SubTask> parseAndSaveSubTasks(Task task, String response) {
        List<SubTask> subTasks = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(response);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    SubTask subTask = SubTask.builder()
                            .task(task)
                            .title(node.path("title").asText())
                            .description(node.path("description").asText())
                            .estimatedHours(node.path("estimatedHours").asDouble())
                            .sortOrder(node.path("sortOrder").asInt())
                            .status(TaskStatus.PENDING)
                            .build();
                    subTasks.add(subTaskRepository.save(subTask));
                }
            }
        } catch (Exception e) {
            log.error("解析AI返回的子任务失败", e);
        }
        return subTasks;
    }

    private void saveConversation(Task task, String role, String message) {
        AIConversation conversation = AIConversation.builder()
                .task(task)
                .role(role)
                .message(message)
                .build();
        aiConversationRepository.save(conversation);
    }

}
