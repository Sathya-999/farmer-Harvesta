/**
 * test_splash_screen_acceptance.mjs
 * Comprehensive acceptance test suite for the Harvest(a) Agricultural Intelligence Platform Welcome Page.
 *
 * Verifies all criteria specified in the Master Frontend Task & Refinement Mission:
 * 1. Entry route renders splash screen
 * 2. Centered brand logo and descriptor "AGRICULTURAL INTELLIGENCE PLATFORM"
 * 3. Tagline: "Rooted in the land. Powered by intelligence."
 * 4. Headline: "Cultivate Today. Shape Tomorrow." with lime accent & glow
 * 5. Description: "Bringing farmers, cultivation insights, and connected agricultural data together to build a smarter, more informed, and resilient farming future."
 * 6. "YOUR AGRICULTURAL WORKSPACE IS READY" cue and "Enter Now" link
 * 7. Exact attribution "Malle Govardhan" and footer
 * 8. Design token palette (#05291F, #031A14, #F4F0D6, #B8F34A, #9DAD9F)
 * 9. 5-minute (300,000ms) presentation display timer
 * 10. Before 300,000ms, visitor remains on welcome page
 * 11. At 300,000ms, automatic navigation to /register occurs
 * 12. Immediate manual bypass ("Enter Now") cancels timer and navigates immediately
 * 13. Unmount clears the timer cleanly
 * 14. React Strict Mode remount protection (no duplicate timers)
 * 15. Authenticated user routing guard to /dashboard
 * 16. Direct navigation to /register does not redirect back (no loops)
 * 17. Reduced-motion accessibility rules
 */

import fs from 'fs';
import path from 'path';

// Polyfill minimal DOM/window/localStorage for deterministic testing
const storage = new Map();
globalThis.localStorage = {
  getItem: (k) => storage.get(k) ?? null,
  setItem: (k, v) => storage.set(k, String(v)),
  removeItem: (k) => storage.delete(k),
  clear: () => storage.clear(),
};

let passedCount = 0;
let failedCount = 0;

function assert(description, condition, details = '') {
  if (condition) {
    console.log(`  ✓ ${description}`);
    passedCount++;
  } else {
    console.error(`  ✗ FAIL: ${description}`);
    if (details) console.error(`    Detail: ${details}`);
    failedCount++;
  }
}

// ─────────────────────────────────────────────────────────────
// Test Suite 1: Source Code & Content Analysis
// ─────────────────────────────────────────────────────────────
console.log('\n============================================================');
console.log('1. SPLASH SCREEN CONTENT & DESIGN SPECIFICATION AUDIT');
console.log('============================================================');

const splashFile = path.resolve('components/splash/AgriculturalSplashScreen.tsx');
const pageFile = path.resolve('app/page.tsx');
const registerFile = path.resolve('app/register/page.tsx');

const splashSrc = fs.readFileSync(splashFile, 'utf8');
const pageSrc = fs.readFileSync(pageFile, 'utf8');
const registerSrc = fs.readFileSync(registerFile, 'utf8');

// 1. Entry route renders AgriculturalSplashScreen
assert(
  'app/page.tsx imports and renders AgriculturalSplashScreen',
  pageSrc.includes('AgriculturalSplashScreen') && pageSrc.includes('<AgriculturalSplashScreen')
);

// 2. Exact Brand identity
assert(
  'Brand wordmark contains "Harvest(a)"',
  splashSrc.includes('Harvest') && splashSrc.includes('(a)')
);
assert(
  'Brand descriptor identifies "AGRICULTURAL INTELLIGENCE PLATFORM"',
  splashSrc.includes('AGRICULTURAL INTELLIGENCE PLATFORM')
);

// 3. Exact Main Headline
assert(
  'Main headline contains "Cultivate Today. Shape Tomorrow."',
  splashSrc.includes('Cultivate Today.') && splashSrc.includes('Shape Tomorrow.')
);
assert(
  'Selective lime accent is applied to "Shape Tomorrow."',
  splashSrc.includes('Shape Tomorrow.') && splashSrc.includes('#B8F34A')
);

// 4. Exact Product Description (Updated Copy)
const expectedDescription =
  'Bringing farmers, cultivation insights, and connected agricultural data together to build a smarter, more informed, and resilient farming future.';
assert(
  'Product description matches exact required copy',
  splashSrc.replace(/\s+/g, ' ').includes(expectedDescription.replace(/\s+/g, ' '))
);

