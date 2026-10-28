/**
 * test_form_api.mjs
 * Test login & registration API error handling against running endpoints.
 */

import { login, register } from './lib/authService.ts';

async function testApiHandling() {
  console.log('========================================');
  console.log('Testing Authentication Error & Network Handling');
  console.log('========================================');

  // Test: Submitting credentials to the server
  // When hitting localhost:8080 or Supabase with invalid credentials
  const loginRes = await login({
    identifier: 'invalid.farmer@test.org',
    password: 'WrongPassword123!',
  });

  console.log('Login result with non-existent user:');
  console.log('  ok:', loginRes.ok);
  console.log('  code:', loginRes.code);
  console.log('  message:', loginRes.message);

  if (loginRes.ok === false) {
    console.log('  ✓ Correctly rejected invalid login');
    if (loginRes.message && !loginRes.message.includes('Exception') && !loginRes.message.includes('at ')) {
      console.log('  ✓ Safe user message (no stack trace or internal leak)');
    } else {
      console.error('  ✗ Error message exposed stack trace or internal details!');
      process.exit(1);
    }
  } else {
    console.error('  ✗ Security flaw: Fake login succeeded on invalid credentials!');
    process.exit(1);
  }

  // Test: Submitting registration with invalid credentials to server
  const regRes = await register({
    firstName: 'Test',
    lastName: 'Farmer',
    email: 'invalid-test-farmer@example.com',
    password: 'ValidPassword123!',
  });

  console.log('\nRegistration result:');
  console.log('  ok:', regRes.ok);
  if (!regRes.ok) {
    console.log('  code:', regRes.code);
    console.log('  message:', regRes.message);
    console.log('  ✓ Handled registration server response safely without crash');
  }

  console.log('\n========================================');
  console.log('✓ All Error & Network Handling Tests Passed!');
  console.log('========================================');
}

testApiHandling().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
