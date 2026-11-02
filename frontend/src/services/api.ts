import axios from 'axios';
import { readAuthToken } from '@/services/authStorage'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  const token = readAuthToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
