import React, { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [role, setRole] = useState(localStorage.getItem('user_role'))

  const login = (data) => {
    localStorage.setItem('token', data.access_token)
    localStorage.setItem('user_role', data.role || 'admin')
    localStorage.setItem('firma_id', data.firma_id)
    setToken(data.access_token)
    setRole(data.role || 'admin')
  }

  const logout = () => {
    localStorage.clear()
    setToken(null)
    setRole(null)
  }

  return (
    <AuthContext.Provider value={{ token, role, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
