import { useMemo } from 'react'
import { AppStoreContext } from './context'
import { createStore } from './store'

export function AppProviders({ children }) {
  const store = useMemo(() => createStore(), [])
  return <AppStoreContext.Provider value={store}>{children}</AppStoreContext.Provider>
}
