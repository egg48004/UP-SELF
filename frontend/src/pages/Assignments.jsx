import Sidebar from '../components/Sidebar'
import '../css/Assignments.css'
import { useNavigate } from 'react-router-dom'


function Assignments() {

  const navigate = useNavigate()

  const assignments = [
    {
      id: 1,
      title: 'Business Proposal',
      dueDate: 'Due in 3 Days',
      time: '20 Minutes'
    },
    {
      id: 2,
      title: 'Marketing Reflection',
      dueDate: 'Due Tomorrow',
      time: '15 Minutes'
    },
    {
      id: 3,
      title: 'Finance Case Study',
      dueDate: 'Due Next Week',
      time: '30 Minutes'
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="assignment-container">

          <h1>Assignments</h1>

          <p className="page-description">
            Focus on one task at a time.
          </p>

          <input
            className="search-bar"
            placeholder="Search Assignment..."
          />

          {assignments.map((assignment) => (

            <div
              key={assignment.id}
              className="assignment-card"
            >

              <h2>{assignment.title}</h2>

              <p>⏰ {assignment.dueDate}</p>

              <p>
                Estimated Time: {assignment.time}
              </p>

              <button
                onClick={() =>
                  navigate(
                    `/assignments/${assignment.id}`
                  )
                }
              >
                Continue Assignment
              </button>

            </div>

          ))}

          <div className="history-card">

            <h2>Submission History</h2>

            <p>
              Review your previous submissions
              and feedback.
            </p>

            <button
              onClick={() =>
                navigate('/assignments/history')
              }
            >
              View History
            </button>

          </div>

        </div>

      </div>

    </div>
  )
}

export default Assignments