import { Client } from '@stomp/stompjs'
import env from '../../config/env'
import { appConfig } from '../../config/appConfig'
import { getItem } from '../storage/localStorage'

const CONNECTION_TIMEOUT_MS = 3000

export function subscribeToSubmission(submissionId, onStatus) {
  const token = getItem(appConfig.tokenStorageKey)
  if (!token) {
    return Promise.reject(new Error('Sign in to receive submission updates'))
  }

  return new Promise((resolve, reject) => {
    let settled = false
    let timeout
    const brokerUrl = env.apiUrl.replace(/^http/, 'ws') + '/ws-live-status'
    const client = new Client({
      brokerURL: brokerUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      onConnect: () => {
        window.clearTimeout(timeout)
        const subscription = client.subscribe(
          `/topic/submissions/${submissionId}`,
          (message) => onStatus(message.body),
        )
        settled = true
        resolve(() => {
          subscription.unsubscribe()
          client.deactivate()
        })
      },
      onStompError: (frame) => {
        if (!settled) {
          settled = true
          reject(new Error(frame.headers.message || 'Submission status connection failed'))
        }
      },
      onWebSocketError: () => {
        if (!settled) {
          settled = true
          reject(new Error('Submission status connection failed'))
        }
      },
    })

    timeout = window.setTimeout(() => {
      if (!settled) {
        settled = true
        client.deactivate()
        reject(new Error('Submission status connection timed out'))
      }
    }, CONNECTION_TIMEOUT_MS)

    client.activate()
  })
}
