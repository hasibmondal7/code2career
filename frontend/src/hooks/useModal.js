import { useState } from 'react'

export function useModal(initialOpen = false) {
  const [open, setOpen] = useState(initialOpen)
  return { open, openModal: () => setOpen(true), closeModal: () => setOpen(false), setOpen }
}
