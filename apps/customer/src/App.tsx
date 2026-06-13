import { useState, useEffect } from 'react';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import ListingDetailPage from './pages/ListingDetailPage';
import BookingCheckoutPage from './pages/BookingCheckoutPage';
import MyTripsPage from './pages/MyTripsPage';
import HostDashboardPage from './pages/HostDashboardPage';
import AuthPage from './pages/AuthPage';
import { get, post, useNavigator } from './services/apiClient';
import type { ApiResponse } from './types/api';
import './index.css';

// ---------------------------------------------------------------------------
// Decode JWT payload to read role (no signature verification needed client-side)
// ---------------------------------------------------------------------------
function getTokenRole(): string | null {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    // Backend embeds role as a plain string claim, e.g. "GUEST", "HOST"
    return payload.role ?? null;
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// Become a Host — modal
// ---------------------------------------------------------------------------
function BecomeHostModal({ onClose }: { onClose: () => void }) {
  const [step, setStep] = useState<'confirm' | 'loading' | 'success' | 'error'>('confirm');
  const [errorMsg, setErrorMsg] = useState('');

  const handleApply = async () => {
    setStep('loading');
    try {
      await post<ApiResponse<unknown>>('/api/v1/host-applications/apply', {});
      setStep('success');
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to submit application. Please try again.');
      setStep('error');
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '24px',
    }} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div style={{
        background: 'var(--bg-card)', border: '1px solid rgba(108,99,255,0.3)',
        borderRadius: 'var(--radius-lg)', padding: '40px 36px',
        maxWidth: 480, width: '100%', boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
      }}>
        {step === 'confirm' && (
          <>
            <div style={{ fontSize: 48, textAlign: 'center', marginBottom: 16 }}>🏡</div>
            <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Become a Host</h2>
            <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 24, fontSize: '0.95rem', lineHeight: 1.6 }}>
              Apply to list your property on StayFinder. Once an admin reviews and approves your application, your role will be upgraded to <strong style={{ color: 'var(--brand-primary)' }}>Host</strong>.
            </p>
            <div style={{
              background: 'rgba(108,99,255,0.08)', border: '1px solid rgba(108,99,255,0.2)',
              borderRadius: 'var(--radius-sm)', padding: '16px', marginBottom: 28, fontSize: '0.88rem',
              color: 'var(--text-secondary)', lineHeight: 1.7,
            }}>
              <div>✅ List unlimited properties</div>
              <div>✅ Set your own pricing & availability</div>
              <div>✅ Receive bookings from verified guests</div>
              <div>⏳ Admin review typically takes 1–2 business days</div>
            </div>
            <div style={{ display: 'flex', gap: 12 }}>
              <button className="btn btn-outline" style={{ flex: 1 }} onClick={onClose}>Cancel</button>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={handleApply}>Submit Application</button>
            </div>
          </>
        )}

        {step === 'loading' && (
          <div style={{ textAlign: 'center', padding: '20px 0' }}>
            <div style={{
              width: 44, height: 44, margin: '0 auto 16px',
              borderRadius: '50%', border: '4px solid var(--border-color)',
              borderTopColor: 'var(--brand-primary)', animation: 'spin 1s linear infinite',
            }} />
            <p style={{ color: 'var(--text-secondary)' }}>Submitting your application...</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        )}

        {step === 'success' && (
          <div style={{ textAlign: 'center', padding: '8px 0' }}>
            <div style={{ fontSize: 56, marginBottom: 16 }}>🎉</div>
            <h2 style={{ marginBottom: 8 }}>Application Submitted!</h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 8, lineHeight: 1.6 }}>
              Your host application is now <strong style={{ color: '#f59e0b' }}>pending review</strong>.
            </p>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', marginBottom: 12, lineHeight: 1.6 }}>
              Once an admin approves your application, your role will be <strong style={{ color: 'var(--brand-primary)' }}>automatically upgraded to Host</strong> — no logout needed!
            </p>
            <div style={{
              background: 'rgba(108,99,255,0.08)', border: '1px solid rgba(108,99,255,0.2)',
              borderRadius: 'var(--radius-sm)', padding: '12px 16px', marginBottom: 24,
              fontSize: '0.82rem', color: 'var(--text-secondary)',
            }}>
              ⚡ The app checks for role changes every 30 seconds and on each page visit.
            </div>
            <button className="btn btn-primary" style={{ width: '100%' }} onClick={onClose}>Got It</button>
          </div>
        )}

        {step === 'error' && (
          <div style={{ textAlign: 'center', padding: '8px 0' }}>
            <div style={{ fontSize: 56, marginBottom: 16 }}>⚠️</div>
            <h2 style={{ marginBottom: 8 }}>Application Failed</h2>
            <div className="alert alert-error" style={{ marginBottom: 24, textAlign: 'left' }}>{errorMsg}</div>
            <div style={{ display: 'flex', gap: 12 }}>
              <button className="btn btn-outline" style={{ flex: 1 }} onClick={onClose}>Close</button>
              <button className="btn btn-primary" style={{ flex: 1 }} onClick={() => setStep('confirm')}>Try Again</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// --- Lightweight unified router (supports both Pathname and Hash) ---
function useRoute(): { page: string; params: Record<string, string> } {
  const path = window.location.pathname;
  const hash = window.location.hash.replace('#', '') || '/';
  const [hashRoute] = hash.split('?');
  const params: Record<string, string> = {};

  // Parse path-based query params
  const pathSearch = new URLSearchParams(window.location.search);
  pathSearch.forEach((v, k) => (params[k] = v));

  // Parse hash-based query params
  const hashQueryStr = hash.includes('?') ? hash.split('?')[1] : '';
  if (hashQueryStr) {
    const hashSearch = new URLSearchParams(hashQueryStr);
    hashSearch.forEach((v, k) => (params[k] = v));
  }

  // Match authentication/verification routes first
  if (path === '/verify-email' || hashRoute === '/verify-email') return { page: 'verify-email', params };
  if (path === '/login' || hashRoute === '/login') return { page: 'login', params };
  if (path === '/register' || hashRoute === '/register') return { page: 'register', params };
  if (path === '/forgot-password' || hashRoute === '/forgot-password') return { page: 'forgot-password', params };
  if (path === '/reset-password' || hashRoute === '/reset-password') return { page: 'reset-password', params };

  // Fallback to standard hash-based routing
  if (hashRoute === '/' || hashRoute === '') return { page: 'home', params };
  if (hashRoute === '/search') return { page: 'search', params };
  if (hashRoute.startsWith('/listing/')) return { page: 'listing', params: { ...params, id: hashRoute.split('/')[2] } };
  if (hashRoute.startsWith('/checkout/')) return { page: 'checkout', params: { ...params, id: hashRoute.split('/')[2] } };
  if (hashRoute === '/my-trips') return { page: 'trips', params };
  if (hashRoute === '/host-dashboard') return { page: 'host-dashboard', params };
  
  return { page: 'home', params };
}

// Override anchor navigation to use hash routing
if (typeof window !== 'undefined') {
  document.addEventListener('click', (e) => {
    const a = (e.target as HTMLElement).closest('a[href]');
    if (!a) return;
    const href = a.getAttribute('href');
    if (!href || href.startsWith('http') || href.startsWith('mailto')) return;
    e.preventDefault();
    window.location.hash = href;
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  });
}

// --- Navbar ---
function Navbar({ page }: { page: string }) {
  const token = localStorage.getItem('accessToken');
  const role = getTokenRole();
  const navigate = useNavigator();
  const [showHostModal, setShowHostModal] = useState(false);

  return (
    <>
      <nav className="navbar">
        <a href="#/" className="navbar-brand">🏡 StayFinder</a>
        <div className="navbar-links">
          {token && (
            <a href="/my-trips" className={`btn btn-ghost ${page === 'trips' ? 'text-brand' : ''}`}>
              My Trips
            </a>
          )}

          {/* Show "Host Dashboard" to logged-in HOSTs */}
          {token && role === 'HOST' && (
            <a
              id="host-dashboard-link"
              href="/host-dashboard"
              className={`btn btn-ghost ${page === 'host-dashboard' ? 'text-brand' : ''}`}
              style={{
                color: 'var(--brand-primary)',
                fontWeight: 600,
                fontSize: '0.88rem',
              }}
            >
              💼 Host Dashboard
            </a>
          )}

          {/* Show "Become a Host" only to logged-in GUESTs */}
          {token && role === 'GUEST' && (
            <button
              id="become-host-btn"
              className="btn btn-ghost"
              style={{
                border: '1px solid rgba(108,99,255,0.35)',
                color: 'var(--brand-primary)',
                fontWeight: 600,
                fontSize: '0.88rem',
              }}
              onClick={() => setShowHostModal(true)}
            >
              🏡 Become a Host
            </button>
          )}

          {token ? (
            <button
              id="logout-btn"
              className="btn btn-outline"
              onClick={() => {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                navigate('#/');
              }}
            >
              Log Out
            </button>
          ) : (
            <a id="login-link" href="/login" className="btn btn-primary">Sign In</a>
          )}
        </div>
      </nav>

      {showHostModal && <BecomeHostModal onClose={() => setShowHostModal(false)} />}
    </>
  );
}

// --- Sync User Role Helper ---
async function checkAndSyncUserRole(onSyncCompleted: () => void) {
  const token = localStorage.getItem('accessToken');
  if (!token) return;

  try {
    const meRes = await get<ApiResponse<{ role: string }>>('/api/v1/auth/me');
    const dbRole = meRes.data.role;

    const payload = JSON.parse(atob(token.split('.')[1]));
    const tokenRole = payload.role;

    if (dbRole && tokenRole && dbRole !== tokenRole) {
      console.log(`Role mismatch detected: token=${tokenRole}, database=${dbRole}. Refreshing...`);
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) return;

      const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';
      const refreshRes = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        const { accessToken: newAccess, refreshToken: newRefresh } = refreshData.data;
        localStorage.setItem('accessToken', newAccess);
        localStorage.setItem('refreshToken', newRefresh);
        console.log(`Tokens successfully updated. New role: ${dbRole}`);
        onSyncCompleted();
      }
    }
  } catch (err) {
    console.warn('Failed to sync user role:', err);
  }
}

