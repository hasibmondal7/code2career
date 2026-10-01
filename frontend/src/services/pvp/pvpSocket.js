import { Client } from '@stomp/stompjs'
import env from '../../config/env'

export function subscribeToPvpRoom(inviteCode, onRoom, onError) {
  return new Promise((resolve, reject) => {
    let settled = false
    const brokerUrl = env.apiUrl.replace(/^http/, 'ws') + '/ws-live-status-sockjs'
    const client = new Client({
      brokerURL: brokerUrl,
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
