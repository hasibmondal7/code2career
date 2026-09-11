export default function Button({ children, className = '', ...props }) {
  return <button className={`primary-button ${className}`.trim()} {...props}>{children}</button>
}
