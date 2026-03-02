import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import HomePage from './pages/HomePage'
import TaskDetailPage from './pages/TaskDetailPage'

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-slate-50">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/task/:id" element={<TaskDetailPage />} />
        </Routes>
      </div>
    </Router>
  )
}

export default App
