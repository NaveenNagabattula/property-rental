import { useState, useEffect } from 'react';
import { get, post, del } from '../services/apiClient';
import type { ApiResponse, PageResponse, UserResponse } from '../types/api';

// ─── Delete Confirmation Modal ────────────────────────────────────────────────
function DeleteConfirmModal({
  user,
  onCancel,
  onDeleted,
}: {
  user: UserResponse;
  onCancel: () => void;
  onDeleted: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDelete = async () => {
    setLoading(true);
    setError(null);
    try {
      await del(`/api/v1/admin/users/${user.id}`);
      onDeleted();
    } catch (err) {
      const e = err as { message?: string };
      setError(e.message ?? 'Failed to delete user.');
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 1000,
        background: 'rgba(0,0,0,0.75)', backdropFilter: 'blur(6px)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24,
      }}
      onClick={e => e.target === e.currentTarget && onCancel()}
    >
      <div style={{
        background: 'var(--bg-card)', border: '1px solid rgba(239,68,68,0.35)',
        borderRadius: 'var(--radius-lg)', padding: '36px 32px',
        maxWidth: 440, width: '100%',
        boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
      }}>
        <div style={{ fontSize: 48, textAlign: 'center', marginBottom: 16 }}>🗑️</div>
        <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Delete User Account?</h2>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 6, lineHeight: 1.6 }}>
          You are about to permanently delete:
        </p>
        <p style={{ textAlign: 'center', fontWeight: 700, marginBottom: 4 }}>
          {user.firstName} {user.lastName}
        </p>
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: '0.88rem', marginBottom: 20 }}>
          {user.email}
        </p>
        <div style={{
          background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.25)',
          borderRadius: 'var(--radius-sm)', padding: '12px 16px', marginBottom: 24,
          fontSize: '0.84rem', color: '#ef4444', lineHeight: 1.6,
        }}>
          ⚠️ This action is <strong>permanent and irreversible</strong>. All sessions will be invalidated immediately.
        </div>
        {error && <div className="alert alert-error" style={{ marginBottom: 16 }}>{error}</div>}
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onCancel} disabled={loading}>
            Cancel
          </button>
          <button
            id={`confirm-delete-${user.id}`}
            className="btn btn-primary"
            style={{ flex: 1, background: '#ef4444', borderColor: '#ef4444' }}
            onClick={handleDelete}
            disabled={loading}
          >
            {loading ? 'Deleting...' : '🗑️ Delete Permanently'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function UserManagementPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [draftSearch, setDraftSearch] = useState('');
  const [actionId, setActionId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchUsers = () => {
    setLoading(true);
    const query = search ? `&query=${encodeURIComponent(search)}` : '';
    get<ApiResponse<PageResponse<UserResponse>>>(`/api/v1/admin/users?page=${page}&size=10${query}`)
      .then(res => {
        setUsers(res.data.content);
        setTotalPages(res.data.totalPages);
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { fetchUsers(); }, [page, search]);

  const handleToggleActive = async (user: UserResponse) => {
    setActionId(user.id);
    setActionError(null);
    try {
      const action = user.active ? 'deactivate' : 'activate';
      await post(`/api/v1/admin/users/${user.id}/${action}`, {});
      showToast(
        user.active
          ? `${user.firstName} ${user.lastName} has been deactivated.`
          : `${user.firstName} ${user.lastName} has been activated.`
      );
      fetchUsers();
    } catch (err) {
      const e = err as { message?: string };
      setActionError(e.message ?? 'Action failed');
    } finally {
      setActionId(null);
    }
  };

  const handleDeleted = () => {
    const name = deleteTarget ? `${deleteTarget.firstName} ${deleteTarget.lastName}` : 'User';
    setDeleteTarget(null);
    showToast(`${name} has been permanently deleted.`);
    // If we deleted the last item on this page, go back one page
    if (users.length === 1 && page > 0) {
      setPage(p => p - 1);
    } else {
      fetchUsers();
    }
  };

  const ROLE_BADGE: Record<string, string> = {
    SUPER_ADMIN: 'badge-danger',
    PROPERTY_MANAGER: 'badge-warning',
    HOST: 'badge-info',
    GUEST: 'badge-success',
    SUPPORT_AGENT: 'badge-info',
  };

  return (
    <div className="page">
      {/* Toast */}
      {toast && (
        <div style={{
          position: 'fixed', top: 80, right: 24, zIndex: 2000,
          background: toast.type === 'success' ? 'rgba(67,217,140,0.15)' : 'rgba(239,68,68,0.15)',
          border: `1px solid ${toast.type === 'success' ? '#43D98C' : '#ef4444'}`,
          color: toast.type === 'success' ? '#43D98C' : '#ef4444',
          padding: '14px 20px', borderRadius: 10,
          fontWeight: 600, fontSize: '0.9rem',
          boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
          animation: 'fadeIn 0.2s ease',
          maxWidth: 380,
        }}>
          {toast.msg}
        </div>
      )}

      <div className="container section">
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>User Management</h1>
            <p className="text-secondary">Search, view, and manage platform users.</p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <form
              id="user-search-form"
              onSubmit={e => { e.preventDefault(); setPage(0); setSearch(draftSearch); }}
              style={{ display: 'flex', gap: 10 }}
            >
              <input
                id="user-search-input"
                className="form-input"
                style={{ width: 260 }}
                placeholder="Search by email or name…"
                value={draftSearch}
                onChange={e => setDraftSearch(e.target.value)}
              />
              <button id="user-search-btn" type="submit" className="btn btn-primary" style={{ padding: '10px 20px' }}>
                🔍
              </button>
            </form>
            <a href="#/" className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.85rem' }}>
              🏡 Back to Dashboard
            </a>
          </div>
        </div>

        {error && <div className="alert alert-error" style={{ marginBottom: 20 }}>{error}</div>}
        {actionError && <div className="alert alert-error" style={{ marginBottom: 20 }}>{actionError}</div>}

        <div className="card" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', color: 'var(--text-secondary)' }}>
                {['Name', 'Email', 'Role', 'Status', 'Verified', 'Actions'].map(h => (
                  <th key={h} style={{ textAlign: 'left', padding: '14px 16px', fontWeight: 600 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading
                ? Array.from({ length: 6 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 6 }).map((__, j) => (
                        <td key={j} style={{ padding: '14px 16px' }}>
                          <div className="skeleton" style={{ height: 14, borderRadius: 4 }} />
                        </td>
                      ))}
                    </tr>
                  ))
                : users.length === 0
                  ? (
                    <tr>
                      <td colSpan={6} style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        No users found.
                      </td>
                    </tr>
                  )
                  : users.map(u => (
                    <tr
                      key={u.id}
                      style={{ borderTop: '1px solid var(--border-color)', transition: 'background 0.15s' }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'rgba(108,99,255,0.04)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <td style={{ padding: '12px 16px', fontWeight: 500 }}>
                        {u.firstName} {u.lastName}
                      </td>
                      <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{u.email}</td>
                      <td style={{ padding: '12px 16px' }}>
                        <span className={`badge ${ROLE_BADGE[u.role] ?? 'badge-info'}`}>{u.role}</span>
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <span className={`badge ${u.active ? 'badge-success' : 'badge-danger'}`}>
                          {u.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td style={{ padding: '12px 16px', color: u.emailVerified ? 'var(--brand-accent)' : 'var(--text-muted)' }}>
                        {u.emailVerified ? '✓ Verified' : '✗ Unverified'}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                          {/* Activate / Deactivate */}
                          <button
                            id={`toggle-user-${u.id}`}
                            className={`btn ${u.active ? 'btn-outline' : 'btn-primary'}`}
                            style={{
                              fontSize: '0.76rem', padding: '4px 12px',
                              ...(u.active ? { borderColor: '#f59e0b', color: '#f59e0b' } : {}),
                            }}
                            disabled={actionId === u.id}
                            onClick={() => handleToggleActive(u)}
                          >
                            {actionId === u.id ? '…' : u.active ? '⏸ Deactivate' : '▶ Activate'}
                          </button>

                          {/* Delete — SUPER_ADMIN only (shown to all, enforced on server) */}
                          <button
                            id={`delete-user-${u.id}`}
                            className="btn btn-outline"
                            style={{
                              fontSize: '0.76rem', padding: '4px 10px',
                              borderColor: '#ef4444', color: '#ef4444',
                            }}
                            disabled={actionId === u.id}
                            onClick={() => setDeleteTarget(u)}
                            title="Permanently delete this user (Super Admin only)"
                          >
                            🗑️
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
              }
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex gap-4 items-center" style={{ marginTop: 24 }}>
            <button id="users-prev" className="btn btn-outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
            <span className="text-secondary text-sm">Page {page + 1} of {totalPages}</span>
            <button id="users-next" className="btn btn-outline" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
          </div>
        )}
      </div>

      {/* Delete confirmation modal */}
      {deleteTarget && (
        <DeleteConfirmModal
          user={deleteTarget}
          onCancel={() => setDeleteTarget(null)}
          onDeleted={handleDeleted}
        />
      )}

      <style>{`
        @keyframes fadeIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
    </div>
  );
}
