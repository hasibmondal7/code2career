export default function Modal({ open, title, children, onClose }) {
  if (!open) return null
  return <div className="modal-backdrop" onClick={onClose}><div className="register-modal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={onClose}>×</button>{title && <h2>{title}</h2>}{children}</div></div>
}
