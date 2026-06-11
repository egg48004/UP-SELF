import Sidebar from '../../components/Sidebar'
import '../../css/Learning.css'
import { useParams } from 'react-router-dom'

function ChapterDetail() {

  const { id } = useParams()

  return (
    <div className="layout">

      <Sidebar />

      <div className="main-content">

        <div className="learning-container">

          <div className="chapter-focus-card">

            <h1>
              Chapter {id}
            </h1>

            <h2>
              Identifying Opportunities
            </h2>

            <p>
              Estimated Time: 15 Minutes
            </p>

          </div>

          <div className="chapter-content">

            <h3>
              What is an Opportunity?
            </h3>

            <p>
              An opportunity is a situation where
              a problem can be solved or a need
              can be fulfilled through a product
              or service.
            </p>

            <p>
              Successful entrepreneurs learn how
              to recognise unmet needs and create
              value for customers.
            </p>

            <h3>
              Quick Reflection
            </h3>

            <p>
              Think of one problem you experience
              in daily life. Could it become a
              business opportunity?
            </p>

          </div>

          <div className="chapter-actions">

            <button>
              Previous
            </button>

            <button className="complete-btn">
              Mark Complete ✓
            </button>

            <button>
              Next
            </button>

          </div>

        </div>

      </div>

    </div>
  )
}

export default ChapterDetail