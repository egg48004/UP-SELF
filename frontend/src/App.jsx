import { BrowserRouter, Routes, Route } from 'react-router-dom'

import Dashboard from './pages/Dashboard'
import Assignments from './pages/Assignments'
import Goals from './pages/Goals'
import MoodTracker from './pages/MoodTracker'
import AIAssistant from './pages/AIAssistant'
import Login from './pages/Login'
import Learning from './pages/Learning/Learning'
import CourseList from './pages/Learning/CourseList'
import CourseDetail from './pages/Learning/CourseDetail'
import ChapterDetail from './pages/Learning/ChapterDetail'
import Communication from './pages/Communication'

function App() {
  return (
    <BrowserRouter>

      <Routes>

        <Route path="/" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/learning" element={<Learning />}/>
        <Route path="/assignments" element={<Assignments />}/>
        <Route path="/goals" element={<Goals />} />
        <Route path="/mood" element={<MoodTracker />}/>
        <Route path="/ai" element={<AIAssistant />} />
        <Route path="/learning" element={<Learning />} />

        <Route path="/learning/courses" element={<CourseList />} />
        <Route path="/learning/course/:id" element={<CourseDetail />} />
        <Route path="/learning/chapter/:id" element={<ChapterDetail />} />

        <Route
          path="/communication"
          element={<Communication />}
        />

      </Routes>

    </BrowserRouter>
  )
}

export default App