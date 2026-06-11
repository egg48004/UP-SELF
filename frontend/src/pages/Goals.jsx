import Sidebar from '../components/Sidebar'
import '../css/Goals.css'

function Goals() {

  const goals = [
    {
      id: 1,
      title: 'Finish Chapter 3',
      progress: '2 / 3',
      steps: [
        {
          name: 'Read Lesson',
          completed: true
        },
        {
          name: 'Take Notes',
          completed: true
        },
        {
          name: 'Complete Quiz',
          completed: false
        }
      ]
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="goals-container">

          <h1>My Goals 🎯</h1>

          <p className="goal-subtitle">
            Break big goals into small achievable steps.
          </p>

          <div className="create-goal-card">

            <h2>Create New Goal</h2>

            <input
              placeholder="Enter your goal..."
              className="goal-input"
            />

            <button>
              Add Goal
            </button>

          </div>

          {goals.map(goal => (

            <div
              key={goal.id}
              className="goal-card"
            >

              <h2>{goal.title}</h2>

              <p>
                Progress: {goal.progress}
              </p>

              <div className="goal-steps">

                {goal.steps.map((step, index) => (

                  <div
                    key={index}
                    className="step-row"
                  >

                    <span>

                      {step.completed
                        ? '✅'
                        : '⬜'}

                      {' '}

                      {step.name}

                    </span>

                    <button>
                      Complete Step
                    </button>

                  </div>

                ))}

              </div>

              <div className="goal-actions">

                <button>
                  Add Step
                </button>

                <button>
                  Edit Goal
                </button>

                <button>
                  Delete Goal
                </button>

                <button className="complete-goal-btn">
                  Complete Goal 🎉
                </button>

              </div>

            </div>

          ))}

        </div>

      </div>

    </div>
  )
}

export default Goals