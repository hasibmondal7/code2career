import { useEffect, useState } from 'react'
import { useDebounce } from '../../../hooks/useDebounce'
import {
  deleteAdminUser,
  getAdminUser,
  searchAdminUsers,
  updateAdminUser,
} from '../api/adminUserApi'
import './AdminUsersPanel.css'

const PAGE_SIZE = 10

function getErrorMessage(error) {
  return error.response?.data?.message || 'The account request failed. Please try again.'
}

export default function AdminUsersPanel() {
  const [query, setQuery] = useState('')
  const [role, setRole] = useState('')
  const [active, setActive] = useState('')
  const [page, setPage] = useState(0)
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [usersPage, setUsersPage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [roleDrafts, setRoleDrafts] = useState({})
  const [selectedUser, setSelectedUser] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const debouncedQuery = useDebounce(query)

  useEffect(() => {
    let cancelled = false
    searchAdminUsers({
      query: debouncedQuery.trim(),
      role: role || undefined,
      active: active === '' ? undefined : active === 'true',
      page,
      size: PAGE_SIZE,
    })
      .then((result) => {
        if (cancelled) return
        if (page > 0 && result.totalPages > 0 && page >= result.totalPages) {
          setLoading(true)
          setPage(result.totalPages - 1)
          return
        }
        setUsersPage(result)
      })
      .catch((requestError) => {
        if (!cancelled) {
          setUsersPage(null)
          setError(getErrorMessage(requestError))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [active, debouncedQuery, page, refreshVersion, role])

  const reload = () => {
    setError('')
    setLoading(true)
    setRefreshVersion((version) => version + 1)
  }

  const updateAccount = async (user, changes, confirmation) => {
    if (confirmation && !window.confirm(confirmation)) return
    setError('')
    setNotice('')
    try {
      await updateAdminUser(user.id, changes)
      setNotice(`Account for ${user.username} was updated.`)
      reload()
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    }
  }

  const removeAccount = async (user) => {
    if (!window.confirm(`Permanently delete ${user.username} (${user.email}) and their account data?`)) return
    setError('')
    setNotice('')
    try {
      await deleteAdminUser(user.id)
      setNotice(`Account for ${user.username} was deleted.`)
      reload()
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    }
  }

  const openDetails = async (userId) => {
    setSelectedUser(null)
    setDetailLoading(true)
    setError('')
    try {
      setSelectedUser(await getAdminUser(userId))
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      setDetailLoading(false)
    }
  }

  const users = usersPage?.content ?? []
  const totalPages = usersPage?.totalPages ?? 0

  return (
    <section className="panel admin-users-panel" aria-labelledby="admin-users-title">
      <div className="admin-users-heading">
        <div>
          <p className="eyebrow">ACCOUNT DIRECTORY</p>
          <h2 id="admin-users-title">User management</h2>
          <p className="muted">Search accounts, review details, and manage access.</p>
        </div>
        <span className="admin-users-total">{usersPage?.totalElements ?? '—'} accounts</span>
      </div>

      <div className="admin-users-filters">
        <label className="admin-users-search">
          <span aria-hidden="true">⌕</span>
          <input
            type="search"
            value={query}
            onChange={(event) => { setLoading(true); setError(''); setQuery(event.target.value); setPage(0) }}
            placeholder="Search by username or email"
            aria-label="Search users by username or email"
          />
        </label>
        <label>
          <span className="sr-only">Filter by role</span>
          <select value={role} onChange={(event) => { setLoading(true); setError(''); setRole(event.target.value); setPage(0) }}>
            <option value="">All roles</option>
            <option value="USER">User</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>
        <label>
          <span className="sr-only">Filter by account status</span>
          <select value={active} onChange={(event) => { setLoading(true); setError(''); setActive(event.target.value); setPage(0) }}>
            <option value="">All statuses</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </label>
      </div>

      {notice && <p className="admin-users-notice" role="status">{notice}</p>}
      {error && <p className="admin-users-error" role="alert">{error}</p>}

      <div className="admin-users-table-wrap">
        <table className="admin-users-table">
          <thead>
            <tr>
              <th scope="col">Account</th>
              <th scope="col">Role</th>
              <th scope="col">Status</th>
              <th scope="col">Progress</th>
              <th scope="col"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {loading ? Array.from({ length: 4 }, (_, index) => (
              <tr key={`loading-${index}`} aria-hidden="true">
                <td colSpan="5"><span className="admin-users-skeleton" /></td>
              </tr>
            )) : users.map((user) => (
              <tr key={user.id}>
                <td>
                  <div className="admin-user-identity">
                    <span className="admin-user-avatar" aria-hidden="true">{user.username.slice(0, 2).toUpperCase()}</span>
                    <span><strong>{user.username}</strong><small>{user.email}</small></span>
                  </div>
                </td>
                <td>
                  <div className="admin-role-control">
                    <select
                      aria-label={`Role for ${user.username}`}
                      value={roleDrafts[user.id] ?? user.role}
                      onChange={(event) => setRoleDrafts((drafts) => ({ ...drafts, [user.id]: event.target.value }))}
                    >
                      <option value="USER">User</option>
                      <option value="ADMIN">Admin</option>
                    </select>
                    {(roleDrafts[user.id] ?? user.role) !== user.role && (
                      <button
                        type="button"
                        onClick={() => {
                          const nextRole = roleDrafts[user.id]
                          void updateAccount(user, { role: nextRole })
                          setRoleDrafts((drafts) => {
                            const nextDrafts = { ...drafts }
                            delete nextDrafts[user.id]
                            return nextDrafts
                          })
                        }}
                      >Save</button>
                    )}
                  </div>
                </td>
                <td><span className={`admin-account-status ${user.active ? 'is-active' : 'is-inactive'}`}>{user.active ? 'Active' : 'Inactive'}</span></td>
                <td><span className="admin-user-progress">Lvl {user.level ?? 1} · {(user.xp ?? 0).toLocaleString()} XP</span></td>
                <td>
                  <div className="admin-user-actions">
                    <button type="button" onClick={() => openDetails(user.id)}>Details</button>
                    <button
                      type="button"
                      onClick={() => updateAccount(
                        user,
                        { active: !user.active },
                        `${user.active ? 'Deactivate' : 'Activate'} ${user.username}'s account?`,
                      )}
                    >{user.active ? 'Deactivate' : 'Activate'}</button>
                    <button className="is-danger" type="button" onClick={() => removeAccount(user)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
            {!loading && users.length === 0 && (
              <tr><td colSpan="5"><div className="admin-users-empty">
                <strong>{error ? 'Accounts could not be loaded' : 'No accounts found'}</strong>
                <span>{error ? 'Check the connection and retry.' : 'Try a different search or filter.'}</span>
                {error && <button type="button" onClick={reload}>Retry</button>}
              </div></td></tr>
            )}
          </tbody>
        </table>
      </div>

      <footer className="admin-users-pagination">
        <span>{usersPage?.totalElements ?? 0} accounts · Page {totalPages ? page + 1 : 0} of {totalPages}</span>
        <div>
          <button type="button" onClick={() => { setLoading(true); setError(''); setPage((current) => Math.max(0, current - 1)) }} disabled={loading || page === 0}>Previous</button>
          <button type="button" onClick={() => { setLoading(true); setError(''); setPage((current) => current + 1) }} disabled={loading || page + 1 >= totalPages}>Next</button>
        </div>
      </footer>

      {detailLoading && <div className="admin-user-detail-loading" role="status">Loading account details…</div>}
      {selectedUser && (
        <div className="admin-user-detail-backdrop" onClick={() => setSelectedUser(null)}>
          <section
            className="admin-user-detail"
            role="dialog"
            aria-modal="true"
            aria-labelledby="admin-user-detail-title"
            onClick={(event) => event.stopPropagation()}
          >
            <button type="button" className="admin-user-detail-close" aria-label="Close account details" onClick={() => setSelectedUser(null)}>×</button>
            <p className="eyebrow">ACCOUNT DETAILS</p>
            <h3 id="admin-user-detail-title">{selectedUser.username}</h3>
            <dl>
              <div><dt>Email</dt><dd>{selectedUser.email}</dd></div>
              <div><dt>Role</dt><dd>{selectedUser.role}</dd></div>
              <div><dt>Status</dt><dd>{selectedUser.active ? 'Active' : 'Inactive'}</dd></div>
              <div><dt>Level</dt><dd>{selectedUser.level ?? 1}</dd></div>
              <div><dt>Experience</dt><dd>{(selectedUser.xp ?? 0).toLocaleString()} XP</dd></div>
              <div><dt>Current streak</dt><dd>{selectedUser.currentStreak ?? 0} days</dd></div>
              <div><dt>Joined</dt><dd>{selectedUser.createdAt ? new Date(selectedUser.createdAt).toLocaleDateString() : '—'}</dd></div>
            </dl>
          </section>
        </div>
      )}
    </section>
  )
}
