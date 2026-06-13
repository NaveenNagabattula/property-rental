import { useState, useEffect } from 'react';
import { get, post, useNavigator } from '../services/apiClient';
import type { ApiResponse, BookingResponse } from '../types/api';

interface BookingCheckoutPageProps { bookingId: string }

declare global {
  interface Window {
    // Razorpay SDK injected via CDN script tag
    Razorpay: new (options: Record<string, unknown>) => { open(): void };
  }
}

export default function BookingCheckoutPage({ bookingId }: BookingCheckoutPageProps) {
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [paymentSuccess, setPaymentSuccess] = useState(false);

  useEffect(() => {
    get<ApiResponse<BookingResponse>>(`/api/v1/bookings/${bookingId}`)
      .then(res => setBooking(res.data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [bookingId]);

  // Dev mode: no real Razorpay key configured
  const razorpayKey = import.meta.env.VITE_RAZORPAY_KEY_ID ?? '';
  const isDevMode = !razorpayKey || razorpayKey === 'rzp_test_REPLACE_ME' || razorpayKey === 'rzp_test_dummykey';

  const handlePay = async () => {
    if (!booking) return;
    setPaymentLoading(true);
    setPaymentError(null);

    try {
      // 1. Initiate payment — gets Razorpay order details (or mock order in dev mode)
      const orderRes = await post<ApiResponse<{
        bookingId: string;
        razorpayOrderId: string;
        amount: number;
        currency: string;
      }>>(`/api/v1/bookings/${bookingId}/initiate-payment`, {});

      const order = orderRes.data;

      // 2a. DEV MODE — skip Razorpay widget, auto-verify with mock data
      if (isDevMode || order.razorpayOrderId.startsWith('mock_order_')) {
        await post('/api/v1/payments/verify', {
          bookingId: booking.id,
          razorpayOrderId: order.razorpayOrderId,
          razorpayPaymentId: 'mock_pay_' + Date.now(),
          razorpaySignature: 'mock_signature_dev',
        });
        setPaymentSuccess(true);
        setPaymentLoading(false);
        return;
      }

      // 2b. PRODUCTION — open Razorpay checkout modal
      const rzp = new window.Razorpay({
        key: razorpayKey,
        amount: Math.round(order.amount * 100), // paise
        currency: order.currency,
        order_id: order.razorpayOrderId,
        name: 'PropertyRental',
        description: `Booking #${booking.id.slice(0, 8)}`,
        prefill: { name: `${booking.guestFirstName} ${booking.guestLastName}` },
        theme: { color: '#6C63FF' },
        handler: async (response: {
          razorpay_order_id: string;
          razorpay_payment_id: string;
          razorpay_signature: string;
        }) => {
          // 3. Verify payment signature with our backend
          try {
            await post('/api/v1/payments/verify', {
              bookingId: booking.id,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            setPaymentSuccess(true);
          } catch (verifyErr) {
            const err = verifyErr as { message?: string };
            setPaymentError(err.message ?? 'Payment verification failed');
          } finally {
            setPaymentLoading(false);
          }
        },
        modal: {
          ondismiss: () => setPaymentLoading(false),
        },
      });
      rzp.open();
    } catch (err) {
      const error = err as { message?: string };
      setPaymentError(error.message ?? 'Payment initiation failed');
      setPaymentLoading(false);
    }
  };

  if (loading) return (
    <div className="container section">
      {[200, 120, 80].map((h, i) => (
        <div key={i} className="skeleton" style={{ height: h, borderRadius: 12, marginBottom: 16 }} />
      ))}
    </div>
  );

  if (error || !booking) return (
    <div className="container section">
      <div className="alert alert-error" role="alert">{error ?? 'Booking not found'}</div>
    </div>
  );

  if (paymentSuccess) return <PaymentSuccessView booking={booking} />;

  const nights = Math.round(
    (new Date(booking.checkOutDate).getTime() - new Date(booking.checkInDate).getTime()) / 86400000
  );
  const pricePerNight = (Number(booking.totalPrice) - Number(booking.platformFee)) / Math.max(nights, 1);

  return (
    <div className="page">
      <div className="container section">
        <a href="#/" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: 16, textDecoration: 'none' }}>
          ← Back to Home
        </a>
        <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: 32 }}>Confirm & Pay</h1>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: 48, alignItems: 'start' }}>
          {/* ---- Left: Booking Summary ---- */}
          <div>
            <div className="card" style={{ padding: 28, marginBottom: 24 }}>
              <h2 className="font-semibold" style={{ marginBottom: 16 }}>Your Trip</h2>
              {booking.thumbnailUrl ? (
                <img src={booking.thumbnailUrl} alt={booking.propertyTitle}
                  style={{ width: '100%', height: 200, objectFit: 'cover', borderRadius: 12, marginBottom: 20 }} />
              ) : (
                <div style={{
                  width: '100%', height: 200, borderRadius: 12, marginBottom: 20,
                  background: 'linear-gradient(135deg, var(--bg-surface), var(--bg-input))',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 48,
                  border: '1px solid var(--border-color)'
                }}>🏡</div>
              )}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
                <div>
                  <p className="form-label" style={{ marginBottom: 4 }}>Property</p>
                  <p className="font-medium">{booking.propertyTitle}</p>
                  <p className="text-sm text-secondary">{booking.propertyAddress}</p>
                </div>
                <div>
                  <p className="form-label" style={{ marginBottom: 4 }}>Dates</p>
                  <p className="font-medium">{booking.checkInDate} → {booking.checkOutDate}</p>
                  <p className="text-sm text-secondary">{nights} night{nights > 1 ? 's' : ''}</p>
                </div>
                <div>
                  <p className="form-label" style={{ marginBottom: 4 }}>Guests</p>
                  <p className="font-medium">{booking.guestCount} guest{booking.guestCount > 1 ? 's' : ''}</p>
                </div>
                <div>
                  <p className="form-label" style={{ marginBottom: 4 }}>Status</p>
                  <span className="badge badge-warning">{booking.status}</span>
                </div>
              </div>
            </div>
          </div>

          {/* ---- Right: Price & Pay ---- */}
          <div className="card" style={{ padding: 28, position: 'sticky', top: 80 }}>
            <h2 className="font-semibold" style={{ marginBottom: 20 }}>Price Details</h2>

            <div className="flex justify-between text-sm text-secondary" style={{ marginBottom: 8 }}>
              <span>₹{pricePerNight.toFixed(0)} × {nights} night{nights > 1 ? 's' : ''}</span>
              <span>₹{(Number(booking.totalPrice) - Number(booking.platformFee)).toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm text-secondary" style={{ marginBottom: 12 }}>
              <span>Platform fee</span>
              <span>₹{Number(booking.platformFee).toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold"
              style={{ borderTop: '1px solid var(--border-color)', paddingTop: 14, marginBottom: 24 }}>
              <span>Total</span>
              <span className="text-brand" style={{ fontSize: '1.2rem' }}>
                ₹{Number(booking.totalPrice).toLocaleString()}
              </span>
            </div>

            {paymentError && (
              <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{paymentError}</div>
            )}

            <button
              id="pay-now-btn"
              className="btn btn-primary"
              style={{ width: '100%', fontSize: '1rem', padding: '14px' }}
              onClick={handlePay}
              disabled={paymentLoading || booking.status !== 'PENDING'}
            >
              {paymentLoading
                ? (isDevMode ? 'Simulating Payment…' : 'Opening Payment…')
                : (isDevMode
                    ? `⚡ Simulate Payment — ₹${Number(booking.totalPrice).toLocaleString()}`
                    : `Pay ₹${Number(booking.totalPrice).toLocaleString()}`)}
            </button>

            {isDevMode && booking.status === 'PENDING' && (
              <div style={{
                marginTop: 12,
                padding: '8px 12px',
                borderRadius: 8,
                background: 'rgba(255,193,7,0.12)',
                border: '1px solid rgba(255,193,7,0.35)',
                fontSize: '0.78rem',
                color: 'var(--text-secondary)',
                textAlign: 'center',
                lineHeight: 1.5,
              }}>
                ⚙️ <strong>Dev Mode</strong> — No real Razorpay key set.<br />
                Payment will be simulated and confirmed automatically.
              </div>
            )}

            {booking.status !== 'PENDING' && (
              <p className="text-sm text-secondary" style={{ textAlign: 'center', marginTop: 10 }}>
                This booking is already {booking.status.toLowerCase()}.
              </p>
            )}

            <p className="text-muted text-sm" style={{ textAlign: 'center', marginTop: 12 }}>
              {isDevMode ? '⚙️ Dev Mode — Payments Simulated' : '🔒 Secured by Razorpay'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function PaymentSuccessView({ booking }: { booking: BookingResponse }) {
  const navigate = useNavigator();
  const [seconds, setSeconds] = useState(7);

  useEffect(() => {
    const timer = setInterval(() => {
      setSeconds(s => {
        if (s <= 1) {
          clearInterval(timer);
          navigate('#/');
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [navigate]);

  return (
    <div className="container section" style={{ textAlign: 'center', padding: '80px 24px' }}>
      <div style={{ fontSize: 72, marginBottom: 24 }}>🎉</div>
      <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: 12 }}>Payment Confirmed!</h1>
      <p className="text-secondary" style={{ marginBottom: 8 }}>
        Your booking for <strong>{booking.propertyTitle}</strong> is confirmed.
      </p>
      <p className="text-secondary" style={{ marginBottom: 24 }}>
        Check-in: <strong>{booking.checkInDate}</strong> · Check-out: <strong>{booking.checkOutDate}</strong>
      </p>
      
      <div style={{
        maxWidth: 320,
        margin: '0 auto 36px',
        padding: '10px 16px',
        borderRadius: 8,
        background: 'rgba(108,99,255,0.08)',
        border: '1px solid rgba(108,99,255,0.2)',
        fontSize: '0.88rem',
        color: 'var(--text-secondary)',
      }}>
        ⏱️ Redirecting to homepage in <strong style={{ color: 'var(--brand-primary)', fontSize: '1rem' }}>{seconds}</strong> seconds...
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
        <a id="view-trips-btn" href="#/my-trips" className="btn btn-primary">
          View My Trips
        </a>
        <a
          id="go-home-btn"
          href="#/"
          className="btn btn-outline"
        >
          Go to Home Page
        </a>
      </div>
    </div>
  );
}
