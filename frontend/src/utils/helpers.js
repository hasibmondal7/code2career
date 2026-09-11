export const initials = (value = '') => value.trim().slice(0, 2).toUpperCase()
export const clamp = (value, min, max) => Math.min(max, Math.max(min, value))
