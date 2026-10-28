/**
 * authService.ts — Harvest(a) Authentication & Identity Service
 *
 * Authentication Architecture:
 * - Spring Boot backend is the SINGLE authentication authority
 * - Issues HMAC-SHA256 signed JWTs for authenticated sessions
 * - Guarded local development simulation (enabled only when explicitly opted in)
 * - Safe internal redirect validation to prevent open-redirect vulnerabilities
 * - Bearer JWT session token persistence for API calls
 * - Clean logout and state teardown
 */

// ─── Types ────────────────────────────────────────────────────────────────────

export interface AuthUser {
  id: string
  firstName: string
  lastName: string
  email?: string
  phone?: string
  role?: string
  isSimulated?: boolean
}

export interface LoginRequest {
  identifier: string // email or phone
  password: string
  rememberMe?: boolean
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phone?: string
  termsAccepted?: boolean
}

export interface AuthSuccessResult {
  ok: true
  user: AuthUser
  token: string
}

export type AuthErrorCode =
  | 'invalid_credentials'
  | 'account_not_found'
  | 'account_exists'
  | 'validation_error'
  | 'server_error'
  | 'network_error'
  | 'not_available'

export interface AuthErrorResult {
  ok: false
  code: AuthErrorCode
  message: string
  fieldErrors?: Record<string, string>
}

export type AuthResult = AuthSuccessResult | AuthErrorResult

// ─── Safe Environment & Configuration Checks ──────────────────────────────────

/**
 * Checks if local development authentication simulation is permitted.
 * STRICT SECURITY INVARIANT:
 * - NEVER allowed when NODE_ENV === 'production'
 * - In non-production, requires explicit NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK === 'true'
 */
export function isDevAuthFallbackAllowed(): boolean {
  if (process.env.NODE_ENV === 'production') {
    return false
  }
  return process.env.NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK === 'true'
}

/**
 * getSafeRedirectUrl — Prevents open-redirect vulnerabilities.
 * Ensures the redirect URL is strictly an internal relative path.
 * Rejects external URLs, protocol-relative paths ('//'), backslashes, data URIs, javascript:, etc.
 */
export function getSafeRedirectUrl(target: string | null | undefined, fallback = '/dashboard'): string {
  if (!target || typeof target !== 'string') return fallback
  const trimmed = target.trim()

  // Must start with a single '/' and not with '//' or '/\'
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.startsWith('/\\')) {
    return fallback
  }

  // Reject protocol indicators, backslashes, null bytes, and script schemas
  if (
    trimmed.includes('://') ||
    trimmed.includes('\\') ||
    trimmed.includes('\0') ||
    trimmed.toLowerCase().includes('javascript:')
  ) {
    return fallback
  }

  return trimmed
}

// ─── Token & Session Management ───────────────────────────────────────────────

const TOKEN_KEY = 'sf_auth_token'
const USER_KEY = 'sf_auth_user'
const PROFILE_KEY = 'sf_farmer_profile'
export const ONBOARDING_KEY = 'sf_onboarding_completed'

export function isOnboardingCompleted(): boolean {
  try {
    if (typeof localStorage === 'undefined') return false
    return localStorage.getItem(ONBOARDING_KEY) === 'true'
  } catch {
    return false
  }
}

export function markOnboardingCompleted(): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(ONBOARDING_KEY, 'true')
    }
  } catch {
    // Storage unavailable
  }
}

export function resetOnboardingMarker(): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(ONBOARDING_KEY)
    }
  } catch {
    // Storage unavailable
  }
}

export function storeToken(token: string, user?: AuthUser): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(TOKEN_KEY, token)
      if (user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      }
    }
  } catch {
    // Storage unavailable
  }
}

export function getToken(): string | null {
  try {
    if (typeof localStorage === 'undefined') return null
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function getCurrentUser(): AuthUser | null {
  try {
    if (typeof localStorage === 'undefined') return null
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function clearToken(): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(PROFILE_KEY)
      // Note: ONBOARDING_KEY is preserved across logouts so returning
      // users are routed directly to login without repeating splash or registration.
    }
  } catch {
    // Best effort
  }
}

export function storeFarmerProfile(profile: any): void {
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
    }
  } catch {
    // Storage unavailable
  }
}

