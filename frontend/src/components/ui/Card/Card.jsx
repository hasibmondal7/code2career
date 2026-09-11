export default function Card({ children, className = '' }) {
  return <section className={`panel ${className}`.trim()}>{children}</section>
}
