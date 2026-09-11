export default function PublicRoute({ isAuthenticated, children, fallback = null }) {
  return isAuthenticated ? fallback : children
}
