import Sidebar from '../components/Sidebar'
import '../css/MoodTracker.css'

function MoodTracker() {

  const moodHistory = [
    {
      mood: '😊 Happy',
      date: 'Today'
    },
    {
      mood: '😐 Neutral',
      date: 'Yesterday'
    },
    {
      mood: '😔 Sad',
      date: '2 Days Ago'
    }
  ]

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="mood-container">

          <h1>Mood Tracker 🌱</h1>

          <p className="mood-subtitle">
            Understanding your emotions is part of learning.
          </p>

          <div className="mood-card">

            <h2>
              How are you feeling today?
            </h2>

            <div className="mood-options">

              <button className="mood-btn happy">
                😊 Happy
              </button>

              <button className="mood-btn neutral">
                😐 Neutral
              </button>

              <button className="mood-btn sad">
                😔 Sad
              </button>

            </div>

          </div>

          <div className="journal-card">

            <h2>Mood Journal</h2>

            <textarea
              placeholder="Write a few thoughts about your day..."
              rows="5"
            />

            <button className="save-mood-btn">
              Save Mood
            </button>

          </div>

          <div className="history-card">

            <h2>Recent Mood History</h2>

            {moodHistory.map((item, index) => (

              <div
                key={index}
                className="history-row"
              >

                <span>{item.mood}</span>

                <span>{item.date}</span>

              </div>

            ))}

          </div>

        </div>

      </div>

    </div>
  )
}

export default MoodTracker