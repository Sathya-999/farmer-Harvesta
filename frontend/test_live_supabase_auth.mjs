/**
 * test_live_supabase_auth.mjs
 * Controlled live Supabase Auth test script for sign-up, login, recovery, and token handling.
 */

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://ienrhuqcbdkdexjwqkul.supabase.co';
const ANON_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY || '';

console.log('====================================================');
console.log('Controlled Live Supabase Auth Test Suite');
console.log('Target URL:', SUPABASE_URL);
console.log('Publishable Key Present:', ANON_KEY ? `Yes (length: ${ANON_KEY.length})` : 'NO (Empty/Blank)');
console.log('====================================================');

async function callAuth(endpoint, body, customHeaders = {}) {
  const url = `${SUPABASE_URL}/auth/v1${endpoint}`;
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'apikey': ANON_KEY,
        'Authorization': `Bearer ${ANON_KEY}`,
        ...customHeaders,
      },
      body: JSON.stringify(body),
    });
    const json = await res.json().catch(() => null);
    return { status: res.status, data: json };
  } catch (err) {
    return { status: 0, error: err.message };
  }
}

async function runLiveTests() {
  const testEmail = `qa.test.farmer.${Date.now()}@agriplatform.test`;
  const testPassword = 'SecureFarmerTestPassword123!';

  console.log('\n--- Test 1: Health & API Gateway Validation ---');
  const healthRes = await fetch(`${SUPABASE_URL}/auth/v1/health`, {
    headers: ANON_KEY ? { 'apikey': ANON_KEY } : {},
  });
  const healthJson = await healthRes.json().catch(() => null);
  console.log('HTTP Status:', healthRes.status);
  console.log('Gateway Response:', JSON.stringify(healthJson));

  console.log('\n--- Test 2: Invalid Credential Rejection ---');
  const invalidLoginRes = await callAuth('/token?grant_type=password', {
    email: 'nonexistent.farmer.999@agriplatform.test',
    password: 'WrongPassword999!',
  });
  console.log('HTTP Status:', invalidLoginRes.status);
  console.log('Response:', JSON.stringify(invalidLoginRes.data));

  console.log('\n--- Test 3: Controlled Sign-up Attempt ---');
  const signupRes = await callAuth('/signup', {
    email: testEmail,
    password: testPassword,
    data: {
      first_name: 'Test',
      last_name: 'Farmer',
    },
  });
  console.log('HTTP Status:', signupRes.status);
  console.log('Response:', JSON.stringify(signupRes.data));

  console.log('\n--- Test 4: Password Recovery Request ---');
  const recoverRes = await callAuth('/recover', {
    email: testEmail,
  });
  console.log('HTTP Status:', recoverRes.status);
  console.log('Response:', JSON.stringify(recoverRes.data));

  console.log('\n--- Test 5: Reset Token Handling (Unauthenticated/Malformed Token) ---');
  const resetRes = await callAuth(
    '/user',
    { password: 'NewSecurePassword456!' },
    { 'Authorization': 'Bearer malformed_expired_dummy_token_12345' }
  );
  console.log('HTTP Status:', resetRes.status);
  console.log('Response:', JSON.stringify(resetRes.data));

  console.log('\n====================================================');
  console.log('Live Supabase Auth Test Execution Complete');
  console.log('====================================================');
}

runLiveTests().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
