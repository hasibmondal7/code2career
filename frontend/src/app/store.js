export function createStore(initialState = {}) {
  let state = initialState
  const listeners = new Set()

  return {
    getState: () => state,
    setState: (nextState) => {
      state = typeof nextState === 'function' ? nextState(state) : nextState
      listeners.forEach((listener) => listener(state))
    },
    subscribe: (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
  }
}
