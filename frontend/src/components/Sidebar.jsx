import { NavLink } from 'react-router-dom'
import './Sidebar.css'

function Sidebar() {
  return (
    <div className="sidebar">

      <h2>UP-SELF</h2>

      <ul>

        <li>
          <NavLink
            to="/dashboard"
            end
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            🏠 Dashboard
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/learning"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            📚 Learning
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/assignments"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            📝 Assignments
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/goals"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            🎯 Goals
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/mood"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            😊 Mood Tracker
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/communication"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            💬 Communication
          </NavLink>
        </li>

        <li>
          <NavLink
            to="/ai"
            className={({ isActive }) =>
              isActive ? "menu-link active-link" : "menu-link"
            }
          >
            🤖 AI Assistant
          </NavLink>
        </li>

        

      </ul>

    </div>
  )
}

export default Sidebar