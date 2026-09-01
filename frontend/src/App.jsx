import { useState } from 'react'
import axios from 'axios'
import './App.css'

function App() {
  const [user, setUser] = useState({ username: '', email: '', password: '' })
  const [message, setMessage] = useState('')

  const handleChange = (e) => {
    setUser({ ...user, [e.target.name]: e.target.value })
  }

  const handleRegister = async (e) => {
    e.preventDefault()
    try {
      const response = await axios.post('http://localhost:8080/api/users/register', user)
      setMessage(`Success! User created with ID: ${response.data.id}`)
    } catch (error) {
      setMessage('Registration failed. Check console.')
      console.error(error)
    }
  }

  return (
      <div className="App">
        <h1>Code2Career</h1>
        <h2>Create Developer Profile</h2>
        <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '10px', width: '300px', margin: '0 auto' }}>
          <input type="text" name="username" placeholder="Username" onChange={handleChange} required />
          <input type="email" name="email" placeholder="Email" onChange={handleChange} required />
          <input type="password" name="password" placeholder="Password" onChange={handleChange} required />
          <button type="submit">Register</button>
        </form>
        {message && <p>{message}</p>}
      </div>
  )
}

export default App