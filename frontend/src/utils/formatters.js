export const formatNumber = (value) => Number(value || 0).toLocaleString()
export const formatDate = (value) => value ? new Date(value).toLocaleDateString() : '—'
