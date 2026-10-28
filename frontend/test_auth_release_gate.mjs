/**
 * test_auth_release_gate.mjs
 * Security release gate test suite for Harvest(a) authentication architecture.
 */

// Simple polyfill for localStorage in node test environment
if (typeof globalThis.localStorage === 'undefined') {
  const store = new Map();
  globalThis.localStorage = {
    getItem: (k) => store.get(k) ?? null,
    setItem: (k, v) => store.set(k, String(v)),
    removeItem: (k) => store.delete(k),
    clear: () => store.clear(),
  };
}

let passed = 0;
let failed = 0;

function assert(desc, condition) {
  if (condition) {
    console.log(`  ✓ ${desc}`);
    passed++;
  } else {
    console.error(`  ✗ FAIL: ${desc}`);
    failed++;
  }
}

async function runSecurityGateTests() {
  console.log('====================================================');
  console.log('HARVEST(A) AUTHENTICATION RELEASE GATE TEST SUITE');
  console.log('====================================================');

  const {
    login,
    register,
    requestPasswordReset,
    storeToken,
    getToken,
    clearToken,
    isAuthenticated,
    getSafeRedirectUrl,
    isDevAuthFallbackAllowed,
  } = await import('./lib/authService.ts');

  // ─── GATE 1: OPEN REDIRECT VULNERABILITY PREVENTION ──────────────────────────
  console.log('\n[GATE 1] Open Redirect & URL Parameter Sanitization:');
  
  assert('Internal dashboard route preserved', getSafeRedirectUrl('/dashboard') === '/dashboard');
  assert('Nested internal route preserved', getSafeRedirectUrl('/cultivations/new?step=2') === '/cultivations/new?step=2');
  assert('Protocol-relative URL //evil.com blocked', getSafeRedirectUrl('//evil.com') === '/dashboard');
  assert('Protocol-relative URL with path //evil.com/phish blocked', getSafeRedirectUrl('//evil.com/phish') === '/dashboard');
  assert('Backslash evasion /\\evil.com blocked', getSafeRedirectUrl('/\\evil.com') === '/dashboard');
  assert('External https:// URL blocked', getSafeRedirectUrl('https://malicious-site.com') === '/dashboard');
  assert('External http:// URL blocked', getSafeRedirectUrl('http://phishing.in/harvest') === '/dashboard');
  assert('javascript: scheme injection blocked', getSafeRedirectUrl('javascript:alert(1)') === '/dashboard');
  assert('Null input returns safe fallback', getSafeRedirectUrl(null) === '/dashboard');
  assert('Undefined input returns safe fallback', getSafeRedirectUrl(undefined) === '/dashboard');
  assert('Custom fallback route respected', getSafeRedirectUrl('//evil.com', '/plots') === '/plots');

  // ─── GATE 2: PRODUCTION FAIL-SAFE WHEN KEYS ARE MISSING ──────────────────────
  console.log('\n[GATE 2] Production Fail-Safe Security:');

  // Simulate production environment
  const originalNodeEnv = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';

  assert('isDevAuthFallbackAllowed strictly returns false in production', isDevAuthFallbackAllowed() === false);

  const prodLoginAttempt = await login({ identifier: 'farmer@test.com', password: 'password123' });
  assert('Production login without anon key FAILS SAFELY (not_available)', !prodLoginAttempt.ok && prodLoginAttempt.code === 'not_available');

  const prodRegAttempt = await register({ firstName: 'Ramesh', lastName: 'Patel', email: 'farmer@test.com', password: 'password123' });
  assert('Production register without anon key FAILS SAFELY (not_available)', !prodRegAttempt.ok && prodRegAttempt.code === 'not_available');

  const prodResetAttempt = await requestPasswordReset('farmer@test.com');
  assert('Production password reset without anon key fails safely', prodResetAttempt.ok === false);

  // Restore environment
  process.env.NODE_ENV = originalNodeEnv;

  // ─── GATE 3: INPUT VALIDATION & CREDENTIAL HYGIENE ──────────────────────────
  console.log('\n[GATE 3] Input Validation & Credential Guards:');

  const emptyLogin = await login({ identifier: '   ', password: '' });
  assert('Empty identifier rejected with validation_error', !emptyLogin.ok && emptyLogin.code === 'validation_error');

  const emptyPassLogin = await login({ identifier: 'farmer@agri.in', password: '' });
  assert('Missing password rejected with validation_error', !emptyPassLogin.ok && emptyPassLogin.code === 'validation_error');

  const emptyReg = await register({ firstName: '', lastName: '', email: '', password: '' });
  assert('Missing names rejected with validation_error', !emptyReg.ok && emptyReg.code === 'validation_error');

  const shortPassReg = await register({ firstName: 'Ramesh', lastName: 'Patel', email: 'ramesh@agri.in', password: '123' });
  assert('Password under 8 chars rejected with validation_error', !shortPassReg.ok && shortPassReg.code === 'validation_error');

  // ─── GATE 4: SESSION STORAGE & TEARDOWN INTEGRITY ───────────────────────────
  console.log('\n[GATE 4] Session Lifecycle & Bearer Token Management:');

  clearToken();
  assert('Initial state has no token', getToken() === null && isAuthenticated() === false);

  const sampleToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sample';
  storeToken(sampleToken, { id: 'farmer-101', firstName: 'Ramesh', lastName: 'Patel', email: 'ramesh@agri.in', role: 'FARMER' });

  assert('Token retrieved matches stored value', getToken() === sampleToken);
  assert('isAuthenticated returns true when token is present', isAuthenticated() === true);

  clearToken();
  assert('clearToken removes token and session state', getToken() === null && isAuthenticated() === false);

  // ─── GATE 5: DEVELOPMENT SIMULATION GUARD ────────────────────────────────────
  console.log('\n[GATE 5] Development Fallback Isolation Guard:');

  process.env.NODE_ENV = 'development';
  process.env.NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK = 'true';

  assert('Dev fallback allowed only when explicit flag is true', isDevAuthFallbackAllowed() === true);

  const devLogin = await login({ identifier: 'farmer@agri.in', password: 'password123' });
  assert('Dev fallback flags user as isSimulated = true', devLogin.ok === true && devLogin.user.isSimulated === true);
  assert('Dev fallback token is explicitly marked not valid for backend', devLogin.ok === true && devLogin.token.includes('not-valid-for-backend'));

  // Disable flag and test rejection
  process.env.NEXT_PUBLIC_ENABLE_DEV_AUTH_FALLBACK = 'false';
  assert('isDevAuthFallbackAllowed returns false when flag is false', isDevAuthFallbackAllowed() === false);

  const devLoginDisabled = await login({ identifier: 'farmer@agri.in', password: 'password123' });
  assert('Dev fallback rejected when flag is false', !devLoginDisabled.ok && devLoginDisabled.code === 'not_available');

  clearToken();

  console.log('\n====================================================');
  console.log(`RELEASE GATE RESULTS: ${passed} Passed, ${failed} Failed`);
  console.log('====================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runSecurityGateTests().catch((err) => {
  console.error('Test execution error:', err);
  process.exit(1);
});
