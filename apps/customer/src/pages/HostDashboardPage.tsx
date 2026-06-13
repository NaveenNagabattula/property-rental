import { useState, useEffect, useCallback } from 'react';
import { get, post } from '../services/apiClient';
import type { ApiResponse, PageResponse, PropertySummary } from '../types/api';

// ─── Types ────────────────────────────────────────────────────────────────────
interface CreatePropertyForm {
  title: string;
  description: string;
  address: string;
  latitude: string;
  longitude: string;
  pricePerNight: string;
  guestCapacity: string;
  bedroomCount: string;
  bathroomCount: string;
  propertyType: string;
  amenities: string;
  photoUrls: string;
}

const PROPERTY_TYPES = ['APARTMENT', 'HOUSE', 'VILLA', 'STUDIO', 'CABIN', 'COTTAGE', 'LOFT', 'OTHER'];
const AMENITY_OPTIONS = [
  'WiFi', 'Air Conditioning', 'Heating', 'Kitchen', 'Parking', 'Pool',
  'Gym', 'Washer', 'Dryer', 'TV', 'Balcony', 'Pet Friendly',
  'Smoke Free', 'Beach Access', 'Mountain View', 'Garden',
];

// ─── Status Badge ─────────────────────────────────────────────────────────────
function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; bg: string; color: string }> = {
    DRAFT:          { label: 'Draft',          bg: 'rgba(156,163,185,0.15)', color: '#9ba3c9' },
    PENDING_REVIEW: { label: 'Pending Review', bg: 'rgba(251,191,36,0.15)',  color: '#fbbf24' },
    ACTIVE:         { label: 'Active',         bg: 'rgba(67,217,140,0.15)',  color: '#43D98C' },
    REJECTED:       { label: 'Rejected',       bg: 'rgba(239,68,68,0.15)',   color: '#ef4444' },
    SUSPENDED:      { label: 'Suspended',      bg: 'rgba(239,68,68,0.12)',   color: '#ef4444' },
  };
  const s = map[status] ?? map['DRAFT'];
  return (
    <span style={{
      background: s.bg, color: s.color,
      padding: '3px 10px', borderRadius: 20,
      fontSize: '0.76rem', fontWeight: 700, letterSpacing: '0.04em',
    }}>{s.label}</span>
  );
}

