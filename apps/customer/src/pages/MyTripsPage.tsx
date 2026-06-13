import { useState, useEffect } from 'react';
import { get, post } from '../services/apiClient';
import type { ApiResponse, PageResponse, BookingResponse } from '../types/api';

const STATUS_BADGE: Record<string, string> = {
  PENDING: 'badge-warning',
  CONFIRMED: 'badge-success',
  COMPLETED: 'badge-success',
  CANCELLED: 'badge-danger',
};

function BookingCard({ booking }: { booking: BookingResponse }) {
  const [cancelling, setCancelling] = useState(false);
  const [cancelled, setCancelled] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const handleCancel = async () => {
    if (!confirm('Are you sure you want to cancel this booking?')) return;
    setCancelling(true);
    setCancelError(null);
    try {
      await post(`/api/v1/bookings/${booking.id}/cancel`, { reason: 'Guest requested cancellation' });
      setCancelled(true);
    } catch (err) {
      const error = err as { message?: string };
      setCancelError(error.message ?? 'Cancellation failed');
    } finally {
      setCancelling(false);
    }
  };

  const status = cancelled ? 'CANCELLED' : booking.status;
  const nights = Math.round(
    (new Date(booking.checkOutDate).getTime() - new Date(booking.checkInDate).getTime()) / 86400000
  );

  return (
    <div className="card" style={{ padding: 24, display: 'flex', gap: 20 }}>
      {booking.thumbnailUrl ? (
        <img src={booking.thumbnailUrl} alt={booking.propertyTitle}
          style={{ width: 120, height: 90, objectFit: 'cover', borderRadius: 10, flexShrink: 0 }} />
      ) : (
        <div style={{
          width: 120, height: 90, borderRadius: 10, flexShrink: 0,
          background: 'linear-gradient(135deg, var(--bg-surface), var(--bg-input))',
          display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 32,
          border: '1px solid var(--border-color)'
        }}>🏡</div>
      )}
      <div style={{ flex: 1 }}>
        <div className="flex justify-between items-center" style={{ marginBottom: 8 }}>
          <h3 className="font-semibold" style={{ fontSize: '1.05rem' }}>{booking.propertyTitle}</h3>
          <span className={`badge ${STATUS_BADGE[status] ?? 'badge-info'}`}>{status}</span>
        </div>
        <p className="text-sm text-secondary" style={{ marginBottom: 6 }}>📍 {booking.propertyAddress}</p>
        <p className="text-sm text-secondary" style={{ marginBottom: 6 }}>
          📅 {booking.checkInDate} → {booking.checkOutDate} ({nights} night{nights > 1 ? 's' : ''})
        </p>
        <p className="text-sm text-secondary" style={{ marginBottom: 12 }}>
          👥 {booking.guestCount} guest{booking.guestCount > 1 ? 's' : ''}
          &nbsp;·&nbsp;
          <strong className="text-brand">₹{Number(booking.totalPrice).toLocaleString()}</strong>
        </p>

        {cancelError && (
          <p className="text-sm" style={{ color: 'var(--brand-secondary)', marginBottom: 8 }}>{cancelError}</p>
        )}

        <div className="flex gap-2">
          {status === 'PENDING' && (
            <a id={`pay-booking-${booking.id}`} href={`#/checkout/${booking.id}`} className="btn btn-primary" style={{ fontSize: '0.82rem', padding: '7px 16px' }}>
              Pay Now
            </a>
          )}
          {(status === 'PENDING' || status === 'CONFIRMED') && !cancelled && (
            <button
              id={`cancel-booking-${booking.id}`}
              className="btn btn-outline btn-danger"
              style={{ fontSize: '0.82rem', padding: '7px 16px', borderColor: '#ef4444', color: '#ef4444' }}
              onClick={handleCancel}
              disabled={cancelling}
            >
              {cancelling ? 'Cancelling…' : 'Cancel'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default function MyTripsPage() {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    get<ApiResponse<PageResponse<BookingResponse>>>(`/api/v1/bookings?page=${page}&size=8`)
      .then(res => {
        setBookings(res.data.content);
        setTotalPages(res.data.totalPages);
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="page">
      <div className="container section">
        <div className="page-header">
          <a href="#/" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: 8, textDecoration: 'none' }}>
            ← Back to Home
          </a>
          <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: 8 }}>My Trips</h1>
          <p className="text-secondary">Manage your bookings and upcoming stays.</p>
        </div>

        {error && <div className="alert alert-error" role="alert">{error}</div>}

        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {[1,2,3].map(i => (
              <div key={i} className="card" style={{ padding: 24, display: 'flex', gap: 20 }}>
                <div className="skeleton" style={{ width: 120, height: 90, borderRadius: 10, flexShrink: 0 }} />
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div className="skeleton" style={{ height: 20, width: '60%' }} />
                  <div className="skeleton" style={{ height: 14, width: '40%' }} />
                  <div className="skeleton" style={{ height: 14, width: '30%' }} />
                </div>
              </div>
            ))}
          </div>
        ) : bookings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <p style={{ fontSize: 56, marginBottom: 16 }}>🧳</p>
            <p className="text-secondary">No trips yet. <a href="#/" className="text-brand">Start exploring</a> properties!</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {bookings.map(b => <BookingCard key={b.id} booking={b} />)}
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination" style={{ marginTop: 32 }}>
            <button id="trips-prev-btn" className="btn btn-outline" disabled={page === 0}
              onClick={() => setPage(p => p - 1)}>← Previous</button>
            <span className="text-secondary text-sm">Page {page + 1} of {totalPages}</span>
            <button id="trips-next-btn" className="btn btn-outline" disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}>Next →</button>
          </div>
        )}
      </div>
    </div>
  );
}
