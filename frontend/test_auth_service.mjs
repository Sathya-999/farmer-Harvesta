/**
 * test_auth_service.mjs
 * Validates authentication business logic, validation rules, and error handling.
 */

import { login, register, storeToken, getToken, clearToken, isAuthenticated } from './lib/authService.ts';

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

async function runTests() {
  console.log('========================================');
  console.log('Running Authentication Service Unit Tests');
  console.log('========================================');

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

  // Test 1: Empty Login Validation
  const loginEmpty = await login({ identifier: '', password: '' });
  assert('Empty login returns validation_error', !loginEmpty.ok && loginEmpty.code === 'validation_error');

  // Test 2: Missing Password Login Validation
  const loginNoPass = await login({ identifier: 'farmer@example.com', password: '' });
  assert('Missing password returns validation_error', !loginNoPass.ok && loginNoPass.code === 'validation_error');

  // Test 3: Empty Register Validation
  const regEmpty = await register({ firstName: '', lastName: '', email: '', password: '' });
  assert('Empty registration returns validation_error', !regEmpty.ok && regEmpty.code === 'validation_error');

  // Test 4: Short Password Register Validation
  const regShortPass = await register({ firstName: 'Ramesh', lastName: 'Kumar', email: 'ramesh@example.com', password: '123' });
  assert('Short password (< 8 chars) returns validation_error', !regShortPass.ok && regShortPass.code === 'validation_error');

  // Test 5: Token Storage & Session Helpers
  clearToken();
  assert('Initial session is unauthenticated', isAuthenticated() === false && getToken() === null);

  storeToken('test-jwt-token-xyz-123', { id: 'farmer-1', firstName: 'Ramesh', lastName: 'Kumar' });
  assert('Token stored successfully', getToken() === 'test-jwt-token-xyz-123');
  assert('isAuthenticated returns true after token stored', isAuthenticated() === true);

  clearToken();
  assert('clearToken removes session', isAuthenticated() === false && getToken() === null);

  // Test 6: Password Recovery Validation
  const { requestPasswordReset, resetPasswordWithToken, fetchFarmerProfile, storeFarmerProfile, getCachedFarmerProfile } = await import('./lib/authService.ts');

  const emptyReset = await requestPasswordReset('');
  assert('requestPasswordReset with empty email rejected', emptyReset.ok === false);

  const shortNewPass = await resetPasswordWithToken('short', 'valid-token-123');
  assert('resetPasswordWithToken with short password (<8 chars) rejected', shortNewPass.ok === false);

  const noTokenReset = await resetPasswordWithToken('ValidPassword123!', '');
  assert('resetPasswordWithToken with missing token rejected', noTokenReset.ok === false);

  // Test 7: Farmer Profile Cache
  storeFarmerProfile({
    id: 'farmer-uuid-1',
    fullName: 'Ramesh Patel',
    homeVillageId: 'village-uuid-1',
    onboardingCompleted: true,
    profileStatus: 'ACTIVE',
  });
  const cached = getCachedFarmerProfile();
  assert('storeFarmerProfile caches profile', cached !== null && cached.fullName === 'Ramesh Patel');

  // Test 8: Backend Unauthorized Rejection
  // Calling /api/v1/farmers/me with null/invalid token should be safely rejected
  const unauthProfile = await fetchFarmerProfile('invalid-mock-token');
  assert('fetchFarmerProfile with invalid token rejected by backend', unauthProfile.ok === false);

  console.log('========================================');
  console.log(`Tests Completed: ${passed} Passed, ${failed} Failed`);
  console.log('========================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests().catch(err => {
  console.error('Fatal test error:', err);
  process.exit(1);
});