// --- App ---
export default function App() {
  const [, forceUpdate] = useState(0);

  // Listen to hash and page navigation changes for re-rendering
  useEffect(() => {
    const handleNavigation = () => forceUpdate(n => n + 1);
    window.addEventListener('hashchange', handleNavigation);
    window.addEventListener('popstate', handleNavigation);
    return () => {
      window.removeEventListener('hashchange', handleNavigation);
      window.removeEventListener('popstate', handleNavigation);
    };
  }, []);

  const { page, params } = useRoute();

  // Automatically check and sync user role when route/page changes
  useEffect(() => {
    if (localStorage.getItem('accessToken')) {
      checkAndSyncUserRole(() => {
        forceUpdate(n => n + 1);
      });
    }
  }, [page]);

  // Periodic role-sync every 30 seconds — catches promotions while user stays on the same page
  useEffect(() => {
    const interval = setInterval(() => {
      if (localStorage.getItem('accessToken')) {
        checkAndSyncUserRole(() => {
          forceUpdate(n => n + 1);
        });
      }
    }, 30_000);
    return () => clearInterval(interval);
  }, []);

  const isAuthRoute = ['login', 'register', 'forgot-password', 'reset-password', 'verify-email'].includes(page);

  return (
    <>
      <Navbar page={page} />
      {isAuthRoute && <AuthPage initialMode={page as any} params={params} />}
      {page === 'home' && <HomePage />}
      {page === 'search' && <SearchPage params={params} />}
      {page === 'listing' && params.id && <ListingDetailPage propertyId={params.id} />}
      {page === 'checkout' && params.id && <BookingCheckoutPage bookingId={params.id} />}
      {page === 'trips' && <MyTripsPage />}
      {page === 'host-dashboard' && (
        getTokenRole() === 'HOST' ? (
          <HostDashboardPage />
        ) : (
          <div className="page" style={{ textAlign: 'center', padding: 100 }}>
            <h2>Access Denied</h2>
            <p className="text-secondary">Only registered hosts can access this page.</p>
            <a href="#/" className="btn btn-primary" style={{ marginTop: 20, display: 'inline-block' }}>Go Home</a>
          </div>
        )
      )}
    </>
  );
}
