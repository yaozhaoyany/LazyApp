import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
    'X-User-Id': '1',
  },
})

// Task APIs
export const getTasks = () => api.get('/tasks')
export const getTask = (id) => api.get(`/tasks/${id}`)
export const createTask = (data) => api.post('/tasks', data)
export const updateTask = (id, data) => api.put(`/tasks/${id}`, data)
export const updateTaskStatus = (id, status) => api.patch(`/tasks/${id}/status?status=${status}`)
export const deleteTask = (id) => api.delete(`/tasks/${id}`)

// AI APIs
export const decomposeTask = (taskId) => api.post(`/ai/tasks/${taskId}/decompose`)
export const askClarification = (taskId) => api.post(`/ai/tasks/${taskId}/clarify`)
export const respondToClarification = (taskId, message) =>
  api.post(`/ai/tasks/${taskId}/respond`, { message })
export const generateDailyPlan = () => api.post('/ai/plan/daily')
export const skipDay = () => api.post('/ai/plan/skip-day')
export const updateTaskDeadline = (taskId, newDeadline) =>
  api.patch(`/ai/tasks/${taskId}/deadline?newDeadline=${newDeadline}`)
export const clarifySubTask = (subTaskId) => api.post(`/ai/subtasks/${subTaskId}/clarify`)
export const chatWithSubTask = (subTaskId, message) =>
  api.post(`/ai/subtasks/${subTaskId}/chat`, { message })
export const updateSubTaskStatus = (subTaskId, status) =>
  api.patch(`/ai/subtasks/${subTaskId}/status?status=${status}`)

export default api
