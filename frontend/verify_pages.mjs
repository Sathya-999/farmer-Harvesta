/**
 * verify_pages.mjs
 * End-to-end verification script for Splash, Login, and Register screens.
 */

const BASE_URL = 'http://localhost:3000';

async function verifyPage(name, path, requiredStrings) {
  console.log(`\n========================================`);
  console.log(`Testing [${name}] at ${BASE_URL}${path}`);
  console.log(`========================================`);

  const res = await fetch(`${BASE_URL}${path}`);
  console.log(`Status: ${res.status} ${res.statusText}`);
  if (res.status !== 200) {
    throw new Error(`Expected 200 OK but got ${res.status}`);
  }

  const html = await res.text();
  console.log(`Payload size: ${html.length} bytes`);

  let allFound = true;
  for (const s of requiredStrings) {
    const found = html.includes(s);
    if (found) {
      console.log(`  ✓ Found expected element/text: "${s}"`);
    } else {
      console.error(`  ✗ MISSING expected element/text: "${s}"`);
      allFound = false;
    }
  }

  if (!allFound) {
    throw new Error(`Verification failed for ${name}: missing required elements.`);
  }

  console.log(`✓ All checks PASSED for [${name}]`);
}

async function main() {
  try {
    // 1. Splash Screen
    await verifyPage('Splash Screen', '/', [
      'Harvest',
      'Cultivate Today.',
      'Shape Tomorrow.',
      'Bringing farmers, cultivation insights, and connected agricultural data together',
      'Rooted in the land.',
      'Powered by intelligence.',
      'Malle Govardhan',
      'CRAFTED WITH PURPOSE BY',
      'AGRICULTURE',
      'INTELLIGENCE',
      'PROGRESS',
      'build a smarter, more informed, and resilient farming future',
      'YOUR AGRICULTURAL WORKSPACE IS READY',
      'splash-screen',
    ]);

    // 2. Login Page
    await verifyPage('Login Page', '/login', [
      'Smart Farmer',
      'Loading Harvest(a) sign in...',
    ]);

    // 3. Farmer Registration Page
    await verifyPage('Farmer Registration Page', '/register', [
      'Smart Farmer',
      'Create Your Farmer Account',
      'Join farmers across India growing smarter',
      'sf-reg-firstname',
      'sf-reg-lastname',
      'sf-reg-email',
      'sf-reg-phone',
      'sf-reg-password',
      'sf-reg-confirm-password',
      'sf-reg-terms',
      'sf-register-submit-btn',
      'register-to-login-link',
    ]);

    // 4. Password Reset Callback Page
    await verifyPage('Password Reset Callback Page', '/reset-password', [
      'Smart Farmer',
      'Reset Your Password',
      'sf-reset-password',
      'sf-reset-confirm-password',
      'sf-reset-submit-btn',
      'reset-back-to-login'
    ]);

    // 5. Protected Route Auth Guards (Must protect farmer workspace from unauthenticated access)
    const protectedRoutes = [
      { name: 'Farmer Dashboard', path: '/dashboard' },
      { name: 'My Plots', path: '/plots' },
      { name: 'Add Plot Page', path: '/plots/new' },
      { name: 'My Cultivations', path: '/cultivations' },
      { name: 'Add Cultivation Page', path: '/cultivations/new' },
      { name: 'Cultivation History', path: '/history' },
      { name: 'Regional Analytics', path: '/analytics' },
      { name: 'Notifications', path: '/notifications' },
      { name: 'Farmer Profile', path: '/profile' },
      { name: 'Settings', path: '/settings' },
      { name: 'Admin Geography Console', path: '/admin/geography' },
    ];

    for (const route of protectedRoutes) {
      await verifyPage(route.name, route.path, [
        // Verifies AuthGuard / security layer activates for protected routes
        'Validating farmer session',
      ]);
    }

    console.log('\n============================================================');
    console.log('🎉 ALL 15 CORE WEBSITE PAGES & GUARDS VERIFIED SUCCESSFULLY!');
    console.log('============================================================');
  } catch (err) {
    console.error('\n❌ Verification Failed:', err.message);
    process.exit(1);
  }
}

main();
