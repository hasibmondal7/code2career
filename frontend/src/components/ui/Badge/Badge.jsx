export default function Badge({ children, className = '' }) {
  return <span className={`featured-badge ${className}`.trim()}>{children}</span>
}
