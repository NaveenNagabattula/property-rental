/**
 * apiClient.ts — Admin App HTTP Client
 *
 * A lightweight native Fetch wrapper that:
 * 1. Injects `Authorization: Bearer <token>` from localStorage on every request.
 * 2. Auto-refreshes the access token on HTTP 401 by calling POST /api/v1/auth/refresh.
 * 3. Retries the original request once with the new token.
 * 4. Clears auth state and redirects to /login if the refresh also fails.
 */

import { useCallback } from 'react';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

const TOKEN_KEY = 'admin_accessToken';
const REFRESH_TOKEN_KEY = 'admin_refreshToken';

// ---------------------------------------------------------------------------
// Token helpers
// ---------------------------------------------------------------------------

function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

// ---------------------------------------------------------------------------
// Internal: refresh the access token using the stored refresh token
// ---------------------------------------------------------------------------

let isRefreshing = false;
let pendingQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  pendingQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token!);
    }
  });
  pendingQueue = [];
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token available');

  const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    throw new Error('Token refresh failed');
  }

  const data = await res.json();
  const { accessToken, refreshToken: newRefreshToken } = data.data;
  setTokens(accessToken, newRefreshToken);
  return accessToken;
}

// ---------------------------------------------------------------------------
// Public: apiClient — drop-in replacement for fetch()
// ---------------------------------------------------------------------------

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface ApiClientOptions extends Omit<RequestInit, 'method' | 'body'> {
  method?: HttpMethod;
  body?: unknown;
  /** Skip auth header injection (e.g. login endpoint). */
  skipAuth?: boolean;
}

/**
 * Fetch wrapper with automatic JWT injection and silent token refresh on 401.
 *
 * @param endpoint  API path relative to VITE_API_BASE_URL (e.g. '/api/v1/admin/listings/pending')
 * @param options   Extended RequestInit options
 * @returns         Parsed JSON response body
 */
export async function apiClient<T = unknown>(
  endpoint: string,
  options: ApiClientOptions = {}
): Promise<T> {
  const { method = 'GET', body, skipAuth = false, headers: extraHeaders = {}, ...rest } = options;

  const isPublicAuth = endpoint.startsWith('/api/v1/auth/') && !endpoint.endsWith('/logout');
  const shouldSkipAuth = skipAuth || isPublicAuth;

  const buildHeaders = (token: string | null): HeadersInit => ({
    'Content-Type': 'application/json',
    ...(token && !shouldSkipAuth ? { Authorization: `Bearer ${token}` } : {}),
    ...(extraHeaders as Record<string, string>),
  });

  const doFetch = (token: string | null) =>
    fetch(`${API_BASE_URL}${endpoint}`, {
      method,
      headers: buildHeaders(token),
      body: body !== undefined ? JSON.stringify(body) : undefined,
      ...rest,
    });

  // First attempt
  let response = await doFetch(shouldSkipAuth ? null : getAccessToken());

  if (response.status !== 401 || shouldSkipAuth) {
    return parseResponse<T>(response);
  }

  // No refresh token — surface the original 401 error (e.g. invalid login credentials)
  if (!getRefreshToken()) {
    return parseResponse<T>(response);
  }

  // ------- 401 — attempt silent token refresh -------
  if (isRefreshing) {
    return new Promise<T>((resolve, reject) => {
      pendingQueue.push({
        resolve: async (newToken) => {
          try {
            const retryRes = await doFetch(newToken);
            resolve(await parseResponse<T>(retryRes));
          } catch (err) {
            reject(err);
          }
        },
        reject,
      });
    });
  }

  isRefreshing = true;

  try {
    const newToken = await refreshAccessToken();
    processQueue(null, newToken);
    response = await doFetch(newToken);
    return parseResponse<T>(response);
  } catch (err) {
    processQueue(err, null);
    clearTokens();
    throw err;
  } finally {
    isRefreshing = false;
  }
}

// ---------------------------------------------------------------------------
// Internal: parse response and throw on non-2xx
// ---------------------------------------------------------------------------

async function parseResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;

  const json = await res.json().catch(() => null);

  if (!res.ok) {
    const message =
      json?.message ?? json?.error ?? `HTTP ${res.status} ${res.statusText}`;
    throw new ApiError(res.status, message, json);
  }

  return json as T;
}

// ---------------------------------------------------------------------------
// ApiError — carries HTTP status for consumers to branch on
// ---------------------------------------------------------------------------

export class ApiError extends Error {
  public readonly status: number;
  public readonly body: unknown;

  constructor(status: number, message: string, body: unknown = null) {
    super(message);
    this.status = status;
    this.body = body;
    this.name = 'ApiError';
  }
}

// ---------------------------------------------------------------------------
// Convenience shorthands
// ---------------------------------------------------------------------------

export const get = <T>(endpoint: string, opts?: Omit<ApiClientOptions, 'method'>) =>
  apiClient<T>(endpoint, { ...opts, method: 'GET' });

export const post = <T>(endpoint: string, body: unknown, opts?: Omit<ApiClientOptions, 'method' | 'body'>) =>
  apiClient<T>(endpoint, { ...opts, method: 'POST', body });

export const put = <T>(endpoint: string, body: unknown, opts?: Omit<ApiClientOptions, 'method' | 'body'>) =>
  apiClient<T>(endpoint, { ...opts, method: 'PUT', body });

export const patch = <T>(endpoint: string, body: unknown, opts?: Omit<ApiClientOptions, 'method' | 'body'>) =>
  apiClient<T>(endpoint, { ...opts, method: 'PATCH', body });

export const del = <T>(endpoint: string, opts?: Omit<ApiClientOptions, 'method'>) =>
  apiClient<T>(endpoint, { ...opts, method: 'DELETE' });

// ---------------------------------------------------------------------------
// SPA Router Navigator
// ---------------------------------------------------------------------------

export function navigate(url: string) {
  if (url.startsWith('#')) {
    window.location.hash = url;
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  } else if (url === '/login' || url === '/forgot-password' || url === '/reset-password') {
    window.history.pushState(null, '', url);
    window.location.hash = '';
    window.dispatchEvent(new PopStateEvent('popstate'));
  } else {
    window.history.pushState(null, '', url);
    window.dispatchEvent(new PopStateEvent('popstate'));
  }
}

export function useNavigator() {
  return useCallback((url: string) => {
    navigate(url);
  }, []);
}
