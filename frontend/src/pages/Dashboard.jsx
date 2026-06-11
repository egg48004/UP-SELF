import '../App.css'
import Sidebar from '../components/Sidebar'
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

function Dashboard() {

  const navigate = useNavigate()

  const [student, setStudent] = useState(null)

  useEffect(() => {

    const mockResponse = {
      name: "Apple",
      mood: "😊 Happy",
      goal: "Finish Chapter 3",
      progress: 65,
      badge: "First Step",

      currentCourse: "Entrepreneurship Basics",
      currentChapter: "Chapter 3: Identifying Opportunities",

      currentChapterId: 3
    }

    setStudent(mockResponse)

  }, [])

  if (!student) {
    return <h2>Loading...</h2>
  }

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="container">

          <div className="header">
            <h2>Welcome Back, {student.name} 👋</h2>
          </div>

          <div className="top-row">

            <div className="card">
              <h3>Today's Mood</h3>
              <p>{student.mood}</p>
            </div>

            <div className="card">
              <h3>Current Goal</h3>
              <p>{student.goal}</p>
            </div>

          </div>

          <div className="card">

            <h3>Continue Learning</h3>

            <p>{student.currentCourse}</p>

            <p>{student.currentChapter}</p>

            <button
              onClick={() =>
                navigate(
                  `/learning/chapter/${student.currentChapterId}`
                )
              }
            >
              Continue Learning
            </button>

          </div>

          <div className="bottom-row">

            <div className="card progress-card">

              <h3>Learning Progress</h3>

              <p>
                You're making great progress! 🌟
              </p>

              <div className="progress-bar">

                <div
                  className="progress-fill"
                  style={{
                    width: `${student.progress}%`
                  }}
                ></div>

              </div>

              <p>{student.progress}% Completed</p>

            </div>

            <div className="card badge-card">

              <h3>Achievement Badge</h3>

              <div className="badge">
                🌱
              </div>

              <div className="badge-title">
                {student.badge}
              </div>

              <div className="badge-desc">
                Completed your first learning milestone.
              </div>

            </div>

          </div>

        </div>

      </div>

    </div>
  )
}

export default Dashboard