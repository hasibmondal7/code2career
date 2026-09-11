import axios from 'axios'
import { env } from '../../config/env'
import { attachAuthInterceptor } from './interceptors'

export const API_URL = env.apiUrl

const api = attachAuthInterceptor(axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
}))

export default api
