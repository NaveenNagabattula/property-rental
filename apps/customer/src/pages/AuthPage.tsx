import React, { useState, useEffect } from 'react';
import { get, post, useNavigator } from '../services/apiClient';
import type { ApiResponse, AuthResponse, UserResponse } from '../types/api';

interface AuthPageProps {
  initialMode: 'login' | 'register' | 'forgot-password' | 'reset-password' | 'verify-email';
  params?: Record<string, string>;
}

export default function AuthPage({ initialMode, params = {} }: AuthPageProps) {
  const navigate = useNavigator();
  const [mode, setMode] = useState(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [role, setRole] = useState('GUEST');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Status states
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Sync mode if props change (e.g. going from /login to /verify-email via link clicks)
  useEffect(() => {
    setMode(initialMode);
    setError(null);
    setSuccess(null);
  }, [initialMode]);

  // Handle automatic email verification on mount if in verify-email mode
  useEffect(() => {
    if (mode === 'verify-email') {
      const verifyToken = params.token;
      if (!verifyToken) {
        setError('Verification token is missing in the URL.');
        return;
      }
      
      setLoading(true);
      setError(null);
      
      get<ApiResponse<void>>(`/api/v1/auth/verify-email?token=${verifyToken}`)
        .then(() => {
          setSuccess('Your email address has been successfully verified! You can now log in.');
        })
        .catch((err) => {
          setError(err.message || 'Failed to verify email. The link might be expired or invalid.');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [mode, params.token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (mode === 'login') {
        const res = await post<ApiResponse<AuthResponse>>('/api/v1/auth/login', {
          email,
          password,
        });
        localStorage.setItem('accessToken', res.data.accessToken);
        localStorage.setItem('refreshToken', res.data.refreshToken);
        setSuccess('Logged in successfully!');
        
        // Brief timeout to let the user see the success state, then redirect home
        setTimeout(() => {
          navigate('#/');
        }, 800);
      } 
      
      else if (mode === 'register') {
        await post<ApiResponse<UserResponse>>('/api/v1/auth/register', {
          email,
          password,
          firstName,
          lastName,
          role,
        });
        setSuccess('Registration successful! Please check your email to verify your account.');
        // Switch to login mode but keep the success notification
        setTimeout(() => {
          setMode('login');
          setPassword('');
          setSuccess('Registration successful! Please check your email to verify your account.');
        }, 1500);
      } 
      
      else if (mode === 'forgot-password') {
        await post<ApiResponse<void>>('/api/v1/auth/forgot-password', { email });
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
        await post<ApiResponse<void>>('/api/v1/auth/reset-password', {
          token: resetToken,
          newPassword,
        });
        setSuccess('Password reset successful. Redirecting to login...');
        setTimeout(() => {
          navigate('/login');
          setMode('login');
          setNewPassword('');
          setConfirmPassword('');
        }, 1500);
      }
    } catch (err: any) {
      setError(err.message || 'An error occurred during submission.');
    } finally {
      setLoading(false);
    }
  };

  const renderTitle = () => {
    switch (mode) {
      case 'login': return 'Welcome Back';
      case 'register': return 'Create an Account';
      case 'forgot-password': return 'Reset Your Password';
      case 'reset-password': return 'Set New Password';
      case 'verify-email': return 'Email Verification';
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 64px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px 24px',
      background: 'radial-gradient(circle at 50% 50%, var(--bg-card) 0%%, var(--bg-page) 100%%)',
    }}>
      <div className="card" style={{
        width: '100%',
        maxWidth: 480,
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
          <h2 className="text-2xl font-bold" style={{
            background: 'linear-gradient(135deg, var(--text-primary), var(--text-secondary))',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            marginBottom: 8
          }}>
            {renderTitle()}
          </h2>
          <p className="text-sm text-secondary">
            {mode === 'login' && 'Sign in to access your bookings and trips'}
            {mode === 'register' && 'Join StayFinder today'}
            {mode === 'forgot-password' && 'Enter your email to receive a password reset link'}
            {mode === 'reset-password' && 'Choose a strong password for your account'}
            {mode === 'verify-email' && 'Verifying your account details'}
          </p>
        </div>

        {/* Success/Error Alerts */}
        {error && <div className="alert alert-error" style={{ animation: 'fadeIn 0.2s' }}>{error}</div>}
        {success && <div className="alert alert-success" style={{ animation: 'fadeIn 0.2s' }}>{success}</div>}

        {/* Loading Spinner for Automatic Verification */}
        {mode === 'verify-email' && loading && (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, padding: '24px 0' }}>
            <div style={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              border: '4px solid var(--border-color)',
              borderTopColor: 'var(--brand-primary)',
              animation: 'spin 1s linear infinite'
            }} />
            <p className="text-sm text-secondary">Processing verification token...</p>
          </div>
        )}

        {/* Back to Login Button for Verification Complete / Expired */}
        {mode === 'verify-email' && !loading && (
          <a href="/login" className="btn btn-primary" style={{ width: '100%', textDecoration: 'none', textAlign: 'center' }}>
            Go to Sign In
          </a>
        )}

        {/* Main Auth Form */}
        {mode !== 'verify-email' && (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            
            {/* Registration specific fields */}
            {mode === 'register' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input
                    type="text"
                    required
                    className="form-input"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="Jane"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input
                    type="text"
                    required
                    className="form-input"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="Doe"
                  />
                </div>
              </div>
            )}

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
                  placeholder="jane.doe@example.com"
                />
              </div>
            )}

            {/* Password Field (only login/register) */}
            {(mode === 'login' || mode === 'register') && (
              <div className="form-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label className="form-label">Password</label>
                  {mode === 'login' && (
                    <a href="/forgot-password" style={{ fontSize: '0.8rem', color: 'var(--brand-primary)' }}>
                      Forgot?
                    </a>
                  )}
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

            {/* Role selection (only register) */}
            {mode === 'register' && (
              <div className="form-group">
                <label className="form-label">Register As</label>
                <select
                  className="form-select"
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                >
                  <option value="GUEST">Guest (Book properties)</option>
                  <option value="HOST">Host (List properties)</option>
                </select>
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
                mode === 'login' ? 'Sign In' :
                mode === 'register' ? 'Sign Up' :
                mode === 'forgot-password' ? 'Send Reset Link' : 'Reset Password'
              )}
            </button>
          </form>
        )}

        {/* Footer Navigation */}
        {mode !== 'verify-email' && (
          <div style={{
            textAlign: 'center',
            fontSize: '0.88rem',
            borderTop: '1px solid rgba(108, 99, 255, 0.12)',
            paddingTop: 20,
            marginTop: 4
          }}>
            {mode === 'login' && (
              <p className="text-secondary">
                Don't have an account?{' '}
                <a href="/register" style={{ fontWeight: 600 }}>Sign Up</a>
              </p>
            )}
            {mode === 'register' && (
              <p className="text-secondary">
                Already have an account?{' '}
                <a href="/login" style={{ fontWeight: 600 }}>Sign In</a>
              </p>
            )}
            {(mode === 'forgot-password' || mode === 'reset-password') && (
              <a href="/login" style={{ fontWeight: 600 }}>Back to Sign In</a>
            )}
          </div>
        )}

      </div>

      {/* Embedded styles for spinning & animations */}
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-4px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}
