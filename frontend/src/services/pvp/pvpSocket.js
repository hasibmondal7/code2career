import { Client } from '@stomp/stompjs'
import env from '../../config/env'
import { appConfig } from '../../config/appConfig'
import { getItem } from '../storage/localStorage'

export function subscribeToPvpRoom(inviteCode, onRoom, onError) {
  const token = getItem(appConfig.tokenStorageKey)
  if (!token) {
    return Promise.reject(new Error('Sign in to receive PvP room updates'))
  }

  return new Promise((resolve, reject) => {
    let settled = false
    const brokerUrl = env.apiUrl.replace(/^http/, 'ws') + '/ws-live-status-sockjs'
    const client = new Client({
      brokerURL: brokerUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      onConnect: () => {
        const subscription = client.subscribe(
          `/topic/pvp/rooms/${inviteCode}`,
          (message) => onRoom(JSON.parse(message.body)),
        )
        settled = true
        resolve(() => {
          subscription.unsubscribe()
          client.deactivate()
        })
      },
      onStompError: (frame) => {
        const error = new Error(frame.headers.message || 'PvP room connection failed')
        if (!settled) {
          settled = true
          reject(error)
        } else {
          onError(error)
        }
      },
      onWebSocketError: () => {
        const error = new Error('PvP room connection failed')
        if (!settled) {
          settled = true
          reject(error)
        } else {
          onError(error)
        }
      },
    })

    client.activate()
  })
}