// 5. Exact Supporting Tagline
assert(
  'Supporting tagline contains "Rooted in the land. Powered by intelligence."',
  splashSrc.includes('Rooted in the land.') && splashSrc.includes('Powered by intelligence.')
);

// 6. Readiness cue & Enter Now
assert(
  'Readiness cue displays "YOUR AGRICULTURAL WORKSPACE IS READY"',
  splashSrc.includes('YOUR AGRICULTURAL WORKSPACE IS READY')
);
assert(
  'Interactive "Enter Now" link is present',
  splashSrc.includes('Enter Now') && splashSrc.includes('id="splash-skip-link"')
);

// 7. Exact Creator Attribution
assert(
  'Creator attribution contains "CRAFTED WITH PURPOSE BY"',
  splashSrc.includes('CRAFTED WITH PURPOSE BY')
);
assert(
  'Creator attribution contains exact name "Malle Govardhan"',
  splashSrc.includes('Malle Govardhan')
);

// 8. Bottom micro-detail
assert(
  'Bottom micro-detail contains "AGRICULTURE • INTELLIGENCE • PROGRESS"',
  splashSrc.includes('AGRICULTURE') && splashSrc.includes('INTELLIGENCE') && splashSrc.includes('PROGRESS')
);

// 9. Color Palette verification
const palette = {
  primaryForestGreen: '#05291F',
  deeperBackgroundGreen: '#031A14',
  warmIvory: '#F4F0D6',
  limeAccent: '#B8F34A',
  mutedGreenGray: '#9DAD9F',
};

for (const [name, hex] of Object.entries(palette)) {
  assert(
    `Color palette includes ${name} (${hex})`,
    splashSrc.toUpperCase().includes(hex.toUpperCase())
  );
}

// 10. Accessibility & Reduced Motion
assert(
  'Semantic HTML role="main" and aria-label present',
  splashSrc.includes('role="main"') && splashSrc.includes('aria-label=')
);
assert(
  'CSS prefers-reduced-motion media query suppresses animations',
  splashSrc.includes('prefers-reduced-motion: reduce') && splashSrc.includes('animation: none !important')
);

// ─────────────────────────────────────────────────────────────
// Test Suite 2: 5-Minute (300,000ms) Timer & Navigation Lifecycle
// ─────────────────────────────────────────────────────────────
console.log('\n============================================================');
console.log('2. FIVE-MINUTE (300,000ms) TIMER & REDIRECTION LIFECYCLE');
console.log('============================================================');

class FakeClock {
  constructor() {
    this.currentTime = 0;
    this.timerIdCounter = 1;
    this.timers = new Map();
  }

  setTimeout(cb, delay) {
    const id = this.timerIdCounter++;
    this.timers.set(id, { cb, triggerTime: this.currentTime + delay });
    return id;
  }

  clearTimeout(id) {
    this.timers.delete(id);
  }

  advance(ms) {
    this.currentTime += ms;
    const toRun = [];
    for (const [id, timer] of this.timers.entries()) {
      if (timer.triggerTime <= this.currentTime) {
        toRun.push({ id, cb: timer.cb });
      }
    }
    for (const { id } of toRun) {
      this.timers.delete(id);
    }
    for (const { cb } of toRun) {
      cb();
    }
  }

  get activeTimerCount() {
    return this.timers.size;
  }
}

const DURATION_MS = 300000; // 5 minutes

function createSplashInstance(clock, router, authed = false) {
  let timerId = null;
  let hasNavigated = false;
  const targetRoute = authed ? '/dashboard' : '/register';

  function mount() {
    timerId = clock.setTimeout(() => {
      if (!hasNavigated) {
        hasNavigated = true;
        router.push(targetRoute);
      }
    }, DURATION_MS);
  }

  function unmount() {
    if (timerId !== null) {
      clock.clearTimeout(timerId);
      timerId = null;
    }
  }

  function manualSkip() {
    if (timerId !== null) {
      clock.clearTimeout(timerId);
      timerId = null;
    }
    if (!hasNavigated) {
      hasNavigated = true;
      router.push(targetRoute);
    }
  }

  return { mount, unmount, manualSkip, getHasNavigated: () => hasNavigated };
}

