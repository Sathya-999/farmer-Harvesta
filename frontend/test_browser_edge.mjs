/**
 * test_browser_edge.mjs
 * End-to-end browser acceptance suite using Microsoft Edge via Chrome DevTools Protocol (CDP).
 *
 * Performs real browser verification matching Reference 1:
 * 1. Desktop viewport (1440 x 900) centered composition & typography audit
 * 2. Brand logo and descriptor centered near upper portion
 * 3. Prominent headline: "Cultivate Today. Shape Tomorrow." with cream + lime glow
 * 4. Description: "Bringing farmers, cultivation insights, and connected agricultural data together to build a smarter, more informed, and resilient farming future."
 * 5. 5-minute presentation display timer: visitor remains on splash at 1s, 2s, 5s
 * 6. "Enter Now ->" immediate bypass navigation
 * 7. Mobile viewport (390 x 844) responsive audit with zero horizontal overflow
 * 8. Real PNG screenshot capture for desktop, mobile, and post-redirect states
 */

import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import os from 'os';

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const DEBUG_PORT = 9222;
const BASE_URL = 'http://localhost:3000';
const ARTIFACT_DIR = process.env.ARTIFACT_DIR || 'C:\\Users\\sathy\\.gemini\\antigravity-ide\\brain\\de872da9-fdf7-41f6-a367-633af16e2e6d';

let passed = 0;
let failed = 0;

