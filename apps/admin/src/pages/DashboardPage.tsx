import { useState, useEffect } from 'react';
import { get } from '../services/apiClient';
import type { ApiResponse, PageResponse, PropertySummary, UserResponse, HostApplicationResponse } from '../types/api';

interface KpiCard {
  label: string;
  value: string | number;
  icon: string;
  color: string;
}

function KpiCardComponent({ card }: { card: KpiCard }) {
  return (
    <div className="kpi-card" style={{
      background: 'var(--bg-card)',
      border: `1px solid ${card.color}33`,
      borderRadius: 'var(--radius-md)',
      padding: '24px',
      display: 'flex',
      alignItems: 'center',
      gap: 20,
      boxShadow: `0 4px 20px ${card.color}18`,
    }}>
      <div style={{
        width: 56, height: 56, borderRadius: '50%',
        background: `${card.color}1A`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 26, flexShrink: 0,
      }}>
        {card.icon}
      </div>
      <div>
        <p style={{ fontSize: '2rem', fontWeight: 800, color: card.color, lineHeight: 1 }}>{card.value}</p>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: 4 }}>{card.label}</p>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const [pendingListings, setPendingListings] = useState<number | null>(null);
  const [pendingApplications, setPendingApplications] = useState<number | null>(null);
  const [totalUsers, setTotalUsers] = useState<number | null>(null);
  const [recentListings, setRecentListings] = useState<PropertySummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.allSettled([
      get<ApiResponse<PageResponse<PropertySummary>>>('/api/v1/admin/listings/pending?page=0&size=1'),
      get<ApiResponse<PageResponse<HostApplicationResponse>>>('/api/v1/admin/host-applications?page=0&size=1'),
      get<ApiResponse<PageResponse<UserResponse>>>('/api/v1/admin/users?page=0&size=1'),
      get<ApiResponse<PageResponse<PropertySummary>>>('/api/v1/admin/listings/pending?page=0&size=5'),
    ]).then(([pending, apps, users, recent]) => {
      if (pending.status === 'fulfilled') setPendingListings(pending.value.data.totalElements);
      if (apps.status === 'fulfilled') setPendingApplications(apps.value.data.totalElements);
      if (users.status === 'fulfilled') setTotalUsers(users.value.data.totalElements);
      if (recent.status === 'fulfilled') setRecentListings(recent.value.data.content);
    }).finally(() => setLoading(false));
  }, []);

  const kpiCards: KpiCard[] = [
    { label: 'Pending Listings', value: pendingListings ?? '—', icon: '🏠', color: '#fbbf24' },
    { label: 'Pending Host Applications', value: pendingApplications ?? '—', icon: '📋', color: '#6C63FF' },
    { label: 'Total Users', value: totalUsers ?? '—', icon: '👥', color: '#43D98C' },
    { label: 'Listings Reviewed Today', value: '—', icon: '✅', color: '#FF6584' },
  ];

  return (
    <div className="page">
      <div className="container section">
        <div style={{ marginBottom: 32 }}>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>Admin Dashboard</h1>
          <p className="text-secondary">Platform overview and quick actions.</p>
        </div>

        {/* KPI Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 20, marginBottom: 40 }}>
          {loading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="skeleton" style={{ height: 104, borderRadius: 14 }} />
              ))
            : kpiCards.map(c => <KpiCardComponent key={c.label} card={c} />)
          }
        </div>

        {/* Pending Listings Queue */}
        <div className="card" style={{ padding: 28 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
            <h2 style={{ fontWeight: 700 }}>⏳ Pending Listings</h2>
            <a id="view-all-listings-link" href="#/moderation" className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 16px' }}>
              View All
            </a>
          </div>
          {loading ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[1,2,3].map(i => <div key={i} className="skeleton" style={{ height: 48, borderRadius: 8 }} />)}
            </div>
          ) : recentListings.length === 0 ? (
            <p className="text-secondary">No pending listings. 🎉</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)' }}>
                  <th style={{ textAlign: 'left', padding: '8px 12px' }}>Title</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px' }}>Host</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px' }}>City</th>
                  <th style={{ textAlign: 'left', padding: '8px 12px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {recentListings.map(l => (
                  <tr key={l.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '10px 12px' }}>{l.title}</td>
                    <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>
                      {l.hostFirstName} {l.hostLastName}
                    </td>
                    <td style={{ padding: '10px 12px', color: 'var(--text-secondary)' }}>{l.city}</td>
                    <td style={{ padding: '10px 12px' }}>
                      <a id={`review-listing-${l.id}`} href={`#/moderation?id=${l.id}`}
                        className="btn btn-outline" style={{ fontSize: '0.78rem', padding: '4px 12px' }}>
                        Review
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