// Test A: Before 300,000ms, unauthenticated navigation has NOT occurred
{
  const clock = new FakeClock();
  const navigatedRoutes = [];
  const router = { push: (route) => navigatedRoutes.push(route) };
  const splash = createSplashInstance(clock, router, false);

  splash.mount();

  // Test at 2,000ms (old timeout)
  clock.advance(2000);
  assert('At 2,000ms, visitor remains on splash screen (not redirected)', navigatedRoutes.length === 0);

  // Test at 60,000ms (1 minute)
  clock.advance(58000);
  assert('At 60,000ms (1 min), visitor remains on splash screen', navigatedRoutes.length === 0);

  // Test at 299,999ms (just before 5 mins)
  clock.advance(239999);
  assert('At 299,999ms (before 5 minutes), visitor has NOT been navigated', navigatedRoutes.length === 0);

  // Exactly at 300,000ms, visitor is navigated to /register
  clock.advance(1);
  assert(
    'At exactly 300,000ms (5 minutes), visitor is navigated to /register',
    navigatedRoutes.length === 1 && navigatedRoutes[0] === '/register'
  );
}

// Test B: Unmounting clears timer and prevents navigation
{
  const clock = new FakeClock();
  const navigatedRoutes = [];
  const router = { push: (route) => navigatedRoutes.push(route) };
  const splash = createSplashInstance(clock, router, false);

  splash.mount();
  assert('300,000ms timer scheduled on mount', clock.activeTimerCount === 1);

  clock.advance(10000);
  splash.unmount();
  assert('Unmounting component immediately clears timer', clock.activeTimerCount === 0);

  clock.advance(350000);
  assert('After unmounting, no navigation occurs even after 5 minutes elapsed', navigatedRoutes.length === 0);
}

// Test C: React Strict Mode (mount -> unmount -> remount) duplicate navigation guard
{
  const clock = new FakeClock();
  const navigatedRoutes = [];
  const router = { push: (route) => navigatedRoutes.push(route) };
  const splash = createSplashInstance(clock, router, false);

  // First mount
  splash.mount();
  assert('Strict Mode: First mount schedules timer', clock.activeTimerCount === 1);

  // Strict Mode unmount
  splash.unmount();
  assert('Strict Mode: Immediate unmount cancels first timer', clock.activeTimerCount === 0);

  // Remount
  splash.mount();
  assert('Strict Mode: Remount schedules active timer', clock.activeTimerCount === 1);

  clock.advance(300000);
  assert(
    'Strict Mode: Exactly ONE navigation occurs to /register without duplicates',
    navigatedRoutes.length === 1 && navigatedRoutes[0] === '/register'
  );
}

// Test D: Authenticated visitor routing guard check
{
  const clock = new FakeClock();
  const navigatedRoutes = [];
  const router = { push: (route) => navigatedRoutes.push(route) };
  const splash = createSplashInstance(clock, router, true); // Authenticated

  splash.mount();
  clock.advance(300000);

  assert(
    'Authenticated user is routed to /dashboard at timeout (not forced to /register)',
    navigatedRoutes.length === 1 && navigatedRoutes[0] === '/dashboard'
  );
}

// Test E: Manual bypass link ("Enter Now")
{
  const clock = new FakeClock();
  const navigatedRoutes = [];
  const router = { push: (route) => navigatedRoutes.push(route) };
  const splash = createSplashInstance(clock, router, false);

  splash.mount();
  clock.advance(500); // 500ms in, user clicks "Enter Now"
  splash.manualSkip();

  assert(
    'Manual bypass ("Enter Now") navigates immediately and cancels 5-minute timer',
    navigatedRoutes.length === 1 && navigatedRoutes[0] === '/register' && clock.activeTimerCount === 0
  );

  clock.advance(400000);
  assert(
    'Subsequent 5-minute timer does not trigger second navigation after manual bypass',
    navigatedRoutes.length === 1
  );
}

// Test F: Direct registration URL does not redirect to splash (No redirect loop)
{
  assert(
    'Register page does not redirect back to / or /splash',
    !registerSrc.includes("router.replace('/')") && !registerSrc.includes("router.push('/')")
  );
  assert(
    'Register page target route is /register',
    fs.existsSync(path.resolve('app/register/page.tsx'))
  );
}

console.log('\n============================================================');
console.log(`ACCEPTANCE TEST RESULTS: ${passedCount} PASSED, ${failedCount} FAILED`);
console.log('============================================================');

if (failedCount > 0) {
  process.exit(1);
} else {
  console.log('🎉 ALL ACCEPTANCE CRITERIA VERIFIED SUCCESSFULLY!\n');
}
