export default function Loader({ label = 'Loading...' }) {
  return <div className="empty-state" role="status">{label}</div>
}
