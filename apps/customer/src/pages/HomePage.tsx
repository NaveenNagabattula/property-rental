import { useState, useEffect } from 'react';
import { get, useNavigator } from '../services/apiClient';
import type { ApiResponse, PageResponse, PropertySummary } from '../types/api';

// Decode role from localStorage JWT (same helper logic as App.tsx)
function getTokenRole(): string | null {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.role ?? null;
  } catch {
    return null;
  }
}

// Category structure with premium visual styling
const CATEGORIES = [
  { type: 'APARTMENT', label: 'Apartments', desc: 'Modern city living', icon: '🏙️', bg: 'linear-gradient(135deg, #6C63FF22, #6C63FF05)' },
  { type: 'HOUSE', label: 'Houses', desc: 'Spacious & cozy stays', icon: '🏡', bg: 'linear-gradient(135deg, #43D98C22, #43D98C05)' },
  { type: 'VILLA', label: 'Villas', desc: 'Luxury private getaways', icon: '🏰', bg: 'linear-gradient(135deg, #FF658422, #FF658405)' },
  { type: 'STUDIO', label: 'Studios', desc: 'Chic spaces for travelers', icon: '🎨', bg: 'linear-gradient(135deg, #fbbf2422, #fbbf2405)' },
];

export default function HomePage() {
  const navigate = useNavigator();
  const token = localStorage.getItem('accessToken');
  const role = getTokenRole();

  // Search form state
  const [location, setLocation] = useState('');
  const [guests, setGuests] = useState('');
  const [propertyType, setPropertyType] = useState('');

  // Featured stays state
  const [featured, setFeatured] = useState<PropertySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    get<ApiResponse<PageResponse<PropertySummary>>>('/api/v1/properties?page=0&size=4', { skipAuth: true })
      .then(res => {
        if (active) {
          setFeatured(res.data.content);
        }
      })
      .catch(err => {
        if (active) {
          setError(err.message || 'Failed to load featured properties');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => { active = false; };
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const query = new URLSearchParams();
    if (location) query.set('location', location);
    if (guests) query.set('guests', guests);
    if (propertyType) query.set('propertyType', propertyType);
    
    // Redirect to search route with params
    navigate(`#/search?${query.toString()}`);
  };

  const handleCategoryClick = (type: string) => {
    navigate(`#/search?propertyType=${type}`);
  };

  return (
    <div className="page" style={{ paddingBottom: 80 }}>
      {/* Hero Header Section */}
      <div style={{
        position: 'relative',
        background: 'linear-gradient(180deg, rgba(108,99,255,0.08) 0%, rgba(13,15,26,0) 100%)',
        padding: '80px 24px 60px',
        textAlign: 'center',
      }}>
        <div className="container" style={{ maxWidth: 800 }}>
          <h1 style={{
            fontSize: '3.2rem',
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: '-0.03em',
            marginBottom: 16,
            background: 'linear-gradient(135deg, #fff 40%, var(--text-secondary) 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            Find your perfect stay
          </h1>
          <p style={{
            color: 'var(--text-secondary)',
            fontSize: '1.2rem',
            maxWidth: 580,
            margin: '0 auto 40px',
            lineHeight: 1.6,
          }}>
            Explore cozy apartments, private houses, luxury villas, and smart studios tailored to your next trip.
          </p>

          {/* Inline Premium Search Bar */}
          <form 
            id="hero-search-form"
            onSubmit={handleSearchSubmit} 
            className="card"
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: 12,
              padding: 16,
              background: 'rgba(19, 22, 42, 0.8)',
              backdropFilter: 'blur(20px)',
              border: '1.5px solid var(--border-color)',
              borderRadius: 'var(--radius-lg)',
              alignItems: 'center',
              boxShadow: '0 24px 64px rgba(0,0,0,0.6)',
            }}
          >
            <div style={{ flex: 1, minWidth: 160, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 4 }}>
              <label htmlFor="home-loc" className="form-label" style={{ fontSize: '0.7rem' }}>Where</label>
              <input
                id="home-loc"
                type="text"
                className="form-input"
                placeholder="Goa, Mumbai, Delhi..."
                value={location}
                onChange={e => setLocation(e.target.value)}
                style={{ background: 'transparent', border: 'none', padding: '6px 0', fontSize: '0.95rem' }}
              />
            </div>

            <div style={{ width: '1px', height: 40, background: 'var(--border-color)', alignSelf: 'center' }} />

            <div style={{ flex: 1, minWidth: 130, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 4 }}>
              <label htmlFor="home-guests" className="form-label" style={{ fontSize: '0.7rem' }}>Guests</label>
              <input
                id="home-guests"
                type="number"
                min={1}
                placeholder="Number of guests"
                value={guests}
                onChange={e => setGuests(e.target.value)}
                style={{ background: 'transparent', border: 'none', padding: '6px 0', fontSize: '0.95rem' }}
              />
            </div>

            <div style={{ width: '1px', height: 40, background: 'var(--border-color)', alignSelf: 'center' }} />

            <div style={{ flex: 1, minWidth: 140, display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 4 }}>
              <label htmlFor="home-type" className="form-label" style={{ fontSize: '0.7rem' }}>Type</label>
              <select
                id="home-type"
                className="form-select"
                value={propertyType}
                onChange={e => setPropertyType(e.target.value)}
                style={{ background: 'transparent', border: 'none', padding: '6px 0', fontSize: '0.95rem' }}
              >
                <option value="">Any type</option>
                <option value="APARTMENT">Apartment</option>
                <option value="HOUSE">House</option>
                <option value="VILLA">Villa</option>
                <option value="STUDIO">Studio</option>
              </select>
            </div>

            <button id="hero-search-btn" type="submit" className="btn btn-primary" style={{ padding: '14px 28px', borderRadius: 'var(--radius-sm)' }}>
              🔍 Search Stays
            </button>
          </form>
        </div>
      </div>

      {/* Category Browser Section */}
      <div className="container" style={{ marginTop: 40 }}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: 20 }}>Browse by property type</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 20 }}>
          {CATEGORIES.map(c => (
            <div
              key={c.type}
              id={`cat-card-${c.type.toLowerCase()}`}
              className="card"
              onClick={() => handleCategoryClick(c.type)}
              style={{
                background: c.bg,
                padding: '24px',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
                textAlign: 'left',
                border: '1px solid var(--border-color)',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
                transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.borderColor = 'var(--brand-primary)';
                e.currentTarget.style.boxShadow = 'var(--shadow-hover)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.borderColor = 'var(--border-color)';
                e.currentTarget.style.boxShadow = 'var(--shadow-card)';
              }}
            >
              <span style={{ fontSize: '2.5rem', display: 'block' }}>{c.icon}</span>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: 4 }}>{c.label}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{c.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Featured Properties Section */}
      <div className="container" style={{ marginTop: 60 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div>
            <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Featured Stays</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', marginTop: 4 }}>Handpicked properties for a memorable trip</p>
          </div>
          <a id="view-all-stays-btn" href="#/search" className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 16px' }}>
            Explore All
          </a>
        </div>

        {error && <div className="alert alert-error" role="alert">{error}</div>}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 24 }}>
          {loading ? (
            Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="card" style={{ overflow: 'hidden' }}>
                <div className="skeleton" style={{ height: 180 }} />
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div className="skeleton" style={{ height: 20, borderRadius: 4 }} />
                  <div className="skeleton" style={{ height: 14, width: '60%', borderRadius: 4 }} />
                  <div className="skeleton" style={{ height: 14, width: '45%', borderRadius: 4 }} />
                </div>
              </div>
            ))
          ) : featured.length === 0 ? (
            <p style={{ color: 'var(--text-secondary)', gridColumn: '1/-1', textAlign: 'center', padding: '40px 0' }}>
              No properties available right now.
            </p>
          ) : (
            featured.map(property => (
              <a 
                key={property.id} 
                href={`#/listing/${property.id}`} 
                className="card property-card" 
                style={{ display: 'block' }}
              >
                <div className="property-card-img" style={{ position: 'relative', height: 180, overflow: 'hidden' }}>
                  {property.thumbnailUrl ? (
                    <img 
                      src={property.thumbnailUrl} 
                      alt={property.title} 
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
                      loading="lazy" 
                    />
                  ) : (
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      height: '100%',
                      background: 'linear-gradient(135deg, var(--bg-surface), var(--bg-input))',
                    }}>
                      <span style={{ fontSize: 32 }}>🏡</span>
                    </div>
                  )}
                  <span className="badge badge-info property-type-badge" style={{ position: 'absolute', top: 12, left: 12 }}>
                    {property.propertyType}
                  </span>
                </div>
                <div className="property-card-body" style={{ padding: 16 }}>
                  <h3 className="property-card-title" style={{ fontSize: '1.05rem', fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {property.title}
                  </h3>
                  <p className="property-card-location" style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: '4px 0 8px' }}>
                    📍 {property.city}
                  </p>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>
                      ₹{property.pricePerNight.toLocaleString('en-IN')}
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 400 }}> / night</span>
                    </span>
                    {property.averageRating ? (
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 4 }}>
                        ⭐ {property.averageRating.toFixed(1)}
                      </span>
                    ) : (
                      <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>No reviews</span>
                    )}
                  </div>
                </div>
              </a>
            ))
          )}
        </div>
      </div>

      {/* Hosting CTA Banner Section */}
      <div className="container" style={{ marginTop: 80 }}>
        <div style={{
          background: 'linear-gradient(135deg, rgba(108,99,255,0.15) 0%, rgba(255,101,132,0.05) 100%)',
          border: '1.5px solid var(--border-color)',
          borderRadius: 'var(--radius-lg)',
          padding: '48px 40px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 32,
          boxShadow: '0 16px 48px rgba(0,0,0,0.3)',
        }}>
          <div style={{ flex: '1 1 400px' }}>
            <h2 style={{ fontSize: '2.1rem', fontWeight: 800, marginBottom: 12 }}>Earn money as a StayFinder Host</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', lineHeight: 1.6 }}>
              Share your room, villa, or secondary home with travelers and start generating passive income today. We handle the process from bookings to payouts.
            </p>
          </div>

          <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
            {token ? (
              role === 'HOST' ? (
                <a href="#/host-dashboard" className="btn btn-primary" style={{ padding: '14px 28px' }}>
                  💼 Host Dashboard
                </a>
              ) : (
                <button 
                  className="btn btn-primary" 
                  style={{ padding: '14px 28px' }}
                  onClick={() => {
                    const btn = document.getElementById('become-host-btn');
                    if (btn) btn.click();
                  }}
                >
                  🏡 Become a Host
                </button>
              )
            ) : (
              <a href="#/login" className="btn btn-primary" style={{ padding: '14px 28px' }}>
                🔑 Sign In to Host
              </a>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
