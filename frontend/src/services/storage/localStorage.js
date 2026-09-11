export function getItem(key) {
  return window.localStorage.getItem(key)
}

export function setItem(key, value) {
  window.localStorage.setItem(key, value)
}

export function removeItem(key) {
  window.localStorage.removeItem(key)
}

export function clear() {
  window.localStorage.clear()
}

export default {
  getItem,
  setItem,
  removeItem,
  clear,
}