export function getCachedFarmerProfile(): any {
  try {
    if (typeof localStorage === 'undefined') return null
    const raw = localStorage.getItem(PROFILE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export async function fetchFarmerProfile(token?: string): Promise<{ ok: boolean; profile: any }> {
  try {
    const { api } = await import('./apiClient')
    const profile = await api.get('/api/v1/farmers/me', token ? { Authorization: `Bearer ${token}` } : undefined)
    return { ok: true, profile }
  } catch {
    return { ok: false, profile: null }
  }
}

/**
 * isTokenExpired — Decodes the JWT payload safely to check the 'exp' claim.
 * Returns true if the token is definitely expired.
 * For simulated dev tokens or stubs without a valid exp claim, returns false.
 */
export function isTokenExpired(token: string): boolean {
  if (!token) return true
  if (token.startsWith('simulated-')) {
    return false
  }
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return false
    }
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    let jsonStr = ''
    if (typeof atob !== 'undefined') {
      jsonStr = atob(base64)
    } else if (typeof Buffer !== 'undefined') {
      jsonStr = Buffer.from(base64, 'base64').toString('utf-8')
    }
    if (!jsonStr) return false
    const payload = JSON.parse(jsonStr)
    if (typeof payload.exp === 'number') {
      const nowInSeconds = Math.floor(Date.now() / 1000)
      return payload.exp <= nowInSeconds
    }
    return false
  } catch {
    return false
  }
}

export function isAuthenticated(): boolean {
  const token = getToken()
  if (!token) return false
  if (isTokenExpired(token)) {
    clearToken()
    return false
  }
  return true
}

// ─── Login Implementation ─────────────────────────────────────────────────────

export async function login(req: LoginRequest): Promise<AuthResult> {
  const idTrimmed = req.identifier.trim()
  if (!idTrimmed) {
    return { ok: false, code: 'validation_error', message: 'Please enter your email or mobile number.' }
  }
  if (!req.password) {
    return { ok: false, code: 'validation_error', message: 'Please enter your password.' }
  }

  // Spring Boot Backend Authentication
  try {
    const baseUrl = typeof window !== 'undefined' ? '' : (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080')
    const res = await fetch(`${baseUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        identifier: idTrimmed,
        password: req.password,
      }),
    })

    const json = await res.json().catch(() => null)
    if (res.ok && json?.success && json?.data?.token) {
      const u = json.data.user
      const user: AuthUser = {
        id: u?.id || 'farmer',
        firstName: u?.firstName || 'Farmer',
        lastName: u?.lastName || '',
        email: u?.email,
        phone: u?.phone,
        role: u?.role || 'FARMER',
      }
      storeToken(json.data.token, user)
      return { ok: true, user, token: json.data.token }
    }

    if (res.status === 401) {
      return {
        ok: false,
        code: 'invalid_credentials',
        message: json?.error || 'The email/phone or password you entered is incorrect. Please try again.',
      }
    }

    if (res.status === 400) {
      return {
        ok: false,
        code: 'validation_error',
        message: json?.error || 'Please enter valid login credentials.',
      }
    }

    return {
      ok: false,
      code: 'server_error',
      message: json?.error || 'Authentication service error. Please try again.',
    }
  } catch (err) {
    // Network error
    const isNetwork = err instanceof TypeError && err.message.toLowerCase().includes('fetch')
    if (isNetwork) {
      return {
        ok: false,
        code: 'network_error',
        message: 'Unable to connect to the authentication server. Please check your network connection.',
      }
    }
  }

  // Development Simulation (only if backend unavailable and dev mode enabled)
  if (isDevAuthFallbackAllowed()) {
    if (req.password.length >= 6) {
      const user: AuthUser = {
        id: 'dev-simulated-farmer',
        firstName: 'Ramesh',
        lastName: 'Patel',
        email: idTrimmed.includes('@') ? idTrimmed : 'farmer@agriplatform.in',
        phone: idTrimmed.includes('@') ? '+91 98765 43210' : idTrimmed,
        role: 'FARMER',
        isSimulated: true,
      }
      const devToken = 'simulated-dev-session-not-valid-for-backend'
      storeToken(devToken, user)
      console.warn('[DEV MODE] Using simulated authentication - backend not available')
      return { ok: true, user, token: devToken }
    }

    return {
      ok: false,
      code: 'invalid_credentials',
      message: 'Password must be at least 6 characters.',
    }
  }

  return {
    ok: false,
    code: 'not_available',
    message: 'Authentication service is currently unavailable. Please try again later.',
  }
}

// ─── Register Implementation ──────────────────────────────────────────────────

export async function register(req: RegisterRequest): Promise<AuthResult> {
  const fName = req.firstName.trim()
  const lName = req.lastName.trim()
  const email = req.email.trim()

  if (!fName || !lName) {
    return { ok: false, code: 'validation_error', message: 'Please provide both your first and last name.' }
  }
  if (!email) {
    return { ok: false, code: 'validation_error', message: 'Please enter your email address.' }
  }
  if (!req.password || req.password.length < 8) {
    return { ok: false, code: 'validation_error', message: 'Password must be at least 8 characters long.' }
  }

  // Spring Boot Backend Registration
  try {
    const baseUrl = typeof window !== 'undefined' ? '' : (process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080')
    const res = await fetch(`${baseUrl}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        firstName: fName,
        lastName: lName,
        email,
        password: req.password,
        phone: req.phone?.trim() || null,
      }),
    })

    const json = await res.json().catch(() => null)
    if (res.status === 201 && json?.success) {
      const u = json.data?.user
      const user: AuthUser = {
        id: u?.id || 'farmer',
        firstName: u?.firstName || fName,
        lastName: u?.lastName || lName,
        email: u?.email || email,
        phone: u?.phone,
        role: u?.role || 'FARMER',
      }
      // Store token if provided (some systems issue token on registration)
      if (json.data?.token) {
        storeToken(json.data.token, user)
      }
      return {
        ok: true,
        user,
        token: json.data?.token || '',
      }
    }

    if (res.status === 409) {
      return {
        ok: false,
        code: 'account_exists',
        message: json?.error || 'An account with this email address or phone number already exists. Please sign in instead.',
      }
    }

    if (res.status === 400) {
      return {
        ok: false,
        code: 'validation_error',
        message: json?.error || 'Please verify your registration details.',
      }
    }

    return {
      ok: false,
      code: 'server_error',
      message: json?.error || 'Unable to complete registration. Please try again.',
    }
  } catch (err) {
    // Network error
    const isNetwork = err instanceof TypeError && err.message.toLowerCase().includes('fetch')
    if (isNetwork) {
      return {
        ok: false,
        code: 'network_error',
        message: 'Unable to connect to the registration service. Please check your network connection.',
      }
    }
  }

  // Development Simulation (only if backend unavailable and dev mode enabled)
  if (isDevAuthFallbackAllowed()) {
    const user: AuthUser = {
      id: `dev-simulated-${Date.now()}`,
      firstName: fName,
      lastName: lName,
      email,
      phone: req.phone,
      role: 'FARMER',
      isSimulated: true,
    }
    const devToken = 'simulated-dev-session-not-valid-for-backend'
    storeToken(devToken, user)
    console.warn('[DEV MODE] Using simulated registration - backend not available')
    return { ok: true, user, token: devToken }
  }

  return {
    ok: false,
    code: 'not_available',
    message: 'Registration service is currently unavailable. Please try again later.',
  }
}

// ─── Password Reset (Future Implementation) ───────────────────────────────────

export async function requestPasswordReset(email: string): Promise<{ ok: boolean; message: string }> {
  const em = email.trim()
  if (!em) {
    return { ok: false, message: 'Please enter your email address to request a reset link.' }
  }

  // TODO: Implement password reset endpoint in Spring Boot backend
  return {
    ok: false,
    message: 'Password recovery is not yet implemented. Please contact support for assistance.',
  }
}

export async function resetPasswordWithToken(
  newPassword: string,
  recoveryToken: string
): Promise<{ ok: boolean; message: string }> {
  if (!newPassword || newPassword.length < 8) {
    return { ok: false, message: 'New password must be at least 8 characters long.' }
  }
  if (!recoveryToken) {
    return { ok: false, message: 'Invalid or missing recovery token. Please request a new password reset link.' }
  }

  // TODO: Implement password reset endpoint in Spring Boot backend
  return {
    ok: false,
    message: 'Password recovery is not yet implemented. Please contact support for assistance.',
  }
}

// ─── Logout Implementation ────────────────────────────────────────────────────

export async function logout(): Promise<void> {
  clearToken()
  // Backend logout endpoint is optional for stateless JWT systems
  // No server-side session to invalidate
}
