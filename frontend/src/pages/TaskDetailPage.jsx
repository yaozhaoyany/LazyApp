import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Sparkles, MessageCircle, Send, CheckCircle2, Clock, Calendar, Lightbulb, RefreshCw } from 'lucide-react'
import { getTask, decomposeTask, askClarification, respondToClarification, updateTaskStatus, clarifySubTask, chatWithSubTask, updateSubTaskStatus } from '../services/api'

const STATUS_STYLES = {
  PENDING: 'bg-slate-100 text-slate-600',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-slate-100 text-slate-400',
}

export default function TaskDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [loading, setLoading] = useState(true)
  const [aiChat, setAiChat] = useState([])
  const [userMessage, setUserMessage] = useState('')
  const [aiLoading, setAiLoading] = useState(false)
  const [subTaskAdvice, setSubTaskAdvice] = useState({})
  const [subTaskLoading, setSubTaskLoading] = useState(null)
  const [chatModal, setChatModal] = useState(null)
  const [chatMessages, setChatMessages] = useState({})
  const [chatInput, setChatInput] = useState('')

  useEffect(() => {
    loadTask()
  }, [id])

  const loadTask = async () => {
    try {
      const res = await getTask(id)
      setTask(res.data)
    } catch (err) {
      console.error('加载任务失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleDecompose = async () => {
    setAiLoading(true)
    try {
      await decomposeTask(id)
      setAiChat(prev => [...prev, { role: 'system', message: '✅ 任务已拆解为子任务！' }])
    } catch (err) {
      console.error('拆解任务失败:', err)
    } finally {
      await loadTask()
      setAiLoading(false)
    }
  }

  const handleAskClarification = async () => {
    setAiLoading(true)
    try {
      const res = await askClarification(id)
      setAiChat(prev => [...prev, { role: 'assistant', message: res.data.message }])
    } catch (err) {
      console.error('AI 追问失败:', err)
    } finally {
      setAiLoading(false)
    }
  }

  const handleSendMessage = async (e) => {
    e.preventDefault()
    if (!userMessage.trim()) return

    const msg = userMessage
    setUserMessage('')
    setAiChat(prev => [...prev, { role: 'user', message: msg }])
    setAiLoading(true)

    try {
      const res = await respondToClarification(id, msg)
      setAiChat(prev => [...prev, { role: 'assistant', message: res.data.message }])
    } catch (err) {
      console.error('发送消息失败:', err)
    } finally {
      setAiLoading(false)
    }
  }

  const handleSubTaskChat = async (subTaskId, subTaskTitle) => {
    // Initialize chat if not exists
    if (!chatMessages[subTaskId]) {
      setChatMessages(prev => ({ ...prev, [subTaskId]: [] }))
    }
    setChatModal({ subTaskId, title: subTaskTitle })
  }

  const handleSendChatMessage = async () => {
    if (!chatInput.trim() || !chatModal) return

    const subTaskId = chatModal.subTaskId
    const userMsg = chatInput.trim()
    
    // Add user message
    setChatMessages(prev => ({
      ...prev,
      [subTaskId]: [...(prev[subTaskId] || []), { role: 'user', message: userMsg }]
    }))
    setChatInput('')
    setSubTaskLoading(subTaskId)

    try {
      const res = await chatWithSubTask(subTaskId, userMsg)
      const aiMsg = res.data.message
      
      // Add AI response
      setChatMessages(prev => ({
        ...prev,
        [subTaskId]: [...prev[subTaskId], { role: 'assistant', message: aiMsg }]
      }))
    } catch (err) {
      console.error('子任务对话失败:', err)
      setChatMessages(prev => ({
        ...prev,
        [subTaskId]: [...prev[subTaskId], { role: 'system', message: '对话失败，请重试' }]
      }))
    } finally {
      setSubTaskLoading(null)
    }
  }

  const handleSubTaskComplete = async (subTaskId) => {
    try {
      await updateSubTaskStatus(subTaskId, 'COMPLETED')
      await loadTask()
    } catch (err) {
      console.error('更新子任务状态失败:', err)
    }
  }


  if (loading) {
    return <div className="flex justify-center items-center min-h-screen text-slate-400">加载中...</div>
  }

  if (!task) {
    return <div className="flex justify-center items-center min-h-screen text-slate-400">任务不存在</div>
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      {/* Back Button */}
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-1 text-slate-500 hover:text-slate-700 mb-6 transition-colors"
      >
        <ArrowLeft size={18} />
        返回
      </button>

      {/* Task Header */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
        <div className="flex items-start justify-between mb-4">
          <h1 className="text-2xl font-bold text-slate-800">{task.title}</h1>
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${STATUS_STYLES[task.status]}`}>
            {task.status === 'PENDING' ? '待办' : task.status === 'IN_PROGRESS' ? '进行中' : task.status === 'COMPLETED' ? '已完成' : '已取消'}
          </span>
        </div>

        {task.description && (
          <p className="text-slate-600 mb-4">{task.description}</p>
        )}

        <div className="flex items-center gap-4 text-sm text-slate-400">
          {task.deadline && (
            <span className="flex items-center gap-1">
              <Calendar size={14} />
              截止: {task.deadline}
            </span>
          )}
          <span className="flex items-center gap-1">
            <Clock size={14} />
            创建: {new Date(task.createdAt).toLocaleDateString('zh-CN')}
          </span>
        </div>
      </div>

      {/* AI Action Buttons */}
      <div className="flex gap-3 mb-6">
        <button
          onClick={handleDecompose}
          disabled={aiLoading}
          className="flex-1 flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 disabled:bg-purple-300 text-white py-3 px-4 rounded-xl font-medium transition-colors"
        >
          <Sparkles size={18} />
          AI 拆解任务
        </button>
        <button
          onClick={handleAskClarification}
          disabled={aiLoading}
          className="flex-1 flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white py-3 px-4 rounded-xl font-medium transition-colors"
        >
          <MessageCircle size={18} />
          AI 追问细化
        </button>
      </div>

      {/* Sub Tasks */}
      {task.subTasks && task.subTasks.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-700 mb-4">
            📋 子任务 ({task.subTasks.filter(s => s.status === 'COMPLETED').length}/{task.subTasks.length})
          </h2>
          <div className="space-y-2">
            {task.subTasks.map((sub, idx) => {
              const advice = subTaskAdvice[sub.id]
              const isLoading = subTaskLoading === sub.id
              const isCompleted = sub.status === 'COMPLETED'

              return (
                <div key={sub.id || idx} className={`rounded-xl border transition-all ${isCompleted ? 'bg-green-50/50 border-green-100' : 'bg-slate-50 border-slate-100'}`}>
                  <div className="flex items-start gap-3 p-3">
                    <button
                      onClick={() => !isCompleted && handleSubTaskComplete(sub.id)}
                      className="flex-shrink-0 mt-0.5"
                      title={isCompleted ? '已完成' : '标记为已完成'}
                    >
                      {isCompleted ? (
                        <CheckCircle2 size={20} className="text-green-500" />
                      ) : (
                        <div className="w-5 h-5 rounded-full border-2 border-slate-300 hover:border-green-400 transition-colors" />
                      )}
                    </button>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className={`font-medium ${isCompleted ? 'text-slate-400 line-through' : 'text-slate-700'}`}>{sub.title}</span>
                        {sub.estimatedHours && (
                          <span className="text-xs text-slate-400 bg-slate-200 px-2 py-0.5 rounded-full">
                            {sub.estimatedHours}h
                          </span>
                        )}
                      </div>
                      {sub.description && (
                        <p className={`text-sm mt-1 ${isCompleted ? 'text-slate-400' : 'text-slate-500'}`}>{sub.description}</p>
                      )}
                      {!isCompleted && (
                        <div className="flex items-center gap-2 mt-2">
                          <button
                            onClick={() => handleSubTaskChat(sub.id, sub.title)}
                            className="inline-flex items-center gap-1 text-xs px-2.5 py-1 bg-purple-50 hover:bg-purple-100 text-purple-600 rounded-lg transition-colors"
                          >
                            <MessageCircle size={12} />
                            AI 对话指导
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* AI Chat */}
      {aiChat.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-700 mb-4">💬 AI 对话</h2>
          <div className="space-y-3 mb-4">
            {aiChat.map((msg, idx) => (
              <div
                key={idx}
                className={`p-3 rounded-lg ${
                  msg.role === 'user'
                    ? 'bg-primary-50 text-primary-800 ml-8'
                    : msg.role === 'system'
                    ? 'bg-green-50 text-green-700'
                    : 'bg-slate-50 text-slate-700 mr-8'
                }`}
              >
                <div className="text-xs font-medium mb-1 opacity-60">
                  {msg.role === 'user' ? '你' : msg.role === 'system' ? '系统' : 'AI 秘书'}
                </div>
                <div className="whitespace-pre-wrap text-sm">{msg.message}</div>
              </div>
            ))}
            {aiLoading && (
              <div className="bg-slate-50 p-3 rounded-lg mr-8">
                <div className="text-xs font-medium mb-1 opacity-60">AI 秘书</div>
                <div className="text-sm text-slate-400">思考中...</div>
              </div>
            )}
          </div>

          {/* Chat Input */}
          <form onSubmit={handleSendMessage} className="flex gap-2">
            <input
              type="text"
              value={userMessage}
              onChange={(e) => setUserMessage(e.target.value)}
              placeholder="回复 AI 的问题..."
              className="flex-1 px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              disabled={aiLoading}
            />
            <button
              type="submit"
              disabled={aiLoading || !userMessage.trim()}
              className="px-4 py-2 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 text-white rounded-lg transition-colors"
            >
              <Send size={16} />
            </button>
          </form>
        </div>
      )}
      {/* SubTask Chat Modal */}
      {chatModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center" onClick={() => setChatModal(null)}>
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
          <div
            className="relative bg-white w-full sm:max-w-lg sm:rounded-2xl rounded-t-2xl shadow-2xl max-h-[80vh] overflow-hidden animate-slideUp flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div className="bg-gradient-to-r from-purple-600 to-indigo-600 px-6 py-4 flex-shrink-0">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-white">
                  <MessageCircle size={18} />
                  <span className="font-semibold">AI 对话指导</span>
                </div>
                <button
                  onClick={() => setChatModal(null)}
                  className="text-white/70 hover:text-white transition-colors"
                >
                  ✕
                </button>
              </div>
              <div className="text-purple-100 text-sm mt-1">{chatModal.title}</div>
            </div>

            {/* Messages */}
            <div className="flex-1 px-6 py-4 overflow-y-auto min-h-0">
              {chatMessages[chatModal.subTaskId]?.length === 0 && (
                <div className="text-center text-slate-400 text-sm py-8">
                  👋 有什么问题可以问我，比如：<br />
                  <span className="text-xs">"这个子任务需要用到什么工具？"</span>
                </div>
              )}
              <div className="space-y-3">
                {chatMessages[chatModal.subTaskId]?.map((msg, idx) => (
                  <div key={idx} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[80%] px-4 py-2 rounded-2xl text-sm ${
                      msg.role === 'user' 
                        ? 'bg-purple-600 text-white' 
                        : msg.role === 'system'
                        ? 'bg-slate-100 text-slate-500'
                        : 'bg-slate-100 text-slate-700'
                    }`}>
                      {msg.message}
                    </div>
                  </div>
                ))}
                {subTaskLoading === chatModal.subTaskId && (
                  <div className="flex justify-start">
                    <div className="bg-slate-100 text-slate-500 px-4 py-2 rounded-2xl text-sm">
                      <RefreshCw size={14} className="inline animate-spin mr-1" />
                      思考中...
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Input */}
            <div className="px-6 py-3 border-t border-slate-100 flex-shrink-0">
              <form onSubmit={(e) => { e.preventDefault(); handleSendChatMessage(); }} className="flex gap-2">
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="输入你的问题..."
                  className="flex-1 px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  disabled={subTaskLoading === chatModal.subTaskId}
                />
                <button
                  type="submit"
                  disabled={!chatInput.trim() || subTaskLoading === chatModal.subTaskId}
                  className="px-3 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-slate-300 text-white rounded-lg transition-colors"
                >
                  <Send size={16} />
                </button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