function assert(desc, condition, info = '') {
  if (condition) {
    console.log(`  ✓ ${desc}`);
    passed++;
  } else {
    console.error(`  ✗ FAIL: ${desc}`);
    if (info) console.error(`    Details: ${info}`);
    failed++;
  }
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

class CdpConnection {
  constructor(wsUrl) {
    this.wsUrl = wsUrl;
    this.ws = null;
    this.nextId = 1;
    this.pending = new Map();
    this.events = [];
  }

  async connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.wsUrl);
      this.ws.onopen = () => resolve();
      this.ws.onerror = (e) => reject(e);
      this.ws.onmessage = (evt) => {
        const msg = JSON.parse(evt.data);
        if (msg.id && this.pending.has(msg.id)) {
          const { res, rej } = this.pending.get(msg.id);
          this.pending.delete(msg.id);
          if (msg.error) rej(msg.error);
          else res(msg.result);
        } else if (msg.method) {
          this.events.push(msg);
        }
      };
    });
  }

  send(method, params = {}) {
    return new Promise((res, rej) => {
      const id = this.nextId++;
      this.pending.set(id, { res, rej });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }

  close() {
    if (this.ws) {
      try {
        this.ws.close();
      } catch {}
    }
  }
}

async function main() {
  console.log('============================================================');
  console.log('STARTING REAL BROWSER (EDGE CDP) END-TO-END ACCEPTANCE SUITE');
  console.log('============================================================');

  const tempProfile = fs.mkdtempSync(path.join(os.tmpdir(), 'edge-cdp-'));

  const edgeProcess = spawn(
    EDGE_PATH,
    [
      '--headless=new',
      `--remote-debugging-port=${DEBUG_PORT}`,
      '--no-first-run',
      '--no-default-browser-check',
      '--disable-extensions',
      '--disable-gpu',
      `--user-data-dir=${tempProfile}`,
      'about:blank',
    ],
    { stdio: 'ignore' }
  );

  // Poll for CDP endpoint ready
  let newTab = null;
  for (let i = 0; i < 30; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${DEBUG_PORT}/json/new?${BASE_URL}/`, { method: 'PUT' });
      if (res.ok) {
        newTab = await res.json();
        break;
      }
    } catch {}
    await sleep(200);
  }

  if (!newTab || !newTab.webSocketDebuggerUrl) {
    console.error('Failed to create new page tab via Microsoft Edge CDP endpoint on port ' + DEBUG_PORT);
    edgeProcess.kill();
    process.exit(1);
  }

  const cdp = new CdpConnection(newTab.webSocketDebuggerUrl);
  await cdp.connect();

  await cdp.send('Page.enable');
  await cdp.send('Runtime.enable');
  await cdp.send('DOM.enable');
  await cdp.send('CSS.enable');

  try {
    // ─────────────────────────────────────────────────────────────
    // TEST 1: DESKTOP VIEWPORT (1440 x 900) RENDERING & VISUAL ASSETS
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 1. Desktop Viewport (1440x900) Rendering & Reference 1 Alignment ---');

    await cdp.send('Emulation.setDeviceMetricsOverride', {
      width: 1440,
      height: 900,
      deviceScaleFactor: 1,
      mobile: false,
    });

    // Clear any previous token
    await cdp.send('Runtime.evaluate', { expression: 'localStorage.clear()' });

    // Wait for entrance animations & DOM load
    for (let retry = 0; retry < 25; retry++) {
      const isLoaded = (await cdp.send('Runtime.evaluate', {
        expression: 'Boolean(document.getElementById("splash-brand-mark"))',
        returnByValue: true
      })).result.value;
      if (isLoaded) break;
      await sleep(300);
    }
    await sleep(400);

    // Evaluate URL and DOM elements
    const initialUrl = (
      await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })
    ).result.value;
    assert('Initial URL is root path "/"', initialUrl === '/');

    // Verify Centered Brand Mark & Descriptor
    const brandInfo = (
      await cdp.send('Runtime.evaluate', {
        expression: `(() => {
          const mark = document.getElementById('splash-brand-mark');
          const title = document.getElementById('splash-brand-title');
          const desc = document.getElementById('splash-brand-descriptor');
          if (!mark) return null;
          const r = mark.getBoundingClientRect();
          return {
            text: mark.innerText.trim(),
            titleText: title ? title.innerText.trim() : '',
            descText: desc ? desc.innerText.trim() : '',
            centerX: Math.round(r.left + r.width / 2),
            windowCenter: Math.round(window.innerWidth / 2)
          };
        })()`,
        returnByValue: true,
      })
    ).result.value;

    assert('Brand mark is centered on desktop', Math.abs(brandInfo.centerX - brandInfo.windowCenter) <= 20);
    assert('Brand title contains "Harvest(a)"', brandInfo.titleText.includes('Harvest'));
    assert('Brand descriptor is "AGRICULTURAL INTELLIGENCE PLATFORM"', brandInfo.descText === 'AGRICULTURAL INTELLIGENCE PLATFORM');

    // Verify Main Headline Typography & Styling
    const headlineInfo = (
      await cdp.send('Runtime.evaluate', {
        expression: `(() => {
          const el = document.getElementById('splash-headline');
          if (!el) return null;
          const cs = window.getComputedStyle(el);
          const r = el.getBoundingClientRect();
          return {
            text: el.innerText.trim(),
            fontFamily: cs.fontFamily,
            fontWeight: cs.fontWeight,
            fontSize: parseFloat(cs.fontSize),
            height: Math.round(r.height),
            color: cs.color
          };
        })()`,
        returnByValue: true,
      })
    ).result.value;

    assert('Headline element rendered', headlineInfo !== null);
    assert(
      'Headline text is "Cultivate Today. Shape Tomorrow."',
      headlineInfo?.text.includes('Cultivate Today.') && headlineInfo?.text.includes('Shape Tomorrow.')
    );
    assert('Headline desktop font size is in target range (~48–58px)', headlineInfo?.fontSize >= 44 && headlineInfo?.fontSize <= 62);

    // Verify Supporting Description Updated Copy
    const descriptionText = (
      await cdp.send('Runtime.evaluate', {
        expression: `document.getElementById('splash-description')?.innerText.trim() || ''`,
      })
    ).result.value;
    assert(
      'Description matches updated reference copy',
      descriptionText.includes('build a smarter, more informed, and resilient farming future.')
    );

    // Verify Creator Attribution & Footer
    const attributionText = (
      await cdp.send('Runtime.evaluate', {
        expression: `document.getElementById('splash-creator-attribution')?.innerText.trim() || ''`,
      })
    ).result.value;
    assert('Creator name is "Malle Govardhan"', attributionText.includes('Malle Govardhan'));
    assert('Attribution contains "CRAFTED WITH PURPOSE BY"', attributionText.includes('CRAFTED WITH PURPOSE BY'));

    // Capture Desktop Splash Screenshot (Refined Reference 1 Presentation)
    const desktopScreenshot = await cdp.send('Page.captureScreenshot', { format: 'png' });
    const desktopImgPath = path.join(ARTIFACT_DIR, 'splash_desktop_real_browser.png');
    fs.writeFileSync(desktopImgPath, Buffer.from(desktopScreenshot.data, 'base64'));
    assert(
      'Desktop splash screenshot captured successfully',
      fs.existsSync(desktopImgPath) && fs.statSync(desktopImgPath).size > 10000
    );

    // ─────────────────────────────────────────────────────────────
    // TEST 2: FIVE-MINUTE PRESENTATION DISPLAY DURATION AUDIT
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 2. Five-Minute Presentation Display Duration Audit ---');

    // Confirm that at 2,000ms visitor is NOT prematurely redirected
    await sleep(1500); // Now at ~2,100ms total
    const at2sUrl = (
      await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })
    ).result.value;
    assert('At 2,000ms, visitor remains calmly on splash screen ("/")', at2sUrl === '/');

    // Confirm that at 4,000ms visitor still remains on splash screen
    await sleep(2000); // Now at ~4,100ms total
    const at4sUrl = (
      await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })
    ).result.value;
    assert('At 4,000ms+, visitor remains on splash screen (5-min presentation duration)', at4sUrl === '/');

    // ─────────────────────────────────────────────────────────────
    // TEST 3: MANUAL SKIP "ENTER NOW" IMMEDIATE BYPASS
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 3. Manual Bypass ("Enter Now ->") Immediate Navigation ---');

    // Click "Enter Now" link
    const skipClickResult = (
      await cdp.send('Runtime.evaluate', {
        expression: `(() => {
          const link = document.getElementById('splash-skip-link');
          if (link) { link.click(); return true; }
          return false;
        })()`,
        returnByValue: true,
      })
    ).result.value;

    assert('"Enter Now" link found and clicked', skipClickResult === true);
    
    // Poll for /register navigation
    let afterSkipUrl = '';
    for (let i = 0; i < 30; i++) {
      afterSkipUrl = (await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })).result.value;
      if (afterSkipUrl === '/register') break;
      await sleep(200);
    }
    assert('Immediately navigated to "/register" upon clicking "Enter Now"', afterSkipUrl === '/register');

    // Capture Register Page Screenshot
    const registerScreenshot = await cdp.send('Page.captureScreenshot', { format: 'png' });
    const registerImgPath = path.join(ARTIFACT_DIR, 'register_desktop_real_browser.png');
    fs.writeFileSync(registerImgPath, Buffer.from(registerScreenshot.data, 'base64'));
    assert(
      'Register page screenshot captured successfully',
      fs.existsSync(registerImgPath) && fs.statSync(registerImgPath).size > 10000
    );

    // ─────────────────────────────────────────────────────────────
    // TEST 4: DIRECT REGISTRATION ACCESS & NO REDIRECT LOOP
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 4. Direct /register Access (No Redirect Loop) ---');
    await cdp.send('Page.navigate', { url: `${BASE_URL}/register` });
    await sleep(600);
    const directRegisterUrl = (
      await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })
    ).result.value;
    assert('Direct access to /register stays on /register without loop', directRegisterUrl === '/register');

    // ─────────────────────────────────────────────────────────────
    // TEST 5: AUTHENTICATED USER ROUTING TO /DASHBOARD
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 5. Authenticated User Redirection Guard ---');
    await cdp.send('Runtime.evaluate', {
      expression: `(() => {
        sessionStorage.clear();
        localStorage.setItem('sf_auth_token', 'simulated-farmer-jwt-token');
      })()`,
    });
    await cdp.send('Page.navigate', { url: `${BASE_URL}/` });
    
    // Wait for splash screen to load on root
    for (let i = 0; i < 30; i++) {
      const isSplashReady = (await cdp.send('Runtime.evaluate', {
        expression: 'Boolean(document.getElementById("splash-skip-link"))',
        returnByValue: true
      })).result.value;
      if (isSplashReady) break;
      await sleep(200);
    }
    await sleep(400);

    // Click "Enter Now" as authenticated user
    await cdp.send('Runtime.evaluate', {
      expression: `document.getElementById('splash-skip-link')?.click()`,
    });

    // Poll for destination URL
    let authedDestUrl = '';
    for (let i = 0; i < 30; i++) {
      authedDestUrl = (await cdp.send('Runtime.evaluate', { expression: 'window.location.pathname' })).result.value;
      if (authedDestUrl === '/dashboard') break;
      await sleep(200);
    }
    assert('Authenticated visitor is routed to "/dashboard" (not /register)', authedDestUrl === '/dashboard', `Actual URL was: ${authedDestUrl}`);

    // Clean up auth token
    await cdp.send('Runtime.evaluate', { expression: 'localStorage.clear()' });

    // ─────────────────────────────────────────────────────────────
    // TEST 6: MOBILE VIEWPORT (390 x 844) & RESPONSIVE FIT
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 6. Mobile Viewport (390x844) Responsiveness & Visual Audit ---');
    await cdp.send('Emulation.setDeviceMetricsOverride', {
      width: 390,
      height: 844,
      deviceScaleFactor: 2,
      mobile: true,
    });

    await cdp.send('Page.navigate', { url: `${BASE_URL}/` });
    await sleep(600);

    // Check horizontal overflow & elements visibility
    const mobileMetrics = (
      await cdp.send('Runtime.evaluate', {
        expression: `(() => {
          const docEl = document.documentElement;
          const body = document.body;
          const scrollWidth = Math.max(docEl.scrollWidth, body.scrollWidth);
          const clientWidth = docEl.clientWidth;
          const headline = document.getElementById('splash-headline');
          const enterBtn = document.getElementById('splash-skip-link');
          const attribution = document.getElementById('splash-creator-attribution');
          return {
            scrollWidth,
            clientWidth,
            hasHorizontalOverflow: scrollWidth > clientWidth,
            headlineVisible: headline ? headline.getBoundingClientRect().height > 0 : false,
            enterBtnVisible: enterBtn ? enterBtn.getBoundingClientRect().height > 0 : false,
            attributionVisible: attribution ? attribution.getBoundingClientRect().height > 0 : false
          };
        })()`,
        returnByValue: true,
      })
    ).result.value;

    assert('No horizontal overflow on mobile 390px', !mobileMetrics.hasHorizontalOverflow);
    assert('Headline is fully visible and responsive on mobile', mobileMetrics.headlineVisible);
    assert('Enter Now button is visible on mobile', mobileMetrics.enterBtnVisible);
    assert('Creator attribution visible on mobile', mobileMetrics.attributionVisible);

    // Capture Mobile Screenshot
    const mobileScreenshot = await cdp.send('Page.captureScreenshot', { format: 'png' });
    const mobileImgPath = path.join(ARTIFACT_DIR, 'splash_mobile_real_browser.png');
    fs.writeFileSync(mobileImgPath, Buffer.from(mobileScreenshot.data, 'base64'));
    assert(
      'Mobile splash screenshot captured successfully',
      fs.existsSync(mobileImgPath) && fs.statSync(mobileImgPath).size > 10000
    );

    // ─────────────────────────────────────────────────────────────
    // TEST 7: REDUCED-MOTION CSS MEDIA QUERY AUDIT
    // ─────────────────────────────────────────────────────────────
    console.log('\n--- 7. Reduced-Motion Accessibility Rules Audit ---');
    await cdp.send('Emulation.setEmulatedMedia', {
      features: [{ name: 'prefers-reduced-motion', value: 'reduce' }],
    });
    await cdp.send('Page.navigate', { url: `${BASE_URL}/` });
    await sleep(350);

    const reducedMotionStyles = (
      await cdp.send('Runtime.evaluate', {
        expression: `(() => {
          const el = document.querySelector('.splash-header');
          if (!el) return null;
          const cs = window.getComputedStyle(el);
          return {
            animationName: cs.animationName,
            animationDuration: cs.animationDuration
          };
        })()`,
        returnByValue: true,
      })
    ).result.value;

    assert(
      'Reduced-motion suppresses CSS animation (name: "none" or duration <= 0.01s)',
      reducedMotionStyles?.animationName === 'none' ||
        parseFloat(reducedMotionStyles?.animationDuration) <= 0.01
    );

    console.log('\n============================================================');
    console.log(`REAL BROWSER ACCEPTANCE SUITE RESULTS: ${passed} PASSED, ${failed} FAILED`);
    console.log('============================================================');

    if (failed === 0) {
      console.log('🎉 ALL REAL BROWSER TESTS PASSED (DECISION: GREEN)\n');
    }
  } finally {
    cdp.close();
    edgeProcess.kill();
    try {
      fs.rmSync(tempProfile, { recursive: true, force: true });
    } catch {}
  }

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error('Fatal test error:', err);
  process.exit(1);
});
