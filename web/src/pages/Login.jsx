import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await client.post('/login', { email, password })
      if (res.data?.role === 'employee') {
        setError('Acest portal este doar pentru administratori.')
        return
      }
      login(res.data)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.detail || 'Email sau parolă incorectă')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="#6B5CE7" strokeWidth="2">
            <path d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h1>Gestiune Firmă</h1>
        <p>Panou de administrare</p>
        <form className="login-form" onSubmit={handleSubmit}>
          <input
            type="email" placeholder="Email" value={email} required
            onChange={e => setEmail(e.target.value)} autoComplete="email"
          />
          <input
            type="password" placeholder="Parolă" value={password} required
            onChange={e => setPassword(e.target.value)} autoComplete="current-password"
          />
          {error && <p className="login-error">{error}</p>}
          <button className="login-submit" disabled={loading}>
            {loading ? 'Se conectează...' : 'Conectare'}
          </button>
        </form>
      </div>
    </div>
  )
}
