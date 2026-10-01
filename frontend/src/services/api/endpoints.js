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

export async function getRoadmaps() {
  const { data } = await api.get('/api/roadmaps')
  return data
}

export async function getRoadmap(roadmapId) {
  const { data } = await api.get(`/api/roadmaps/${roadmapId}`)
  return data
}

export async function createPvpRoom(payload) {
  const { data } = await api.post('/api/pvp/rooms', payload)
  return data
}

export async function joinPvpRoom(inviteCode) {
  const { data } = await api.post(`/api/pvp/rooms/${encodeURIComponent(inviteCode)}/join`)
  return data
}

export async function getPvpRoom(inviteCode) {
  const { data } = await api.get(`/api/pvp/rooms/${encodeURIComponent(inviteCode)}`)
  return data
}

export async function startPvpRoom(inviteCode) {
  const { data } = await api.post(`/api/pvp/rooms/${encodeURIComponent(inviteCode)}/start`)
  return data
}

export async function getOpenPvpRooms() {
  const { data } = await api.get('/api/pvp/rooms/open')
  return data
}

export async function submitPvpCode(inviteCode, code, language = 'JAVA') {
  const { data } = await api.post(`/api/pvp/rooms/${encodeURIComponent(inviteCode)}/submit`, { code, language })
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

export async function runCode(code, customInput, language = 'java') {
  const { data } = await api.post('/api/submissions/run', {
    language,
    code,
    customInput,
  })
  return data
}

export async function submitSolution(problemId, code, language = 'java') {
  const { data } = await api.post('/api/submissions', {
    problemId,
    language,
    code,
  })
  return data
}

export async function updateProfile(username, profilePhoto, bannerPhoto) {
  const { data } = await api.patch('/api/users/me', { username, profilePhoto, bannerPhoto })
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
