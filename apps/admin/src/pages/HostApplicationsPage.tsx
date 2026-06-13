import { useState, useEffect, useCallback } from 'react';
import { get, post } from '../services/apiClient';
import type { ApiResponse, PageResponse, HostApplicationResponse } from '../types/api';

// ─── Reject reason modal ─────────────────────────────────────────────────────
function RejectModal({
  app,
  onCancel,
  onConfirm,
}: {
  app: HostApplicationResponse;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}) {
  const [reason, setReason] = useState('');
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.65)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24,
    }} onClick={(e) => e.target === e.currentTarget && onCancel()}>
      <div style={{
        background: 'var(--bg-card)', border: '1px solid rgba(255,100,100,0.3)',
        borderRadius: 'var(--radius-lg)', padding: '36px 32px', maxWidth: 440, width: '100%',
        boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
      }}>
        <h2 style={{ marginBottom: 8 }}>❌ Reject Application</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: 20, fontSize: '0.92rem' }}>
          Rejecting host application from <strong>{app.userFirstName} {app.userLastName}</strong> ({app.userEmail})
        </p>
        <div className="form-group" style={{ marginBottom: 24 }}>
          <label className="form-label">Rejection Reason <span style={{ color: '#ef4444' }}>*</span></label>
          <textarea
            className="form-input"
            rows={3}
            placeholder="e.g. Incomplete information provided, please reapply with full details."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            style={{ resize: 'vertical', fontFamily: 'inherit' }}
          />
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onCancel}>Cancel</button>
          <button
            className="btn btn-primary"
            style={{ flex: 1, background: '#ef4444', borderColor: '#ef4444' }}
            disabled={!reason.trim()}
            onClick={() => onConfirm(reason.trim())}
          >
            Confirm Rejection
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Status badge ─────────────────────────────────────────────────────────────
function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, { bg: string; color: string }> = {
    PENDING:  { bg: 'rgba(251,191,36,0.15)',  color: '#fbbf24' },
    APPROVED: { bg: 'rgba(67,217,140,0.15)',  color: '#43D98C' },
    REJECTED: { bg: 'rgba(239,68,68,0.15)',   color: '#ef4444' },
  };
  const style = colors[status] ?? { bg: 'rgba(108,99,255,0.15)', color: '#6C63FF' };
  return (
    <span style={{
      background: style.bg, color: style.color,
      padding: '3px 10px', borderRadius: 20,
      fontSize: '0.78rem', fontWeight: 600, letterSpacing: '0.03em',
    }}>
      {status}
    </span>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────
export default function HostApplicationsPage() {
  const [apps, setApps] = useState<HostApplicationResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null); // id of app being actioned
  const [rejectTarget, setRejectTarget] = useState<HostApplicationResponse | null>(null);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const showToast = (msg: string, type: 'success' | 'error') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchApps = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const res = await get<ApiResponse<PageResponse<HostApplicationResponse>>>(
        `/api/v1/admin/host-applications?page=${p}&size=10`
      );
      setApps(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (e: any) {
      showToast(e.message || 'Failed to load applications', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchApps(page); }, [page, fetchApps]);

  const handleApprove = async (app: HostApplicationResponse) => {
    setActionLoading(app.id);
    try {
      await post<ApiResponse<HostApplicationResponse>>(
        `/api/v1/admin/host-applications/${app.id}/approve`, {}
      );
      showToast(`✅ ${app.userFirstName} ${app.userLastName} approved as Host`, 'success');
      fetchApps(page);
    } catch (e: any) {
      showToast(e.message || 'Approval failed', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (reason: string) => {
    if (!rejectTarget) return;
    const app = rejectTarget;
    setRejectTarget(null);
    setActionLoading(app.id);
    try {
      await post<ApiResponse<HostApplicationResponse>>(
        `/api/v1/admin/host-applications/${app.id}/reject`,
        { reason }
      );
      showToast(`❌ Application from ${app.userFirstName} rejected`, 'success');
      fetchApps(page);
    } catch (e: any) {
      showToast(e.message || 'Rejection failed', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="page">
      <div className="container section">

        {/* Toast */}
        {toast && (
          <div style={{
            position: 'fixed', top: 24, right: 24, zIndex: 2000,
            background: toast.type === 'success' ? 'rgba(67,217,140,0.15)' : 'rgba(239,68,68,0.15)',
            border: `1px solid ${toast.type === 'success' ? '#43D98C' : '#ef4444'}`,
            color: toast.type === 'success' ? '#43D98C' : '#ef4444',
            padding: '14px 20px', borderRadius: 10,
            fontWeight: 600, fontSize: '0.9rem',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
            animation: 'fadeIn 0.2s ease',
          }}>
            {toast.msg}
          </div>
        )}

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>📋 Host Applications</h1>
            <p className="text-secondary">
              Review and approve or reject guest requests to become a Host.
              {totalElements > 0 && <span style={{ marginLeft: 8, color: '#fbbf24', fontWeight: 600 }}>{totalElements} total</span>}
            </p>
          </div>
          <a href="#/" className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.85rem' }}>
            🏡 Back to Dashboard
          </a>
        </div>

        {/* Table */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {loading ? (
            <div style={{ padding: 32, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[1, 2, 3, 4].map(i => <div key={i} className="skeleton" style={{ height: 52, borderRadius: 8 }} />)}
            </div>
          ) : apps.length === 0 ? (
            <div style={{ padding: '56px 32px', textAlign: 'center' }}>
              <div style={{ fontSize: 48, marginBottom: 12 }}>🎉</div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '1rem' }}>No pending host applications.</p>
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ background: 'rgba(108,99,255,0.06)', borderBottom: '1px solid var(--border-color)' }}>
                  {['Applicant', 'Email', 'Applied On', 'Status', 'Actions'].map(h => (
                    <th key={h} style={{
                      textAlign: 'left', padding: '14px 20px',
                      color: 'var(--text-secondary)', fontWeight: 600,
                      fontSize: '0.8rem', letterSpacing: '0.05em', textTransform: 'uppercase',
                    }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {apps.map((app, idx) => {
                  const isActioning = actionLoading === app.id;
                  const isPending = app.status === 'PENDING';
                  return (
                    <tr
                      key={app.id}
                      style={{
                        borderBottom: idx < apps.length - 1 ? '1px solid var(--border-color)' : 'none',
                        transition: 'background 0.15s',
                        opacity: isActioning ? 0.5 : 1,
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'rgba(108,99,255,0.04)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                          <div style={{
                            width: 36, height: 36, borderRadius: '50%',
                            background: 'rgba(108,99,255,0.18)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontWeight: 700, fontSize: '0.95rem', color: 'var(--brand-primary)', flexShrink: 0,
                          }}>
                            {app.userFirstName?.[0]}{app.userLastName?.[0]}
                          </div>
                          <span style={{ fontWeight: 600 }}>{app.userFirstName} {app.userLastName}</span>
                        </div>
                      </td>
                      <td style={{ padding: '14px 20px', color: 'var(--text-secondary)' }}>{app.userEmail}</td>
                      <td style={{ padding: '14px 20px', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        {app.createdDate
                          ? new Date(app.createdDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
                          : '—'}
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <StatusBadge status={app.status} />
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        {isPending ? (
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button
                              id={`approve-app-${app.id}`}
                              className="btn btn-primary"
                              style={{ fontSize: '0.78rem', padding: '6px 14px', minWidth: 80 }}
                              disabled={isActioning}
                              onClick={() => handleApprove(app)}
                            >
                              {isActioning ? '…' : '✅ Approve'}
                            </button>
                            <button
                              id={`reject-app-${app.id}`}
                              className="btn btn-outline"
                              style={{
                                fontSize: '0.78rem', padding: '6px 14px', minWidth: 80,
                                borderColor: '#ef4444', color: '#ef4444',
                              }}
                              disabled={isActioning}
                              onClick={() => setRejectTarget(app)}
                            >
                              ❌ Reject
                            </button>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontStyle: 'italic' }}>
                            {app.status === 'APPROVED' ? 'Already approved' : 'Already rejected'}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{
              display: 'flex', justifyContent: 'center', alignItems: 'center',
              gap: 8, padding: '16px 20px', borderTop: '1px solid var(--border-color)',
            }}>
              <button className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 14px' }}
                disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.88rem' }}>
                Page {page + 1} of {totalPages}
              </span>
              <button className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 14px' }}
                disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
            </div>
          )}
        </div>
      </div>

      {/* Reject modal */}
      {rejectTarget && (
        <RejectModal
          app={rejectTarget}
          onCancel={() => setRejectTarget(null)}
          onConfirm={handleReject}
        />
      )}

      <style>{`
        @keyframes fadeIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
    </div>
  );
}
