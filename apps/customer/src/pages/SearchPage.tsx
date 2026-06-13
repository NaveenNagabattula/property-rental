import { useState, useEffect } from 'react';
import { get } from '../services/apiClient';
import type { ApiResponse, PageResponse, PropertySummary, PropertySearchRequest } from '../types/api';

// ---- Star Rating Display ----
function StarRating({ value }: { value?: number }) {
  const rounded = Math.round(value ?? 0);
  return (
    <span className="star-rating" aria-label={`${value?.toFixed(1) ?? '—'} stars`}>
      {[1,2,3,4,5].map(n => (
        <span key={n}>{n <= rounded ? '★' : '☆'}</span>
      ))}
    </span>
  );
}

// ---- Property Card ----
function PropertyCard({ property }: { property: PropertySummary }) {
  return (
    <a href={`#/listing/${property.id}`} className="card property-card" style={{ display: 'block' }}>
      <div className="property-card-img">
        {property.thumbnailUrl ? (
          <img src={property.thumbnailUrl} alt={property.title} loading="lazy" />
        ) : (
          <div className="property-card-img-placeholder" aria-hidden
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexDirection: 'column',
              gap: 8,
              height: '100%',
              background: 'linear-gradient(135deg, var(--bg-surface), var(--bg-input))',
              color: 'var(--text-secondary)',
            }}>
            <span style={{ fontSize: 32 }}>🏡</span>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, opacity: 0.7 }}>No Image</span>
          </div>
        )}
        <span className="badge badge-info property-type-badge">{property.propertyType}</span>
      </div>
      <div className="property-card-body">
        <h3 className="property-card-title">{property.title}</h3>
        <p className="property-card-location">📍 {property.city}</p>
        <div className="property-card-meta flex items-center gap-4">
          <span className="flex items-center gap-2">
            <StarRating value={property.averageRating} />
            <span className="text-sm text-secondary">
              {property.averageRating ? property.averageRating.toFixed(1) : '—'}
              {' '}({property.reviewCount})
            </span>
          </span>
          <span className="text-sm text-secondary">👥 {property.guestCapacity}</span>
        </div>
        <div className="property-card-price">
          <span className="price-amount">₹{property.pricePerNight.toLocaleString()}</span>
          <span className="price-per"> / night</span>
        </div>
      </div>
    </a>
  );
}

// ---- Skeleton Card ----
function SkeletonCard() {
  return (
    <div className="card" style={{ overflow: 'hidden' }}>
      <div className="skeleton" style={{ height: 200 }} />
      <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="skeleton" style={{ height: 20, borderRadius: 4 }} />
        <div className="skeleton" style={{ height: 14, width: '60%', borderRadius: 4 }} />
        <div className="skeleton" style={{ height: 14, width: '40%', borderRadius: 4 }} />
      </div>
    </div>
  );
}

// ---- Main SearchPage ----
interface SearchPageProps {
  params?: Record<string, string>;
}

