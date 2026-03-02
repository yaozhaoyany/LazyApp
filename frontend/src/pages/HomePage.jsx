import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Fish, Calendar, Sparkles, Trash2, CheckCircle2, Clock, AlertTriangle, AlertCircle, ChevronRight, RefreshCw, X } from 'lucide-react'
import { getTasks, createTask, deleteTask, updateTaskStatus, skipDay, generateDailyPlan, updateTaskDeadline } from '../services/api'

const URGENCY_CONFIG = {
  LOW: { label: '低', color: 'bg-blue-100 text-blue-700', icon: Clock },
  MEDIUM: { label: '中', color: 'bg-yellow-100 text-yellow-700', icon: AlertTriangle },
  HIGH: { label: '高', color: 'bg-orange-100 text-orange-700', icon: AlertCircle },
  URGENT: { label: '紧急', color: 'bg-red-100 text-red-700', icon: AlertCircle },
}

const STATUS_CONFIG = {
  PENDING: { label: '待办', color: 'text-slate-500' },
  IN_PROGRESS: { label: '进行中', color: 'text-blue-600' },
  COMPLETED: { label: '已完成', color: 'text-green-600' },
  CANCELLED: { label: '已取消', color: 'text-slate-400' },
}

export default function HomePage() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState([])
  const [showAddForm, setShowAddForm] = useState(false)
  const [newTask, setNewTask] = useState({ title: '', description: '', urgency: 'MEDIUM', deadline: '' })
  const [dailyPlan, setDailyPlan] = useState(null)
  const [planGeneratedAt, setPlanGeneratedAt] = useState(null)
  const [planIsSkipDay, setPlanIsSkipDay] = useState(false)
  const [deadlineConflicts, setDeadlineConflicts] = useState([])
  const [showConflictDialog, setShowConflictDialog] = useState(false)
  const [conflictNewDeadline, setConflictNewDeadline] = useState('')
  const [loading, setLoading] = useState(true)
  const [planLoading, setPlanLoading] = useState(false)

  useEffect(() => {
    loadTasks()
  }, [])

  const loadTasks = async () => {
    try {
      const res = await getTasks()
      setTasks(res.data)
    } catch (err) {
      console.error('加载任务失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateTask = async (e) => {
    e.preventDefault()
    if (!newTask.title.trim()) return
    try {
      const payload = {
        title: newTask.title,
        description: newTask.description || null,
        urgency: newTask.urgency,
        deadline: newTask.deadline || null,
      }
      await createTask(payload)
      setNewTask({ title: '', description: '', urgency: 'MEDIUM', deadline: '' })
      setShowAddForm(false)
      loadTasks()
    } catch (err) {
      console.error('创建任务失败:', err)
    }
  }

  const handleDelete = async (id, e) => {
    e.stopPropagation()
    try {
      await deleteTask(id)
      loadTasks()
    } catch (err) {
      console.error('删除任务失败:', err)
    }
  }

  const handleComplete = async (id, e) => {
    e.stopPropagation()
    try {
      await updateTaskStatus(id, 'COMPLETED')
      loadTasks()
    } catch (err) {
      console.error('更新状态失败:', err)
    }
  }

  const handlePlanResponse = (data) => {
    setDailyPlan(data.plan)
    setPlanGeneratedAt(data.generatedAt)
    setPlanIsSkipDay(data.skippedDay)
    if (data.deadlineConflicts && data.deadlineConflicts.length > 0) {
      setDeadlineConflicts(data.deadlineConflicts)
      setShowConflictDialog(true)
    }
  }

  const handleSkipDay = async () => {
    setPlanLoading(true)
    try {
      const res = await skipDay()
      handlePlanResponse(res.data)
    } catch (err) {
      console.error('摸鱼失败:', err)
    } finally {
      setPlanLoading(false)
    }
  }

  const handleGeneratePlan = async () => {
    setPlanLoading(true)
    try {
      const res = await generateDailyPlan()
      handlePlanResponse(res.data)
    } catch (err) {
      console.error('生成日程失败:', err)
    } finally {
      setPlanLoading(false)
    }
  }

  const handleConflictResetDeadline = async (taskId) => {
    if (!conflictNewDeadline) return
    try {
      await updateTaskDeadline(taskId, conflictNewDeadline)
      setDeadlineConflicts(prev => prev.filter(c => c.taskId !== taskId))
      if (deadlineConflicts.length <= 1) {
        setShowConflictDialog(false)
        setConflictNewDeadline('')
      }
      loadTasks()
    } catch (err) {
      console.error('更新截止日期失败:', err)
    }
  }

  const handleConflictDeleteTask = async (taskId) => {
    try {
      await deleteTask(taskId)
      setDeadlineConflicts(prev => prev.filter(c => c.taskId !== taskId))
      if (deadlineConflicts.length <= 1) {
        setShowConflictDialog(false)
      }
      loadTasks()
    } catch (err) {
      console.error('删除任务失败:', err)
    }
  }

  const activeTasks = tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'CANCELLED')
  const completedTasks = tasks.filter(t => t.status === 'COMPLETED')

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="text-center mb-8">
        <h1 className="text-4xl font-bold text-slate-800 mb-2">
          🦥 LazyApp
        </h1>
        <p className="text-slate-500 text-lg">做了么？让 AI 帮你安排一切</p>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-3 mb-6">
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="flex-1 flex items-center justify-center gap-2 bg-primary-600 hover:bg-primary-700 text-white py-3 px-4 rounded-xl font-medium transition-colors"
        >
          <Plus size={20} />
          添加待办
        </button>
        <button
          onClick={handleGeneratePlan}
          disabled={planLoading}
          className="flex-1 flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 disabled:bg-purple-300 text-white py-3 px-4 rounded-xl font-medium transition-colors"
        >
          {planLoading ? <RefreshCw size={20} className="animate-spin" /> : <Sparkles size={20} />}
          {dailyPlan ? '刷新日程' : 'AI 排日程'}
        </button>
        <button
          onClick={handleSkipDay}
          disabled={planLoading}
          className="flex items-center justify-center gap-2 bg-slate-100 hover:bg-slate-200 disabled:bg-slate-50 text-slate-600 py-3 px-4 rounded-xl font-medium transition-colors border border-slate-200"
          title="今天偷懒，摸鱼一天"
        >
          <Fish size={20} />
          摸鱼
        </button>
      </div>

      {/* Add Task Form */}
      {showAddForm && (
        <form onSubmit={handleCreateTask} className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 mb-6">
          <h3 className="text-lg font-semibold text-slate-700 mb-4">添加新任务</h3>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="输入任务，可以很模糊，比如「今年想学日语」"
              value={newTask.title}
              onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
              className="w-full px-4 py-3 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-slate-700"
              autoFocus
            />
            <textarea
              placeholder="补充说明（可选）"
              value={newTask.description}
              onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
              className="w-full px-4 py-3 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent text-slate-700 resize-none"
              rows={2}
            />
            <div className="flex gap-3">
              <select
                value={newTask.urgency}
                onChange={(e) => setNewTask({ ...newTask, urgency: e.target.value })}
                className="flex-1 px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-slate-700"
              >
                <option value="LOW">低优先级</option>
                <option value="MEDIUM">中优先级</option>
                <option value="HIGH">高优先级</option>
                <option value="URGENT">紧急</option>
              </select>
              <input
                type="date"
                value={newTask.deadline}
                onChange={(e) => setNewTask({ ...newTask, deadline: e.target.value })}
                className="flex-1 px-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-slate-700"
              />
            </div>
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setShowAddForm(false)}
                className="px-4 py-2 text-slate-500 hover:text-slate-700 transition-colors"
              >
                取消
              </button>
              <button
                type="submit"
                className="px-6 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg font-medium transition-colors"
              >
                添加
              </button>
            </div>
          </div>
        </form>
      )}

      {/* Daily Plan */}
      {dailyPlan && (
        <div className={`rounded-xl border p-5 mb-6 ${planIsSkipDay ? 'bg-gradient-to-r from-slate-50 to-blue-50 border-slate-200' : 'bg-gradient-to-r from-purple-50 to-indigo-50 border-purple-200'}`}>
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              {planIsSkipDay ? <Fish size={20} className="text-slate-500" /> : <Calendar size={20} className="text-purple-600" />}
              <h3 className={`text-lg font-semibold ${planIsSkipDay ? 'text-slate-700' : 'text-purple-800'}`}>
                {planIsSkipDay ? '摸鱼日计划' : '今日日程'}
              </h3>
            </div>
            {planGeneratedAt && (
              <span className="text-xs text-slate-400">
                生成于 {new Date(planGeneratedAt).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit', month: '2-digit', day: '2-digit' })}
              </span>
            )}
          </div>
          <div className="text-slate-700 whitespace-pre-wrap text-sm leading-relaxed">
            {dailyPlan}
          </div>
        </div>
      )}

      {/* Deadline Conflict Dialog */}
      {showConflictDialog && deadlineConflicts.length > 0 && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 px-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-red-600 flex items-center gap-2">
                <AlertTriangle size={20} />
                截止日期冲突
              </h3>
              <button onClick={() => setShowConflictDialog(false)} className="text-slate-400 hover:text-slate-600">
                <X size={20} />
              </button>
            </div>
            <p className="text-sm text-slate-500 mb-4">以下任务的截止日期已到或已过，无法再推迟。请选择处理方式：</p>
            <div className="space-y-4">
              {deadlineConflicts.map((conflict) => (
                <div key={conflict.taskId} className="border border-red-100 bg-red-50 rounded-xl p-4">
                  <div className="font-medium text-slate-800 mb-1">{conflict.taskTitle}</div>
                  <div className="text-xs text-red-500 mb-3">
                    截止：{conflict.deadline} — {conflict.reason}
                  </div>
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <input
                        type="date"
                        value={conflictNewDeadline}
                        onChange={(e) => setConflictNewDeadline(e.target.value)}
                        className="flex-1 px-3 py-1.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                        min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
                      />
                      <button
                        onClick={() => handleConflictResetDeadline(conflict.taskId)}
                        disabled={!conflictNewDeadline}
                        className="px-3 py-1.5 bg-primary-600 hover:bg-primary-700 disabled:bg-slate-300 text-white text-sm rounded-lg transition-colors"
                      >
                        重设日期
                      </button>
                    </div>
                    <button
                      onClick={() => handleConflictDeleteTask(conflict.taskId)}
                      className="w-full px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-sm rounded-lg border border-red-200 transition-colors"
                    >
                      删除此任务
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Task List */}
      {loading ? (
        <div className="text-center py-12 text-slate-400">加载中...</div>
      ) : activeTasks.length === 0 ? (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🎉</div>
          <p className="text-slate-500 text-lg">没有待办事项，享受自由吧！</p>
          <p className="text-slate-400 text-sm mt-2">点击「添加待办」开始记录</p>
        </div>
      ) : (
        <div className="space-y-3">
          <h2 className="text-sm font-medium text-slate-400 uppercase tracking-wider">
            待办事项 ({activeTasks.length})
          </h2>
          {activeTasks.map((task) => {
            const urgencyConf = URGENCY_CONFIG[task.urgency] || URGENCY_CONFIG.MEDIUM
            const UrgencyIcon = urgencyConf.icon
            return (
              <div
                key={task.id}
                onClick={() => navigate(`/task/${task.id}`)}
                className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 hover:shadow-md hover:border-slate-300 transition-all cursor-pointer group"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-medium text-slate-800 truncate">{task.title}</h3>
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${urgencyConf.color}`}>
                        <UrgencyIcon size={12} />
                        {urgencyConf.label}
                      </span>
                    </div>
                    {task.description && (
                      <p className="text-sm text-slate-500 truncate">{task.description}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2 text-xs text-slate-400">
                      {task.deadline && (
                        <span className="flex items-center gap-1">
                          <Calendar size={12} />
                          {task.deadline}
                        </span>
                      )}
                      {task.subTasks && task.subTasks.length > 0 && (
                        <span>{task.subTasks.filter(s => s.status === 'COMPLETED').length}/{task.subTasks.length} 子任务</span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={(e) => handleComplete(task.id, e)}
                      className="p-1.5 text-green-500 hover:bg-green-50 rounded-lg transition-colors"
                      title="标记完成"
                    >
                      <CheckCircle2 size={18} />
                    </button>
                    <button
                      onClick={(e) => handleDelete(task.id, e)}
                      className="p-1.5 text-red-400 hover:bg-red-50 rounded-lg transition-colors"
                      title="删除"
                    >
                      <Trash2 size={18} />
                    </button>
                    <ChevronRight size={18} className="text-slate-300" />
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Completed Tasks */}
      {completedTasks.length > 0 && (
        <div className="mt-8 space-y-3">
          <h2 className="text-sm font-medium text-slate-400 uppercase tracking-wider">
            已完成 ({completedTasks.length})
          </h2>
          {completedTasks.map((task) => (
            <div
              key={task.id}
              className="bg-white/50 rounded-xl border border-slate-100 p-4 opacity-60"
            >
              <div className="flex items-center gap-2">
                <CheckCircle2 size={18} className="text-green-500" />
                <span className="text-slate-500 line-through">{task.title}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
