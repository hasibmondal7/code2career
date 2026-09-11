import api from './axios'
import { appConfig } from '../../config/appConfig'
import { removeItem, setItem } from '../storage/localStorage'

export const API_URL = api.defaults.baseURL

export async function login(credentials) {
  const { data } = await api.post('/api/users/login', credentials)
  setItem(appConfig.tokenStorageKey, data.accessToken)
  return getCurrentUser()
}

export async function register(details) {
  await api.post('/api/users/register', details)
  return login({ email: details.email, password: details.password })
}

export async function getCurrentUser() {
  const { data } = await api.get('/api/users/me')
  return data
}

export async function getLeaderboard() {
  const { data } = await api.get('/api/leaderboard')
  return data
}

export async function getTopics() {
  const { data } = await api.get('/api/topics')
  return data
}

export async function getProblems(topicId) {
  const { data } = await api.get(topicId ? `/api/problems/topic/${topicId}` : '/api/problems')
  return data
}

export async function getBadges() {
  const { data } = await api.get('/api/users/me/badges')
  return data
}

export async function getActivity(from, to) {
  const { data } = await api.get('/api/users/me/activity', { params: { from, to } })
  return data
}

export async function getSubmissions(userId) {
  const { data } = await api.get(`/api/submissions/user/${userId}`)
  return data
}

export async function getSubmission(submissionId) {
  const { data } = await api.get(`/api/submissions/${submissionId}`)
  return data
}

export async function runCode(code, customInput) {
  const { data } = await api.post('/api/submissions/run', {
    language: 'java',
    code,
    customInput,
  })
  return data
}

export async function submitSolution(problemId, code) {
  const { data } = await api.post('/api/submissions', {
    problemId,
    language: 'java',
    code,
  })
  return data
}

export async function updateProfile(username, profilePhoto) {
  const { data } = await api.patch('/api/users/me', { username, profilePhoto })
  return data
}

export async function createTopic(payload) {
  const { data } = await api.post('/api/topics', payload)
  return data
}

export async function createProblem(payload) {
  const { data } = await api.post('/api/problems', payload)
  return data
}

export async function createTestCase(payload) {
  const { data } = await api.post('/api/testcases', payload)
  return data
}

export async function changePassword(currentPassword, newPassword) {
  await api.put('/api/users/password', { currentPassword, newPassword })
}

export function logout() {
  removeItem(appConfig.tokenStorageKey)
}

export default api