export default function SearchPage({ params = {} }: SearchPageProps) {
  const [filters, setFilters] = useState<PropertySearchRequest>({
    location: params.location || undefined,
    startDate: params.startDate || undefined,
    endDate: params.endDate || undefined,
    guests: params.guests ? parseInt(params.guests, 10) : undefined,
    propertyType: params.propertyType || undefined,
    page: params.page ? parseInt(params.page, 10) : 0,
    size: params.size ? parseInt(params.size, 10) : 12,
  });
  const [draftFilters, setDraftFilters] = useState<PropertySearchRequest>({
    location: params.location || undefined,
    startDate: params.startDate || undefined,
    endDate: params.endDate || undefined,
    guests: params.guests ? parseInt(params.guests, 10) : undefined,
    propertyType: params.propertyType || undefined,
    page: params.page ? parseInt(params.page, 10) : 0,
    size: params.size ? parseInt(params.size, 10) : 12,
  });
  const [properties, setProperties] = useState<PropertySummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync state if URL query params change
  useEffect(() => {
    const synced: PropertySearchRequest = {
      location: params.location || undefined,
      startDate: params.startDate || undefined,
      endDate: params.endDate || undefined,
      guests: params.guests ? parseInt(params.guests, 10) : undefined,
      propertyType: params.propertyType || undefined,
      page: params.page ? parseInt(params.page, 10) : 0,
      size: params.size ? parseInt(params.size, 10) : 12,
    };
    setFilters(synced);
    setDraftFilters(synced);
  }, [params.location, params.startDate, params.endDate, params.guests, params.propertyType, params.page]);

  const buildQuery = (req: PropertySearchRequest) => {
    const params = new URLSearchParams();
    if (req.location) params.set('location', req.location);
    if (req.startDate) params.set('startDate', req.startDate);
    if (req.endDate) params.set('endDate', req.endDate);
    if (req.guests) params.set('guests', String(req.guests));
    if (req.minPrice) params.set('minPrice', String(req.minPrice));
    if (req.maxPrice) params.set('maxPrice', String(req.maxPrice));
    if (req.propertyType) params.set('propertyType', req.propertyType);
    params.set('page', String(req.page ?? 0));
    params.set('size', String(req.size ?? 12));
    return params.toString();
  };

  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError(null);

    get<ApiResponse<PageResponse<PropertySummary>>>(
      `/api/v1/properties?${buildQuery(filters)}`,
      { skipAuth: true }
    )
      .then(res => {
        if (cancelled) return;
        setProperties(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      })
      .catch(err => {
        if (!cancelled) setError(err.message ?? 'Failed to load properties');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [filters]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters({ ...draftFilters, page: 0, size: 12 });
  };

  return (
    <div className="page">
      {/* ---- Search Bar ---- */}
      <div className="search-hero">
        <div className="container">
          <h1 className="search-hero-title">Find your perfect stay</h1>
          <form id="search-form" onSubmit={handleSearch} className="search-bar">
            <div className="form-group">
              <label className="form-label" htmlFor="location-input">Location</label>
              <input
                id="location-input"
                className="form-input"
                placeholder="Mumbai, Goa, Delhi…"
                value={draftFilters.location ?? ''}
                onChange={e => setDraftFilters(f => ({ ...f, location: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="checkin-input">Check In</label>
              <input
                id="checkin-input"
                type="date"
                className="form-input"
                value={draftFilters.startDate ?? ''}
                onChange={e => setDraftFilters(f => ({ ...f, startDate: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="checkout-input">Check Out</label>
              <input
                id="checkout-input"
                type="date"
                className="form-input"
                value={draftFilters.endDate ?? ''}
                onChange={e => setDraftFilters(f => ({ ...f, endDate: e.target.value }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="guests-input">Guests</label>
              <input
                id="guests-input"
                type="number"
                min={1}
                max={20}
                className="form-input"
                placeholder="2"
                value={draftFilters.guests ?? ''}
                onChange={e => setDraftFilters(f => ({ ...f, guests: Number(e.target.value) }))}
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="type-select">Type</label>
              <select
                id="type-select"
                className="form-select"
                value={draftFilters.propertyType ?? ''}
                onChange={e => setDraftFilters(f => ({ ...f, propertyType: e.target.value || undefined }))}
              >
                <option value="">All types</option>
                <option value="APARTMENT">Apartment</option>
                <option value="HOUSE">House</option>
                <option value="VILLA">Villa</option>
                <option value="STUDIO">Studio</option>
              </select>
            </div>
            <button id="search-submit" type="submit" className="btn btn-primary search-btn">
              🔍 Search
            </button>
          </form>
        </div>
      </div>

      {/* ---- Results ---- */}
      <div className="container section">
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        {!loading && !error && (
          <p className="text-secondary text-sm" style={{ marginBottom: 20 }}>
            {totalElements.toLocaleString()} propert{totalElements === 1 ? 'y' : 'ies'} found
          </p>
        )}

        <div className="property-grid">
          {loading
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : properties.length > 0
              ? properties.map(p => <PropertyCard key={p.id} property={p} />)
              : !loading && (
                  <div style={{ gridColumn: '1/-1', textAlign: 'center', padding: '60px 0' }}>
                    <p className="text-2xl" style={{ marginBottom: 8 }}>🏡</p>
                    <p className="text-secondary">No properties found. Try adjusting your filters.</p>
                  </div>
                )
          }
        </div>

        {/* ---- Pagination ---- */}
        {totalPages > 1 && (
          <div className="pagination" style={{ marginTop: 40 }}>
            <button
              id="prev-page-btn"
              className="btn btn-outline"
              disabled={(filters.page ?? 0) === 0}
              onClick={() => setFilters(f => ({ ...f, page: (f.page ?? 0) - 1 }))}
            >
              ← Previous
            </button>
            <span className="text-secondary text-sm">
              Page {(filters.page ?? 0) + 1} of {totalPages}
            </span>
            <button
              id="next-page-btn"
              className="btn btn-outline"
              disabled={(filters.page ?? 0) >= totalPages - 1}
              onClick={() => setFilters(f => ({ ...f, page: (f.page ?? 0) + 1 }))}
            >
              Next →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