// ─── Create Listing Modal ─────────────────────────────────────────────────────
function CreateListingModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const blank: CreatePropertyForm = {
    title: '', description: '', address: '',
    latitude: '', longitude: '',
    pricePerNight: '', guestCapacity: '', bedroomCount: '', bathroomCount: '',
    propertyType: 'APARTMENT', amenities: '', photoUrls: '',
  };
  const [form, setForm] = useState<CreatePropertyForm>(blank);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState<1 | 2>(1);

  const set = (field: keyof CreatePropertyForm, val: string) =>
    setForm(f => ({ ...f, [field]: val }));

  const toggleAmenity = (a: string) =>
    setSelectedAmenities(prev =>
      prev.includes(a) ? prev.filter(x => x !== a) : [...prev, a]
    );

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    try {
      const photoList = form.photoUrls
        .split('\n')
        .map(u => u.trim())
        .filter(Boolean);

      await post<ApiResponse<unknown>>('/api/v1/properties', {
        title: form.title.trim(),
        description: form.description.trim(),
        address: form.address.trim(),
        latitude: parseFloat(form.latitude) || 0,
        longitude: parseFloat(form.longitude) || 0,
        pricePerNight: parseFloat(form.pricePerNight),
        guestCapacity: parseInt(form.guestCapacity),
        bedroomCount: parseInt(form.bedroomCount),
        bathroomCount: parseInt(form.bathroomCount),
        propertyType: form.propertyType,
        amenities: selectedAmenities,
        photoUrls: photoList,
      });
      onCreated();
    } catch (e: any) {
      setError(e.message || 'Failed to create listing.');
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.75)', backdropFilter: 'blur(8px)',
      display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
      padding: '32px 24px', overflowY: 'auto',
    }} onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{
        background: 'var(--bg-card)', border: '1px solid rgba(108,99,255,0.3)',
        borderRadius: 'var(--radius-lg)', width: '100%', maxWidth: 640,
        boxShadow: '0 32px 80px rgba(0,0,0,0.6)',
      }}>
        {/* Header */}
        <div style={{
          padding: '28px 32px 20px',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <div>
            <h2 style={{ fontWeight: 800 }}>🏠 Create New Listing</h2>
            <p className="text-secondary" style={{ fontSize: '0.88rem', marginTop: 4 }}>
              Step {step} of 2 — {step === 1 ? 'Basic Details' : 'Photos & Amenities'}
            </p>
          </div>
          {/* Step indicator */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            {[1, 2].map(s => (
              <div key={s} style={{
                width: 28, height: 28, borderRadius: '50%',
                background: step === s ? 'var(--brand-primary)' : step > s ? 'var(--brand-accent)' : 'var(--bg-surface)',
                color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '0.78rem', fontWeight: 700,
              }}>{step > s ? '✓' : s}</div>
            ))}
          </div>
        </div>

        {/* Step 1 */}
        {step === 1 && (
          <div style={{ padding: '28px 32px', display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div className="form-group">
              <label className="form-label">Listing Title *</label>
              <input className="form-input" placeholder="e.g. Cozy Beach Villa with Ocean View"
                value={form.title} onChange={e => set('title', e.target.value)} />
            </div>

            <div className="form-group">
              <label className="form-label">Description *</label>
              <textarea className="form-input" rows={4} placeholder="Describe your property, nearby attractions, house rules..."
                value={form.description} onChange={e => set('description', e.target.value)}
                style={{ resize: 'vertical', fontFamily: 'inherit' }} />
            </div>

            <div className="form-group">
              <label className="form-label">Full Address *</label>
              <input className="form-input" placeholder="e.g. 12, Beachfront Road, Goa - 403001"
                value={form.address} onChange={e => set('address', e.target.value)} />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="form-group">
                <label className="form-label">Latitude</label>
                <input className="form-input" type="number" step="any" placeholder="15.2993"
                  value={form.latitude} onChange={e => set('latitude', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Longitude</label>
                <input className="form-input" type="number" step="any" placeholder="74.1240"
                  value={form.longitude} onChange={e => set('longitude', e.target.value)} />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="form-group">
                <label className="form-label">Price Per Night (₹) *</label>
                <input className="form-input" type="number" min="1" placeholder="3000"
                  value={form.pricePerNight} onChange={e => set('pricePerNight', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Property Type *</label>
                <select className="form-input" value={form.propertyType}
                  onChange={e => set('propertyType', e.target.value)}>
                  {PROPERTY_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
              <div className="form-group">
                <label className="form-label">Guests *</label>
                <input className="form-input" type="number" min="1" placeholder="4"
                  value={form.guestCapacity} onChange={e => set('guestCapacity', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Bedrooms *</label>
                <input className="form-input" type="number" min="0" placeholder="2"
                  value={form.bedroomCount} onChange={e => set('bedroomCount', e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Bathrooms *</label>
                <input className="form-input" type="number" min="0" placeholder="1"
                  value={form.bathroomCount} onChange={e => set('bathroomCount', e.target.value)} />
              </div>
            </div>
          </div>
        )}

        {/* Step 2 */}
        {step === 2 && (
          <div style={{ padding: '28px 32px', display: 'flex', flexDirection: 'column', gap: 24 }}>
            <div className="form-group">
              <label className="form-label">Amenities</label>
              <div style={{
                display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 4,
              }}>
                {AMENITY_OPTIONS.map(a => {
                  const selected = selectedAmenities.includes(a);
                  return (
                    <button key={a} type="button"
                      onClick={() => toggleAmenity(a)}
                      style={{
                        padding: '6px 14px', borderRadius: 20, cursor: 'pointer',
                        fontSize: '0.82rem', fontWeight: 600, border: '1.5px solid',
                        transition: 'all 0.15s',
                        background: selected ? 'rgba(108,99,255,0.2)' : 'transparent',
                        borderColor: selected ? 'var(--brand-primary)' : 'var(--border-color)',
                        color: selected ? 'var(--brand-primary)' : 'var(--text-secondary)',
                      }}
                    >
                      {selected ? '✓ ' : ''}{a}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Photo URLs</label>
              <p className="text-secondary" style={{ fontSize: '0.8rem', marginBottom: 6 }}>
                One URL per line (max 20). Use public image links.
              </p>
              <textarea className="form-input" rows={5}
                placeholder={"https://images.unsplash.com/photo-xxx\nhttps://images.unsplash.com/photo-yyy"}
                value={form.photoUrls} onChange={e => set('photoUrls', e.target.value)}
                style={{ resize: 'vertical', fontFamily: 'monospace', fontSize: '0.82rem' }} />
            </div>

            {error && <div className="alert alert-error">{error}</div>}
          </div>
        )}

        {/* Footer */}
        <div style={{
          padding: '20px 32px 28px',
          borderTop: '1px solid var(--border-color)',
          display: 'flex', justifyContent: 'space-between', gap: 12,
        }}>
          <button className="btn btn-outline" onClick={step === 1 ? onClose : () => setStep(1)}>
            {step === 1 ? 'Cancel' : '← Back'}
          </button>
          {step === 1 ? (
            <button className="btn btn-primary"
              disabled={!form.title || !form.description || !form.address || !form.pricePerNight || !form.guestCapacity || !form.bedroomCount || !form.bathroomCount}
              onClick={() => setStep(2)}>
              Next: Photos & Amenities →
            </button>
          ) : (
            <button className="btn btn-primary" disabled={loading} onClick={handleSubmit}>
              {loading ? 'Creating...' : '🚀 Create Listing (Save as Draft)'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Submit for Review Confirm ────────────────────────────────────────────────
function SubmitConfirmModal({ id, title, onCancel, onConfirmed }: {
  id: string; title: string; onCancel: () => void; onConfirmed: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const handleSubmit = async () => {
    setLoading(true);
    try {
      await post<ApiResponse<unknown>>(`/api/v1/properties/${id}/submit`, {});
      onConfirmed();
    } catch (e: any) {
      setError(e.message || 'Failed to submit.');
      setLoading(false);
    }
  };
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24,
    }} onClick={e => e.target === e.currentTarget && onCancel()}>
      <div style={{
        background: 'var(--bg-card)', border: '1px solid rgba(108,99,255,0.3)',
        borderRadius: 'var(--radius-lg)', padding: '36px 32px', maxWidth: 420, width: '100%',
        boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
      }}>
        <div style={{ fontSize: 48, textAlign: 'center', marginBottom: 16 }}>📤</div>
        <h2 style={{ textAlign: 'center', marginBottom: 8 }}>Submit for Review?</h2>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 8, lineHeight: 1.6 }}>
          You're about to submit <strong style={{ color: 'var(--text-primary)' }}>"{title}"</strong> for admin review.
        </p>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', fontSize: '0.88rem', marginBottom: 24, lineHeight: 1.6 }}>
          Once submitted, you cannot edit the listing until the admin completes their review.
        </p>
        {error && <div className="alert alert-error" style={{ marginBottom: 16 }}>{error}</div>}
        <div style={{ display: 'flex', gap: 12 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onCancel}>Cancel</button>
          <button className="btn btn-primary" style={{ flex: 1 }} disabled={loading} onClick={handleSubmit}>
            {loading ? 'Submitting...' : '✅ Submit'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function HostDashboardPage() {
  const [listings, setListings] = useState<PropertySummary[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [submitTarget, setSubmitTarget] = useState<PropertySummary | null>(null);
  const [toast, setToast] = useState<{ msg: string; type: 'success' | 'error' } | null>(null);

  const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchListings = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const res = await get<ApiResponse<PageResponse<PropertySummary>>>(
        `/api/v1/properties/host/my-listings?page=${p}&size=10`
      );
      setListings(res.data.content);
      setTotalElements(res.data.totalElements);
      setTotalPages(res.data.totalPages);
    } catch (e: any) {
      showToast(e.message || 'Failed to load listings', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchListings(page); }, [page, fetchListings]);

  const handleCreated = () => {
    setShowCreate(false);
    showToast('🎉 Listing created as Draft! Review it and submit for admin approval.');
    fetchListings(0);
    setPage(0);
  };

  const handleSubmitted = () => {
    setSubmitTarget(null);
    showToast('📤 Listing submitted for admin review!');
    fetchListings(page);
  };

  return (
    <div className="page">
      <div className="container section">

        {/* Toast */}
        {toast && (
          <div style={{
            position: 'fixed', top: 80, right: 24, zIndex: 2000,
            background: toast.type === 'success' ? 'rgba(67,217,140,0.15)' : 'rgba(239,68,68,0.15)',
            border: `1px solid ${toast.type === 'success' ? '#43D98C' : '#ef4444'}`,
            color: toast.type === 'success' ? '#43D98C' : '#ef4444',
            padding: '14px 20px', borderRadius: 10,
            fontWeight: 600, fontSize: '0.9rem',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
            animation: 'fadeIn 0.2s ease',
            maxWidth: 380,
          }}>
            {toast.msg}
          </div>
        )}

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 32, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <a href="#/" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: 8, textDecoration: 'none' }}>
              ← Back to Home
            </a>
            <h1 style={{ fontSize: '1.8rem', fontWeight: 800 }}>🏠 My Listings</h1>
            <p className="text-secondary" style={{ marginTop: 4 }}>
              Manage your properties. Create drafts, then submit for admin approval to go live.
            </p>
          </div>
          <button id="create-listing-btn" className="btn btn-primary" onClick={() => setShowCreate(true)}>
            + New Listing
          </button>
        </div>

        {/* Listing status guide */}
        <div style={{
          display: 'flex', gap: 12, marginBottom: 28, flexWrap: 'wrap',
        }}>
          {[
            { s: 'DRAFT', desc: 'Saved, not submitted' },
            { s: 'PENDING_REVIEW', desc: 'Awaiting admin' },
            { s: 'ACTIVE', desc: 'Live & bookable' },
            { s: 'REJECTED', desc: 'Edit & resubmit' },
          ].map(({ s, desc }) => (
            <div key={s} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <StatusBadge status={s} />
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{desc}</span>
            </div>
          ))}
        </div>

        {/* Listings Table */}
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {loading ? (
            <div style={{ padding: 32, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[1, 2, 3].map(i => <div key={i} className="skeleton" style={{ height: 60, borderRadius: 8 }} />)}
            </div>
          ) : listings.length === 0 ? (
            <div style={{ padding: '64px 32px', textAlign: 'center' }}>
              <div style={{ fontSize: 56, marginBottom: 16 }}>🏡</div>
              <h3 style={{ marginBottom: 8 }}>No listings yet</h3>
              <p className="text-secondary" style={{ marginBottom: 24 }}>
                Create your first property listing to get started.
              </p>
              <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
                + Create First Listing
              </button>
            </div>
          ) : (
            <>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
                <thead>
                  <tr style={{ background: 'rgba(108,99,255,0.06)', borderBottom: '1px solid var(--border-color)' }}>
                    {['Property', 'Type', 'Price / Night', 'Guests', 'Status', 'Actions'].map(h => (
                      <th key={h} style={{
                        textAlign: 'left', padding: '14px 20px',
                        color: 'var(--text-secondary)', fontWeight: 600,
                        fontSize: '0.78rem', letterSpacing: '0.05em', textTransform: 'uppercase',
                      }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {listings.map((l, idx) => (
                    <tr key={l.id}
                      style={{
                        borderBottom: idx < listings.length - 1 ? '1px solid var(--border-color)' : 'none',
                        transition: 'background 0.15s',
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'rgba(108,99,255,0.04)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ fontWeight: 600, marginBottom: 2 }}>{l.title}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{l.city}</div>
                      </td>
                      <td style={{ padding: '14px 20px', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        {l.propertyType}
                      </td>
                      <td style={{ padding: '14px 20px', fontWeight: 600, color: 'var(--brand-primary)' }}>
                        ₹{l.pricePerNight?.toLocaleString('en-IN')}
                      </td>
                      <td style={{ padding: '14px 20px', color: 'var(--text-secondary)' }}>
                        {l.guestCapacity}
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <StatusBadge status={l.status} />
                      </td>
                      <td style={{ padding: '14px 20px' }}>
                        <div style={{ display: 'flex', gap: 8 }}>
                          {/* View on customer portal */}
                          {l.status === 'ACTIVE' && (
                            <a
                              id={`view-listing-${l.id}`}
                              href={`#/listing/${l.id}`}
                              className="btn btn-outline"
                              style={{ fontSize: '0.76rem', padding: '5px 12px' }}
                            >
                              👁 View
                            </a>
                          )}
                          {/* Submit for review (only DRAFT) */}
                          {l.status === 'DRAFT' && (
                            <button
                              id={`submit-listing-${l.id}`}
                              className="btn btn-primary"
                              style={{ fontSize: '0.76rem', padding: '5px 12px' }}
                              onClick={() => setSubmitTarget(l)}
                            >
                              📤 Submit
                            </button>
                          )}
                          {l.status === 'PENDING_REVIEW' && (
                            <span style={{ color: 'var(--text-secondary)', fontSize: '0.82rem', fontStyle: 'italic', alignSelf: 'center' }}>
                              Under review
                            </span>
                          )}
                          {l.status === 'REJECTED' && (
                            <span style={{ color: '#ef4444', fontSize: '0.82rem', alignSelf: 'center' }}>
                              Edit & resubmit
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div style={{
                  display: 'flex', justifyContent: 'center', alignItems: 'center',
                  gap: 8, padding: '16px 20px', borderTop: '1px solid var(--border-color)',
                }}>
                  <button className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 14px' }}
                    disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
                  <span style={{ color: 'var(--text-secondary)', fontSize: '0.88rem' }}>
                    Page {page + 1} of {totalPages} · {totalElements} total
                  </span>
                  <button className="btn btn-outline" style={{ fontSize: '0.82rem', padding: '6px 14px' }}
                    disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Modals */}
      {showCreate && <CreateListingModal onClose={() => setShowCreate(false)} onCreated={handleCreated} />}
      {submitTarget && (
        <SubmitConfirmModal
          id={submitTarget.id}
          title={submitTarget.title}
          onCancel={() => setSubmitTarget(null)}
          onConfirmed={handleSubmitted}
        />
      )}

      <style>{`
        @keyframes fadeIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
    </div>
  );
}
