import api from '../../../services/api/axios'

export async function searchAdminUsers(params) {
  const { data } = await api.get('/api/admin/users', { params })
  return data
}

export async function getAdminUser(userId) {
  const { data } = await api.get(`/api/admin/users/${userId}`)
  return data
}

export async function updateAdminUser(userId, changes) {
  const { data } = await api.patch(`/api/admin/users/${userId}`, changes)
  return data
}

export async function deleteAdminUser(userId) {
  await api.delete(`/api/admin/users/${userId}`)
}
