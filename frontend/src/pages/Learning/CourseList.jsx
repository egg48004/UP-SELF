import Sidebar from '../../components/Sidebar'
import '../../css/Learning.css'
import { useNavigate } from 'react-router-dom'

function CourseList() {

  const navigate = useNavigate()

  const courses = [
    {
      id: 1,
      title: 'Entrepreneurship Basics',
      level: 'Beginner',
      duration: '2 Hours'
    },
    {
      id: 2,
      title: 'Marketing Fundamentals',
      level: 'Beginner',
      duration: '1.5 Hours'
    },
    {
      id: 3,
      title: 'Finance Essentials',
      level: 'Beginner',
      duration: '2 Hours'
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="learning-container">

          <h1>All Courses</h1>

          <p className="page-subtitle">
            Find a course that matches your current goal.
          </p>

          <input
            className="search-bar"
            placeholder="Search courses..."
          />

          {courses.map(course => (

            <div
              key={course.id}
              className="course-list-card"
            >

              <h2>{course.title}</h2>

              <p>Level: {course.level}</p>

              <p>Estimated Time: {course.duration}</p>

              <button
                onClick={() =>
                  navigate(`/learning/course/${course.id}`)
                }
              >
                Open Course
              </button>

            </div>

          ))}

        </div>

      </div>

    </div>
  )
}

export default CourseList