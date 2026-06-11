import Sidebar from '../components/Sidebar'
import '../css/Communication.css'

function Communication() {

  const messages = [
    {
      sender: 'Mentor Sarah',
      message: 'Great job staying consistent this week. Keep going!'
    },
    {
      sender: 'Community',
      message: 'How do I complete Assignment 2?'
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="communication-container">

          <h1>Communication 💬</h1>

          <p className="communication-subtitle">
            Support is always available when you need it.
          </p>

          <div className="support-section">

            <div className="support-card">

              <h2>👩‍🏫 Mentor Support</h2>

              <p>
                Ask for guidance when you feel stuck.
              </p>

              <button>
                Message Mentor
              </button>

            </div>

            <div className="support-card">

              <h2>👥 Community Support</h2>

              <p>
                Connect with other learners and share experiences.
              </p>

              <button>
                Ask Community
              </button>

            </div>

          </div>

          <div className="messages-card">

            <h2>Recent Messages</h2>

            {messages.map((message, index) => (

              <div
                key={index}
                className="message-row"
              >

                <h3>{message.sender}</h3>

                <p>{message.message}</p>

              </div>

            ))}

          </div>

        </div>

      </div>

    </div>
  )
}

export default Communication