import axios from 'axios'

const BASE_URL = 'https://registrational-jessenia-sleevelike.ngrok-free.dev'

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 20000,
  headers: {
    'ngrok-skip-browser-warning': 'true',
    'Content-Type': 'application/json'
  }
})

client.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/web/#/login'
    }
    return Promise.reject(err)
  }
)

export default client
export { BASE_URL }
