import Sidebar from '../../components/Sidebar'
import '../../css/Learning.css'
import { useNavigate } from 'react-router-dom'

function Learning() {

  const navigate = useNavigate()

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="learning-container">

          <div className="welcome-section">
            <h1>Good Morning, Sarah 🌱</h1>
            <p>
              Small steps every day lead to meaningful progress.
            </p>
          </div>

          {/* Continue Learning */}

          <div className="focus-card">

            <h2>Continue Learning</h2>

            <h3>Entrepreneurship Basics</h3>

            <p>Chapter 3 of 8</p>

            <div className="progress-bar">
              <div className="progress-fill"></div>
            </div>

            <p>You're making great progress! 🌟</p>

            <button
              onClick={() => navigate('/learning/chapter/3')}
            >
              Continue Learning
            </button>

          </div>

          {/* Today's Goal */}

          <div className="goal-card">

            <h2>Today's Goal 🎯</h2>

            <h3>Finish Chapter 3</h3>

            {/* <p>Estimated Time: 15 Minutes</p> */}

          </div>

          {/* Learning Paths */}

          <div className="course-section">

            <h2>Your Learning Paths</h2>

            <div className="course-grid">

              <div className="course-card">

                <h3>🚀 Entrepreneurship</h3>

                <p>
                  Learn how to identify opportunities and build ideas.
                </p>

                <button
                  onClick={() => navigate('/learning/course/1')}
                >
                  Open Course
                </button>

              </div>

              <div className="course-card">

                <h3>📈 Marketing</h3>

                <p>
                  Understand audiences and communicate effectively.
                </p>

                <button
                  onClick={() => navigate('/learning/course/2')}
                >
                  Open Course
                </button>

              </div>

              <div className="course-card">

                <h3>💰 Finance</h3>

                <p>
                  Learn budgeting and financial planning.
                </p>

                <button
                  onClick={() => navigate('/learning/course/3')}
                >
                  Open Course
                </button>

              </div>

            </div>

          </div>

          {/* All Courses */}

          <div className="all-courses-card">

            <h2>Explore More Courses</h2>

            <p>
              Browse all available courses at your own pace.
            </p>

            <button
              onClick={() => navigate('/learning/courses')}
            >
              View All Courses
            </button>

          </div>

        </div>

      </div>

    </div>
  )
}

export default Learning