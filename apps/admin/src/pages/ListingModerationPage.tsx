import { useState, useEffect } from 'react';
import { get, post, useNavigator } from '../services/apiClient';
import type { ApiResponse, PageResponse, PropertySummary } from '../types/api';

interface ModerationActionModal {
  type: 'approve' | 'reject' | 'suspend';
  listingId: string;
  title: string;
}

interface ListingModerationPageProps {
  params?: Record<string, string>;
}

export default function ListingModerationPage({ params = {} }: ListingModerationPageProps) {
  const navigate = useNavigator();
  const [listings, setListings] = useState<PropertySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [modal, setModal] = useState<ModerationActionModal | null>(null);
  const [reason, setReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchListings = () => {
    setLoading(true);
    if (params.id) {
      get<ApiResponse<any>>(`/api/v1/properties/${params.id}`)
        .then(res => {
          setListings([res.data]);
          setTotalPages(1);
        })
        .catch(err => setError(err.message))
        .finally(() => setLoading(false));
    } else {
      get<ApiResponse<PageResponse<PropertySummary>>>(`/api/v1/admin/listings/pending?page=${page}&size=10`)
        .then(res => {
          setListings(res.data.content);
          setTotalPages(res.data.totalPages);
        })
        .catch(err => setError(err.message))
        .finally(() => setLoading(false));
    }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps, react-hooks/set-state-in-effect
  useEffect(() => { fetchListings(); }, [page, params.id]);

  const handleAction = async () => {
    if (!modal) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const endpoint = `/api/v1/admin/listings/${modal.listingId}/${modal.type}`;
      await post(endpoint, { reason: reason || `${modal.type} by admin` });
      setModal(null);
      setReason('');
      if (params.id) {
        // Redirect back to dashboard homepage after finishing work
        navigate('#/');
      } else {
        fetchListings();
      }
    } catch (err) {
      const error = err as { message?: string };
      setActionError(error.message ?? 'Action failed');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="container section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Listing Moderation</h1>
            <p className="text-secondary">Review and approve property submissions.</p>
          </div>
          <a href="#/" className="btn btn-outline" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.85rem' }}>
            🏡 Back to Dashboard
          </a>
        </div>

        {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 20 }}>{error}</div>}

        <div className="card" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', color: 'var(--text-secondary)' }}>
                {['Title', 'Host', 'City', 'Type', 'Price/Night', 'Actions'].map(h => (
                  <th key={h} style={{ textAlign: 'left', padding: '14px 16px', fontWeight: 600 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading
                ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 6 }).map((__, j) => (
                        <td key={j} style={{ padding: '14px 16px' }}>
                          <div className="skeleton" style={{ height: 14, borderRadius: 4 }} />
                        </td>
                      ))}
                    </tr>
                  ))
                : listings.length === 0
                  ? (
                    <tr>
                      <td colSpan={6} style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        No pending listings. Moderation queue is empty. 🎉
                      </td>
                    </tr>
                  )
                  : listings.map(l => (
                    <tr key={l.id} style={{ borderTop: '1px solid var(--border-color)' }}>
                      <td style={{ padding: '14px 16px', fontWeight: 500 }}>{l.title}</td>
                      <td style={{ padding: '14px 16px', color: 'var(--text-secondary)' }}>
                        {l.hostFirstName} {l.hostLastName}
                      </td>
                      <td style={{ padding: '14px 16px', color: 'var(--text-secondary)' }}>{l.city}</td>
                      <td style={{ padding: '14px 16px' }}>
                        <span className="badge badge-info">{l.propertyType}</span>
                      </td>
                      <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--brand-primary)' }}>
                        ₹{l.pricePerNight?.toLocaleString()}
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <div className="flex gap-2">
                          <button
                            id={`approve-btn-${l.id}`}
                            className="btn btn-primary"
                            style={{ fontSize: '0.78rem', padding: '5px 14px', background: 'linear-gradient(135deg,#43D98C,#22c55e)' }}
                            onClick={() => setModal({ type: 'approve', listingId: l.id, title: l.title })}
                          >✓ Approve</button>
                          <button
                            id={`reject-btn-${l.id}`}
                            className="btn btn-outline"
                            style={{ fontSize: '0.78rem', padding: '5px 14px', borderColor: '#ef4444', color: '#ef4444' }}
                            onClick={() => setModal({ type: 'reject', listingId: l.id, title: l.title })}
                          >✗ Reject</button>
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
            <button id="mod-prev" className="btn btn-outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
            <span className="text-secondary text-sm">Page {page + 1} of {totalPages}</span>
            <button id="mod-next" className="btn btn-outline" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
          </div>
        )}
      </div>

      {/* ---- Modal ---- */}
      {modal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 200, backdropFilter: 'blur(4px)',
        }}>
          <div className="card" style={{ width: 480, padding: 32, margin: 24 }}>
            <h2 style={{ marginBottom: 8, textTransform: 'capitalize' }}>
              {modal.type} Listing
            </h2>
            <p className="text-secondary" style={{ marginBottom: 20, fontSize: '0.9rem' }}>
              "{modal.title}"
            </p>
            <div className="form-group" style={{ marginBottom: 20 }}>
              <label className="form-label" htmlFor="reason-input">
                Reason {modal.type !== 'approve' ? '(required)' : '(optional)'}
              </label>
              <textarea
                id="reason-input"
                className="form-textarea"
                rows={3}
                value={reason}
                onChange={e => setReason(e.target.value)}
                placeholder={modal.type === 'approve' ? 'Optional note to host…' : 'Reason for rejection…'}
              />
            </div>
            {actionError && <div className="alert alert-error" style={{ marginBottom: 16 }}>{actionError}</div>}
            <div className="flex gap-3">
              <button
                id="confirm-action-btn"
                className="btn btn-primary"
                onClick={handleAction}
                disabled={actionLoading || (modal.type !== 'approve' && !reason.trim())}
                style={{ flex: 1 }}
              >
                {actionLoading ? 'Processing…' : `Confirm ${modal.type}`}
              </button>
              <button id="cancel-modal-btn" className="btn btn-ghost" onClick={() => { setModal(null); setReason(''); }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
