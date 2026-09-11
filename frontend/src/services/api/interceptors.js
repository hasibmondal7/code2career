import { appConfig } from '../../config/appConfig'
import { getItem } from '../storage/localStorage'

export function attachAuthInterceptor(client) {
  client.interceptors.request.use((config) => {
    const token = getItem(appConfig.tokenStorageKey)
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  return client
}
