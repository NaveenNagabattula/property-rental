import React, { useState, useEffect } from 'react';
import { get, post, ApiError } from '../services/apiClient';
import type { ApiResponse, AuthResponse } from '../types/api';

interface AuthPageProps {
  initialMode: 'login' | 'forgot-password' | 'reset-password';
  params?: Record<string, string>;
}

export default function AuthPage({ initialMode, params = {} }: AuthPageProps) {
  const [mode, setMode] = useState(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Status states
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    setMode(initialMode);
    setError(null);
    setSuccess(null);
  }, [initialMode]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (mode === 'login') {
        const res = await post<ApiResponse<AuthResponse>>(
          '/api/v1/auth/login',
          { email, password },
          { skipAuth: true },
        );

        const token = res.data.accessToken;
        const refresh = res.data.refreshToken;

        // Temporarily set tokens to verify admin permissions by fetching dashboard KPI
        localStorage.setItem('admin_accessToken', token);
        localStorage.setItem('admin_refreshToken', refresh);

        try {
          // Verify role by calling a protected admin endpoint
          await get('/api/v1/admin/listings/pending?page=0&size=1');

          setSuccess('Access granted! Logging in...');
          // Force a full page reload to the dashboard — clears all React state
          // and guarantees App re-mounts with the token already in localStorage
          window.location.replace('/#/');
        } catch (err: any) {
          // If the admin check fails with 403/401, the user is not an admin/manager
          localStorage.removeItem('admin_accessToken');
          localStorage.removeItem('admin_refreshToken');
          throw new Error('Access denied. You do not have administrator or property manager permissions.');
        }
      } 
      
      else if (mode === 'forgot-password') {
        await post<ApiResponse<void>>('/api/v1/auth/forgot-password', { email }, { skipAuth: true });
        setSuccess('If that email exists in our system, a password reset link has been sent.');
      } 
      
      else if (mode === 'reset-password') {
        const resetToken = params.token;
        if (!resetToken) {
          throw new Error('Reset token is missing. Please use the link sent to your email.');
        }
        if (newPassword !== confirmPassword) {
          throw new Error('Passwords do not match.');
        }
        await post<ApiResponse<void>>(
          '/api/v1/auth/reset-password',
          { token: resetToken, newPassword },
          { skipAuth: true },
        );
        setSuccess('Password reset successful. Redirecting to login...');
        setTimeout(() => {
          window.location.href = window.location.origin + '/login';
          setMode('login');
          setNewPassword('');
          setConfirmPassword('');
        }, 1500);
      }
    } catch (err: unknown) {
      const message =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
            ? err.message
            : 'An error occurred during submission.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const renderTitle = () => {
    switch (mode) {
      case 'login': return 'Admin Portal';
      case 'forgot-password': return 'Reset Your Password';
      case 'reset-password': return 'Set New Password';
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px 24px',
      background: 'radial-gradient(circle at 50% 50%, var(--bg-card) 0%, var(--bg-page) 100%)',
    }}>
      <div className="card" style={{
        width: '100%',
        maxWidth: 440,
        padding: '40px 32px',
        display: 'flex',
        flexDirection: 'column',
        gap: 24,
        background: 'rgba(19, 22, 42, 0.8)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(108, 99, 255, 0.25)',
      }}>
        
        {/* Title */}
        <div style={{ textAlign: 'center' }}>
          <h2 className="text-xl font-bold" style={{
            background: 'linear-gradient(135deg, var(--text-primary), var(--text-secondary))',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            marginBottom: 6
          }}>
            {renderTitle()}
          </h2>
          <p className="text-sm text-secondary">
            {mode === 'login' && 'Sign in to access listings moderation and configuration'}
            {mode === 'forgot-password' && 'Enter your email to receive a password reset link'}
            {mode === 'reset-password' && 'Choose a strong password for your account'}
          </p>
        </div>

        {/* Success/Error Alerts */}
        {error && <div className="alert alert-error" style={{ animation: 'fadeIn 0.2s' }}>{error}</div>}
        {success && <div className="alert alert-success" style={{ animation: 'fadeIn 0.2s' }}>{success}</div>}

        {/* Main Auth Form */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          
          {/* Email Field (except for reset-password) */}
          {mode !== 'reset-password' && (
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                required
                className="form-input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@propertyrental.com"
              />
            </div>
          )}

          {/* Password Field (only login) */}
          {mode === 'login' && (
            <div className="form-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label className="form-label">Password</label>
                <a href="#/forgot-password" style={{ fontSize: '0.8rem', color: 'var(--brand-primary)' }}>
                  Forgot?
                </a>
              </div>
              <input
                type="password"
                required
                className="form-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>
          )}

          {/* Reset Password specific fields */}
          {mode === 'reset-password' && (
            <>
              <div className="form-group">
                <label className="form-label">New Password</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Confirm Password</label>
                <input
                  type="password"
                  required
                  className="form-input"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                />
              </div>
            </>
          )}

          {/* Submit Button */}
          <button type="submit" disabled={loading} className="btn btn-primary" style={{ width: '100%', marginTop: 8 }}>
            {loading ? 'Processing...' : (
              mode === 'login' ? '🔐 Sign In to Console' :
              mode === 'forgot-password' ? 'Send Reset Link' : 'Reset Password'
            )}
          </button>
        </form>

        {/* Footer Navigation */}
        <div style={{
          textAlign: 'center',
          fontSize: '0.88rem',
          borderTop: '1px solid rgba(108, 99, 255, 0.12)',
          paddingTop: 20,
          marginTop: 4
        }}>
          {(mode === 'forgot-password' || mode === 'reset-password') && (
            <a href="#/login" style={{ fontWeight: 600 }}>Back to Sign In</a>
          )}
        </div>

      </div>

      {/* Embedded styles for spinning & animations */}
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-4px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}
