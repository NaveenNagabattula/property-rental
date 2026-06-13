import { useState, useEffect } from 'react';
import DashboardPage from './pages/DashboardPage';
import ListingModerationPage from './pages/ListingModerationPage';
import UserManagementPage from './pages/UserManagementPage';
import HostApplicationsPage from './pages/HostApplicationsPage';
import AuthPage from './pages/AuthPage';
import { useNavigator } from './services/apiClient';
import './index.css';

// --- Lightweight unified router (supports both Pathname and Hash) ---
function useRoute(): { page: string; params: Record<string, string> } {
  const path = window.location.pathname;
  const rawHash = window.location.hash;
  // Normalise: '#/' -> '/', '#' -> '/', '' -> '/'
  const hash = rawHash.replace(/^#/, '') || '/';
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

  // Auth routes
  if (path === '/login' || hashRoute === '/login') return { page: 'login', params };
  if (path === '/forgot-password' || hashRoute === '/forgot-password') return { page: 'forgot-password', params };
  if (path === '/reset-password' || hashRoute === '/reset-password') return { page: 'reset-password', params };

  // Dashboard & moderation routes
  if (hashRoute === '/' || hashRoute === '') return { page: 'dashboard', params };
  if (hashRoute === '/moderation') return { page: 'moderation', params };
  if (hashRoute === '/users') return { page: 'users', params };
  if (hashRoute === '/host-applications') return { page: 'host-applications', params };

  return { page: 'dashboard', params };
}

// Override anchor navigation to use hash routing
if (typeof window !== 'undefined') {
  document.addEventListener('click', (e) => {
    const a = (e.target as HTMLElement).closest('a[href]');
    if (!a) return;
    const href = a.getAttribute('href');
    if (!href || href.startsWith('http') || href.startsWith('mailto') || href.startsWith('#')) return;
    e.preventDefault();
    window.location.hash = href;
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  });
}

// --- Sidebar Nav ---
const NAV_ITEMS = [
  { path: '/', label: '📊 Dashboard', id: 'nav-dashboard' },
  { path: '/moderation', label: '🏠 Moderation', id: 'nav-moderation' },
  { path: '/host-applications', label: '📋 Host Applications', id: 'nav-host-applications' },
  { path: '/users', label: '👥 Users', id: 'nav-users' },
];

function Sidebar({ current }: { current: string }) {
  const navigate = useNavigator();
  return (
    <aside style={{
      width: 240, background: 'var(--bg-card)', borderRight: '1px solid var(--border-color)',
      minHeight: '100vh', padding: '32px 16px', position: 'sticky', top: 0, flexShrink: 0,
    }}>
      <a
        href="#/"
        className="navbar-brand"
        style={{
          display: 'block',
          marginBottom: 40,
          paddingLeft: 8,
          fontSize: '1.1rem',
          textDecoration: 'none',
          color: 'inherit'
        }}
      >
        🔑 Admin Panel
      </a>
      <nav>
        {NAV_ITEMS.map(item => (
          <a
            key={item.path}
            id={item.id}
            href={`#${item.path}`}
            style={{
              display: 'flex', alignItems: 'center', gap: 10,
              padding: '12px 16px', borderRadius: 'var(--radius-sm)',
              color: current === item.path ? 'var(--brand-primary)' : 'var(--text-secondary)',
              background: current === item.path ? 'rgba(108,99,255,0.12)' : 'transparent',
              fontWeight: current === item.path ? 600 : 400,
              fontSize: '0.92rem', marginBottom: 4,
              transition: 'all 0.18s',
              textDecoration: 'none',
              borderLeft: current === item.path ? '3px solid var(--brand-primary)' : '3px solid transparent',
            }}
          >
            {item.label}
          </a>
        ))}
      </nav>
      <div style={{ position: 'absolute', bottom: 24, left: 16, right: 16 }}>
        <button
          id="admin-logout-btn"
          className="btn btn-outline"
          style={{ width: '100%', fontSize: '0.85rem' }}
          onClick={() => {
            localStorage.removeItem('admin_accessToken');
            localStorage.removeItem('admin_refreshToken');
            navigate('/login');
          }}
        >
          🚪 Log Out
        </button>
      </div>
    </aside>
  );
}

// --- App ---
export default function App() {
  const [, forceUpdate] = useState(0);

  // Listen to hash and page navigation changes for re-rendering
  useEffect(() => {
    const handleNavigation = () => forceUpdate(n => n + 1);
    window.addEventListener('hashchange', handleNavigation);
    window.addEventListener('popstate', handleNavigation);
    // Also re-check on storage changes (token set in another tab)
    window.addEventListener('storage', handleNavigation);
    return () => {
      window.removeEventListener('hashchange', handleNavigation);
      window.removeEventListener('popstate', handleNavigation);
      window.removeEventListener('storage', handleNavigation);
    };
  }, []);

  const { page, params } = useRoute();
  // Read token fresh on every render — forceUpdate ensures this stays current
  const token = localStorage.getItem('admin_accessToken');

  const isAuthRoute = ['login', 'forgot-password', 'reset-password'].includes(page);

  if (isAuthRoute) {
    return <AuthPage initialMode={page as 'login' | 'forgot-password' | 'reset-password'} params={params} />;
  }

  if (!token) {
    return <AuthPage initialMode="login" params={params} />;
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar current={page === 'dashboard' ? '/' : page === 'moderation' ? '/moderation' : page === 'host-applications' ? '/host-applications' : '/users'} />
      <main style={{ flex: 1, overflow: 'auto' }}>
        {page === 'dashboard' && <DashboardPage />}
        {page === 'moderation' && <ListingModerationPage params={params} />}
        {page === 'host-applications' && <HostApplicationsPage />}
        {page === 'users' && <UserManagementPage />}
      </main>
    </div>
  );
}
