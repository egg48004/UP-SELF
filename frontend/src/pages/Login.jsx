import { useNavigate } from 'react-router-dom'

function Login() {
    const navigate = useNavigate()
  return (
    <div>

      <h1>UP-SELF</h1>

      <input
        type="email"
        placeholder="Email"
      />

      <br /><br />

      <input
        type="password"
        placeholder="Password"
      />

      <br /><br />

      <button onClick={() => navigate('/dashboard')}>
        Login
      </button>

    </div>
  )
}

export default Login