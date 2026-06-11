import Sidebar from '../components/Sidebar'
import '../css/AIAssistant.css'

function AIAssistant() {

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="ai-container">

          <div className="ai-card">

            <div className="ai-icon">
              🤖
            </div>

            <h1>AI Learning Assistant</h1>

            <p className="ai-subtitle">
              Personalized support for neurodiverse learners.
            </p>

            <div className="coming-soon-box">

              <h2>🚧 Coming Soon</h2>

              <p>
                Our AI Assistant is currently under development.
              </p>

              <p>
                Future features will include:
              </p>

              <ul>
                <li>✨ Personalized learning guidance</li>
                <li>✨ Assignment support</li>
                <li>✨ Motivation and encouragement</li>
                <li>✨ Goal planning assistance</li>
                <li>✨ Neurodiversity-friendly learning tips</li>
              </ul>

            </div>

            <button className="notify-btn">
              Stay Tuned
            </button>

          </div>

        </div>

      </div>

    </div>
  )
}

export default AIAssistant