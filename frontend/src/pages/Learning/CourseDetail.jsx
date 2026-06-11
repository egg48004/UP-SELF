import Sidebar from '../../components/Sidebar'
import '../../css/Learning.css'
import { useNavigate, useParams } from 'react-router-dom'

function CourseDetail() {

  const navigate = useNavigate()

  const { id } = useParams()

  const course = {
    title: 'Entrepreneurship Basics',
    progress: 65,
    description:
      'Learn how to identify opportunities and develop business ideas.'
  }

  const chapters = [
    {
      id: 1,
      title: 'Introduction',
      time: '10 min'
    },
    {
      id: 2,
      title: 'Business Models',
      time: '15 min'
    },
    {
      id: 3,
      title: 'Identifying Opportunities',
      time: '15 min'
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="learning-container">

          <h1>{course.title}</h1>

          <p>{course.description}</p>

          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{
                width: `${course.progress}%`
              }}
            ></div>
          </div>

          <p>{course.progress}% Completed</p>

          <h2>Course Chapters</h2>

          {chapters.map(chapter => (

            <div
              key={chapter.id}
              className="chapter-card"
            >

              <div>

                <h3>{chapter.title}</h3>

                <p>
                  Estimated Time: {chapter.time}
                </p>

              </div>

              <button
                onClick={() =>
                  navigate(
                    `/learning/chapter/${chapter.id}`
                  )
                }
              >
                Start
              </button>

            </div>

          ))}

        </div>

      </div>

    </div>
  )
}

export default CourseDetail