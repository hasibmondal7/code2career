export default function ProtectedRoute({ isAuthenticated, children, fallback = null }) {
  return isAuthenticated ? children : fallback
}
