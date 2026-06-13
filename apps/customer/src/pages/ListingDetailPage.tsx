import { useState, useEffect } from 'react';
import { get, post } from '../services/apiClient';
import type { ApiResponse, PropertyDetail, BookingResponse } from '../types/api';

// Read role from stored JWT without a round-trip
function getTokenRole(): string | null {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    return JSON.parse(atob(token.split('.')[1]))?.role ?? null;
  } catch { return null; }
}

// Simple date diff in days
function daysBetween(a: string, b: string) {
  return Math.max(0, Math.round((new Date(b).getTime() - new Date(a).getTime()) / 86400000));
}

function StarRating({ value }: { value?: number }) {
  const r = Math.round(value ?? 0);
  return (
    <span className="star-rating">
      {[1,2,3,4,5].map(n => <span key={n}>{n <= r ? '★' : '☆'}</span>)}
    </span>
  );
}

interface ListingDetailPageProps { propertyId: string }

export default function ListingDetailPage({ propertyId }: ListingDetailPageProps) {
  const [property, setProperty] = useState<PropertyDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Booking form
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [guests, setGuests] = useState(1);
  const [specialRequests, setSpecialRequests] = useState('');
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [bookingError, setBookingError] = useState<string | null>(null);
  const [bookingLoading, setBookingLoading] = useState(false);

  const [activePhoto, setActivePhoto] = useState(0);

  useEffect(() => {
    get<ApiResponse<PropertyDetail>>(`/api/v1/properties/${propertyId}`, { skipAuth: true })
      .then(res => setProperty(res.data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [propertyId]);

  const nights = checkIn && checkOut ? daysBetween(checkIn, checkOut) : 0;
  const baseAmount = property ? property.pricePerNight * nights : 0;
  const platformFee = baseAmount * 0.1; // 10% estimate shown pre-booking
  const total = baseAmount + platformFee;

  const handleBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!checkIn || !checkOut || nights <= 0) {
      setBookingError('Please select valid check-in and check-out dates.');
      return;
    }
    setBookingLoading(true);
    setBookingError(null);
    try {
      const res = await post<ApiResponse<BookingResponse>>('/api/v1/bookings', {
        propertyId,
        checkInDate: checkIn,
        checkOutDate: checkOut,
        guestCount: guests,
        specialRequests: specialRequests || undefined,
      });
      setBooking(res.data);
    } catch (err) {
      const error = err as { message?: string };
      setBookingError(error.message ?? 'Failed to create booking');
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) return (
    <div className="container section">
      <div className="skeleton" style={{ height: 400, borderRadius: 16, marginBottom: 32 }} />
      <div className="skeleton" style={{ height: 24, width: '50%', marginBottom: 12 }} />
      <div className="skeleton" style={{ height: 14, width: '70%' }} />
    </div>
  );

  if (error || !property) return (
    <div className="container section">
      <div className="alert alert-error" role="alert">{error ?? 'Property not found'}</div>
    </div>
  );

  return (
    <div className="page">
      <div className="container section">
        {/* ---- Breadcrumb ---- */}
        <div style={{ marginBottom: 20, display: 'flex', gap: 12, alignItems: 'center' }}>
          <a href="#/" style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', textDecoration: 'none' }}>Home</a>
          <span style={{ color: 'var(--text-muted)' }}>›</span>
          <a href="#/search" style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', textDecoration: 'none' }}>Search</a>
          <span style={{ color: 'var(--text-muted)' }}>›</span>
          <span style={{ fontSize: '0.82rem', color: 'var(--text-primary)' }}>{property.title}</span>
        </div>

        {/* ---- Photo Gallery ---- */}
        <div className="listing-gallery" style={{ marginBottom: 32 }}>
          <div className="listing-main-photo">
            {property.photoUrls.length > 0 ? (
              <img src={property.photoUrls[activePhoto]} alt={property.title}
                  style={{ width: '100%', height: 420, objectFit: 'cover', borderRadius: 16 }} />
            ) : (
              <div style={{
                height: 420,
                borderRadius: 16,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, var(--bg-surface), var(--bg-input))',
                border: '1px solid var(--border-color)',
                color: 'var(--text-secondary)',
                gap: 12,
              }}>
                <span style={{ fontSize: 64 }}>🏡</span>
                <span style={{ fontWeight: 600, fontSize: '1.1rem' }}>No Photo Available</span>
                <span style={{ fontSize: '0.88rem', opacity: 0.7 }}>This host hasn't uploaded photos yet.</span>
              </div>
            )}
          </div>
          {property.photoUrls.length > 1 && (
            <div className="listing-thumbs flex gap-2" style={{ marginTop: 12 }}>
              {property.photoUrls.slice(0, 5).map((url, i) => (
                <button
                  key={i}
                  id={`thumb-${i}`}
                  onClick={() => setActivePhoto(i)}
                  style={{
                    border: i === activePhoto ? '2px solid var(--brand-primary)' : '2px solid transparent',
                    borderRadius: 8, overflow: 'hidden', cursor: 'pointer', padding: 0, background: 'none',
                  }}
                >
                  <img src={url} alt="" style={{ width: 80, height: 60, objectFit: 'cover', display: 'block' }} />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="listing-layout" style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: 48, alignItems: 'start' }}>
          {/* ---- Left: Details ---- */}
          <div>
            <div className="flex items-center gap-4" style={{ marginBottom: 8 }}>
              <span className="badge badge-info">{property.propertyType}</span>
              <span className="text-sm text-secondary">👥 Up to {property.guestCapacity} guests</span>
            </div>
            <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: 8 }}>{property.title}</h1>
            <p className="text-secondary" style={{ marginBottom: 16 }}>📍 {property.address}, {property.city}</p>

            <div className="flex items-center gap-4" style={{ marginBottom: 24 }}>
              <StarRating value={property.averageRating} />
              <span className="text-secondary text-sm">
                {property.averageRating?.toFixed(1) ?? '—'} · {property.reviewCount} review{property.reviewCount !== 1 ? 's' : ''}
              </span>
            </div>

            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.8, marginBottom: 32 }}>{property.description}</p>

            {/* Amenities */}
            {property.amenities.length > 0 && (
              <div style={{ marginBottom: 32 }}>
                <h2 style={{ marginBottom: 16, fontSize: '1.2rem', fontWeight: 700 }}>Amenities</h2>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                  {property.amenities.map(a => (
                    <span key={a} className="badge badge-info" style={{ fontSize: '0.85rem' }}>✓ {a}</span>
                  ))}
                </div>
              </div>
            )}

            {/* Host */}
            <div className="card" style={{ padding: 20 }}>
              <p className="font-semibold" style={{ marginBottom: 4 }}>Hosted by {property.hostFirstName} {property.hostLastName}</p>
              <p className="text-sm text-secondary">Contact your host after booking confirmation.</p>
            </div>
          </div>

          {/* ---- Right: Booking Widget ---- */}
          <div className="card" style={{ padding: 28, position: 'sticky', top: 80 }}>
            {/* Price header always shown */}
            <div style={{ marginBottom: 20 }}>
              <span style={{ fontSize: '1.5rem', fontWeight: 800 }}>₹{property.pricePerNight.toLocaleString()}</span>
              <span className="text-secondary"> / night</span>
            </div>

            {getTokenRole() === 'HOST' ? (
              /* Hosts cannot book — show informational panel */
              <div style={{
                background: 'rgba(108,99,255,0.08)',
                border: '1px solid rgba(108,99,255,0.25)',
                borderRadius: 'var(--radius-sm)',
                padding: '20px 18px',
                textAlign: 'center',
              }}>
                <div style={{ fontSize: 36, marginBottom: 10 }}>🏠</div>
                <p style={{ fontWeight: 700, marginBottom: 6 }}>You're a Host</p>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                  Hosts cannot make bookings. Switch to a guest account to book properties.
                </p>
              </div>
            ) : booking ? (
              <div>
                <div className="alert alert-success" role="status" style={{ marginBottom: 20 }}>
                  🎉 Booking created! Proceed to payment.
                </div>
                <p className="text-sm text-secondary">Booking ID: <strong>{booking.id}</strong></p>
                <p className="text-sm text-secondary">Status: <strong>{booking.status}</strong></p>
                <a id="go-to-payment-btn" href={`/checkout/${booking.id}`} className="btn btn-primary" style={{ marginTop: 16, width: '100%' }}>
                  → Proceed to Payment
                </a>
              </div>
            ) : (
              <form id="booking-form" onSubmit={handleBook}>
                <div className="form-group" style={{ marginBottom: 14 }}>
                  <label className="form-label" htmlFor="detail-checkin">Check In</label>
                  <input id="detail-checkin" type="date" className="form-input"
                    value={checkIn} onChange={e => setCheckIn(e.target.value)}
                    min={new Date().toISOString().split('T')[0]} required />
                </div>
                <div className="form-group" style={{ marginBottom: 14 }}>
                  <label className="form-label" htmlFor="detail-checkout">Check Out</label>
                  <input id="detail-checkout" type="date" className="form-input"
                    value={checkOut} onChange={e => setCheckOut(e.target.value)}
                    min={checkIn || new Date().toISOString().split('T')[0]} required />
                </div>
                <div className="form-group" style={{ marginBottom: 14 }}>
                  <label className="form-label" htmlFor="detail-guests">Guests</label>
                  <input id="detail-guests" type="number" className="form-input"
                    min={1} max={property.guestCapacity} value={guests}
                    onChange={e => setGuests(Number(e.target.value))} required />
                </div>
                <div className="form-group" style={{ marginBottom: 20 }}>
                  <label className="form-label" htmlFor="detail-requests">Special Requests (optional)</label>
                  <textarea id="detail-requests" className="form-textarea" rows={2}
                    value={specialRequests} onChange={e => setSpecialRequests(e.target.value)}
                    style={{ resize: 'vertical' }} />
                </div>

                {/* Price Breakdown */}
                {nights > 0 && (
                  <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: 16, marginBottom: 16 }}>
                    <div className="flex justify-between text-sm text-secondary" style={{ marginBottom: 6 }}>
                      <span>₹{property.pricePerNight.toLocaleString()} × {nights} night{nights > 1 ? 's' : ''}</span>
                      <span>₹{baseAmount.toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between text-sm text-secondary" style={{ marginBottom: 6 }}>
                      <span>Platform fee (~10%)</span>
                      <span>₹{platformFee.toFixed(0)}</span>
                    </div>
                    <div className="flex justify-between font-semibold" style={{ borderTop: '1px solid var(--border-color)', paddingTop: 10 }}>
                      <span>Estimated Total</span>
                      <span className="text-brand">₹{total.toFixed(0)}</span>
                    </div>
                  </div>
                )}

                {bookingError && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{bookingError}</div>}

                {!localStorage.getItem('accessToken') ? (
                  <a id="login-to-book-btn" href="#/login" className="btn btn-primary" style={{ width: '100%', textAlign: 'center' }}>
                    Sign In to Book
                  </a>
                ) : (
                  <button id="book-now-btn" type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={bookingLoading}>
                    {bookingLoading ? 'Creating Booking…' : 'Book Now'}
                  </button>
                )}
                <p className="text-muted text-sm" style={{ textAlign: 'center', marginTop: 10 }}>
                  You won't be charged yet
                </p>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
