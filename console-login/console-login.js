/**
 * Google Login for Unbxd Console
 * Login page: CONSOLE_LOGIN_BASE_URL in .env (default https://console.unbxd.io) + /unbxdlogin
 *
 * Flow:
 * 1. Navigate to login page
 * 2. Click "Log in with google" (anchor to Google OAuth)
 * 3. Enter email, Next, password, Next on Google
 * 4. TOTP 2FA if required
 * 5. Redirect back to console → navigate to regional URL → capture cookies
 */

import puppeteerExtra from 'puppeteer-extra';
import StealthPlugin from 'puppeteer-extra-plugin-stealth';
import { executablePath as puppeteerBundledExecutablePath } from 'puppeteer';
import dotenv from 'dotenv';
import { authenticator } from 'otplib';
import fs from 'fs';
import path from 'path';
import os from 'os';

// Apply stealth plugin — patches all fingerprinting signals Google checks
puppeteerExtra.use(StealthPlugin());

dotenv.config();

/** Base URL for /unbxdlogin (no trailing slash). Same file as credentials — set in .env. */
function consoleLoginBaseUrl() {
  const envBase = process.env.CONSOLE_LOGIN_BASE_URL;
  const raw = envBase != null ? String(envBase).trim() : '';
  const base = raw && raw.length > 0 ? raw : 'https://console.unbxd.io';
  return base.replace(/\/+$/, '');
}

/** Windows: Chrome-for-Testing under Puppeteer cache. */
function getChromeExecutableFromCacheWin() {
  const cacheDir = process.env.PUPPETEER_CACHE_DIR || path.join(os.homedir(), '.cache', 'puppeteer');
  const chromeDir = path.join(cacheDir, 'chrome');
  if (!fs.existsSync(chromeDir)) return null;
  const entries = fs.readdirSync(chromeDir, { withFileTypes: true });
  const winDir = entries.find(d => d.isDirectory() && d.name.startsWith('win64-'));
  if (!winDir) return null;
  const exe = path.join(chromeDir, winDir.name, 'chrome-win64', 'chrome.exe');
  return fs.existsSync(exe) ? exe : null;
}

/**
 * Stable Chrome for Puppeteer (Maven/Java often invokes Node where implicit bundled resolution fails).
 * Order: PUPPETEER_EXECUTABLE_PATH → puppeteer.executablePath() → OS defaults → Windows cache.
 */
function resolveChromeExecutable() {
  const envPath = process.env.PUPPETEER_EXECUTABLE_PATH;
  const fromEnv = envPath != null ? String(envPath).trim() : '';
  if (fromEnv && fs.existsSync(fromEnv)) {
    return fromEnv;
  }

  try {
    const bundled = puppeteerBundledExecutablePath();
    if (bundled && fs.existsSync(bundled)) {
      return bundled;
    }
  } catch (e) {
    // ignore
  }

  if (process.platform === 'darwin') {
    const candidates = [
      '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
      '/Applications/Google Chrome Beta.app/Contents/MacOS/Google Chrome Beta',
    ];
    for (const c of candidates) {
      if (fs.existsSync(c)) return c;
    }
  }

  if (process.platform === 'linux') {
    const candidates = ['/usr/bin/google-chrome-stable', '/usr/bin/google-chrome', '/usr/bin/chromium-browser', '/usr/bin/chromium'];
    for (const c of candidates) {
      if (fs.existsSync(c)) return c;
    }
  }

  if (process.platform === 'win32') {
    return getChromeExecutableFromCacheWin();
  }

  return null;
}

const CONSOLE_BASE_URL = consoleLoginBaseUrl();
const LOGIN_PAGE_URL = `${CONSOLE_BASE_URL}/unbxdlogin`;

const GOOGLE_EMAIL = process.env.GOOGLE_EMAIL;
const GOOGLE_PASSWORD = process.env.GOOGLE_PASSWORD;
const TOTP_SECRET = process.env.TOTP_SECRET;
const HEADLESS = process.env.HEADLESS === 'true' || process.env.HEADLESS === '1';

function generateTOTPCode() {
  if (!TOTP_SECRET) return null;
  try {
    // Normalize to uppercase (base32 secrets from Google Authenticator are uppercase)
    // window:1 accepts current ± adjacent 30-second window to tolerate minor clock drift
    authenticator.options = { digits: 6, step: 30, window: 1 };
    return authenticator.generate(TOTP_SECRET.toUpperCase().replace(/\s/g, ''));
  } catch (e) {
    return null;
  }
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

/** Short wait after DOM appears — prefer navigation/selectors over long fixed delays. */
const microWait = () => sleep(120);

/** Clicks in the page main frame only — avoids $$eval + navigation races that trigger
 * "Argument should belong to the same JavaScript world as target object" on some Chrome/Puppeteer builds. */
async function clickInPage(page, fn) {
  return page.evaluate(fn);
}

/** Milliseconds to wait for the user to solve CAPTCHA / image challenges manually (default 2 min). */
function captchaManualWaitMs() {
  const raw = process.env.CAPTCHA_MANUAL_WAIT_MS;
  if (raw != null && String(raw).trim() !== '') {
    const n = parseInt(String(raw).trim(), 10);
    if (!Number.isNaN(n) && n >= 0) return n;
  }
  return 120000;
}

/**
 * Detects Google CAPTCHA / bot-check screens. Does not treat challenge/pwd (password) as CAPTCHA.
 */
async function pageHasCaptchaLikeChallenge(page) {
  return page.evaluate(() => {
    const href = window.location.href;
    if (/challenge\/(captcha|recaptcha)|recaptcha\/anchor/i.test(href)) return true;
    if (document.querySelector('iframe[src*="recaptcha"], iframe[title*="reCAPTCHA"], iframe[src*="google.com/recaptcha"]')) {
      return true;
    }
    const body = (document.body && document.body.innerText) || '';
    if (/Type the (text|characters|words you hear)|unusual traffic|I'm not a robot|Verify you.{0,30}not a robot/i.test(body)) {
      return true;
    }
    const inputs = [...document.querySelectorAll('input[type="text"], input:not([type])')];
    for (const el of inputs) {
      const hint = `${el.getAttribute('aria-label') || ''} ${el.placeholder || ''} ${el.id || ''}`.toLowerCase();
      if (hint.includes('captcha') || /characters?\s+in\s+the/i.test(hint)) return true;
    }
    return false;
  });
}

/**
 * If a CAPTCHA / verification challenge is present, wait (default 2 min) for manual completion.
 * Never types the account password during this wait — avoids filling CAPTCHA text fields by mistake.
 */
async function waitForManualCaptchaIfNeeded(page) {
  const maxMs = captchaManualWaitMs();
  const pollMs = 2500;
  let elapsed = 0;
  let captchaElapsed = 0;
  let loggedHelp = false;
  let sawCaptcha = false;
  let idleNoPassNoCaptcha = 0;

  while (elapsed < maxMs) {
    const hasPasswd = await page.evaluate(() => {
      const p = document.querySelector('input[name="Passwd"]');
      return !!(p && !p.disabled && p.offsetParent !== null);
    });
    const captcha = await pageHasCaptchaLikeChallenge(page);

    if (captcha) {
      sawCaptcha = true;
      captchaElapsed += pollMs;
    } else {
      captchaElapsed = 0;
    }

    if (hasPasswd && !captcha) {
      if (sawCaptcha) console.log('  [captcha] Challenge cleared — Google password field is ready.');
      return;
    }

    if (!captcha && hasPasswd) return;

    if (captcha) {
      if (!loggedHelp) {
        console.log(
          `  [captcha] CAPTCHA / bot-check detected — waiting up to ${maxMs / 1000}s for manual solve. ` +
            'Use HEADLESS=false. The script will NOT type your password into CAPTCHA fields.'
        );
        loggedHelp = true;
      } else if (captchaElapsed > 0 && Math.floor(captchaElapsed / 30000) > Math.floor((captchaElapsed - pollMs) / 30000)) {
        console.log(`  [captcha] Still waiting… ~${Math.round(captchaElapsed / 1000)}s in CAPTCHA state`);
      }
    }

    if (!captcha && !hasPasswd) {
      idleNoPassNoCaptcha += pollMs;
      if (idleNoPassNoCaptcha >= 20000) {
        return;
      }
    } else {
      idleNoPassNoCaptcha = 0;
    }

    await sleep(pollMs);
    elapsed += pollMs;
  }

  if (sawCaptcha) {
    console.log(`  [captcha] Max wait (${maxMs / 1000}s) reached — continuing; password will only be sent to input[name="Passwd"].`);
  }
}

export async function loginToConsole() {
  if (!GOOGLE_EMAIL || !GOOGLE_PASSWORD) {
    throw new Error('GOOGLE_EMAIL and GOOGLE_PASSWORD required');
  }

  const args = [
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-sync',
    '--disable-features=SyncDisabledWithNoNetwork,IdentityStatusConsistency,ChromeWhatsNewUI',
    '--disable-background-networking',
    '--disable-client-side-phishing-detection',
    '--disable-popup-blocking',
    '--disable-notifications',
    '--disable-infobars',
    '--disable-features=ChromeWhatsNewUI,SyncDisabledWithNoNetwork,IdentityStatusConsistency,ChromeSigninPromotionInfoBar,IdentityConsistencyHeaderAccountSwitcher',
    '--disable-sync-preferences',
    // Prevent navigator.webdriver from being exposed (avoids Google bot detection)
    '--disable-blink-features=AutomationControlled',
    '--disable-extensions',
    ...(HEADLESS
      ? [
          '--no-sandbox',
          '--disable-dev-shm-usage',
          '--headless=new',
          // --ozone-platform=headless is Linux-only, skip on macOS/Windows
          ...(process.platform === 'linux' ? ['--ozone-platform=headless'] : []),
        ]
      : []),
  ];

  const chromeExe = resolveChromeExecutable();
  const t0 = Date.now();
  const browser = await puppeteerExtra.launch({
    headless: HEADLESS ? 'new' : false,
    args,
    ignoreDefaultArgs: ['--enable-automation'],
    ...(chromeExe && { executablePath: chromeExe }),
  });

  // Use default browser context — avoids cross-JS-world errors with $$eval in Puppeteer 24
  const page = await browser.newPage();

  try {
    console.log(`Chromium launched (headless=${HEADLESS}) in ${Date.now() - t0}ms · regional target=${process.env.CONSOLE_TARGET_URL || '(from .env only)'}`);

    const step = async (name, fn) => {
      const ts = Date.now() - t0;
      console.log(`  [+${ts}ms] ${name}`);
      try { return await fn(); }
      catch (e) { throw new Error(name + ' → ' + e.message); }
    };

    // Step 1: Navigate to console login page
    // 'load' is much faster than networkidle0; login page does not need a fully quiet network
    await step(`Step 1: Navigating to ${LOGIN_PAGE_URL}`, () =>
      page.goto(LOGIN_PAGE_URL, { waitUntil: 'load', timeout: 25000 })
    );

    // Step 2: Click "Log in with google" — single main-world evaluate (no cross-world handles)
    await step('Step 2: Clicking "Log in with google"', async () => {
      const clicked = await clickInPage(page, () => {
        const links = [...document.querySelectorAll('a')];
        const link = links.find(l =>
          (l.href && (l.href.includes('accounts.google') || l.href.includes('oauth'))) ||
          (l.textContent && l.textContent.toLowerCase().includes('google'))
        );
        if (link) { link.click(); return true; }
        return false;
      });
      if (!clicked) throw new Error('Could not find "Log in with google" link');
    });

    // Step 3: Wait for Google login page
    await step('Step 3: Waiting for Google login page', () =>
      page.waitForFunction(
        () => window.location.href.includes('accounts.google.com'),
        { timeout: 15000 }
      )
    );

    // Step 4: Enter email (Google uses #identifierId or name=identifier on many flows)
    await step('Step 4: Entering email', async () => {
      await page.waitForFunction(
        () =>
          !!document.querySelector('input[type="email"]') ||
          !!document.querySelector('#identifierId') ||
          !!document.querySelector('input[name="identifier"]'),
        { timeout: 20000 }
      );
      await microWait();
      const emailSelector = await page.evaluate(() => {
        if (document.querySelector('#identifierId')) return '#identifierId';
        if (document.querySelector('input[name="identifier"]')) return 'input[name="identifier"]';
        return 'input[type="email"]';
      });
      await page.focus(emailSelector);
      await page.type(emailSelector, GOOGLE_EMAIL, { delay: 22 });
      await page.keyboard.press('Enter');
      await page.waitForNetworkIdle({ idleTime: 300, timeout: 12000 }).catch(() => sleep(600));
    });

    // Step 4b: Account chooser / "Verify it's you" — click tile matching this email if present
    await step('Step 4b: Account chooser (if shown)', async () => {
      await sleep(500);
      const picked = await page.evaluate((email) => {
        const lower = (email || '').toLowerCase();
        const tiles = [...document.querySelectorAll('[data-identifier], [data-email], div[role="link"], div[role="button"]')];
        for (const el of tiles) {
          const id = (el.getAttribute('data-email') || el.getAttribute('data-identifier') || '').toLowerCase();
          const text = (el.textContent || '').toLowerCase();
          if (id && lower.includes(id)) {
            el.click();
            return 'data-attribute';
          }
          if (lower && text.includes(lower.split('@')[0])) {
            el.click();
            return 'text-match';
          }
        }
        return '';
      }, GOOGLE_EMAIL);
      if (picked) {
        console.log(`  Step 4b: Selected account (${picked})`);
        await page.waitForNetworkIdle({ idleTime: 250, timeout: 10000 }).catch(() => sleep(500));
      }
    });

    // Step 5: Enter password — only Google’s real password field (name="Passwd"), never CAPTCHA text inputs
    await step('Step 5: Entering password', async () => {
      await waitForManualCaptchaIfNeeded(page);
      await page.waitForSelector('input[name="Passwd"]', { visible: true, timeout: 90000 });
      await microWait();
      await page.focus('input[name="Passwd"]');
      await page.type('input[name="Passwd"]', GOOGLE_PASSWORD, { delay: 22 });
      await page.keyboard.press('Enter');
      await page.waitForNetworkIdle({ idleTime: 350, timeout: 12000 }).catch(() => sleep(700));
    });

    // Step 5b: Handle any intermediate Google challenge screens in a loop.
    // Google may show: password re-confirmation, TOTP, "Continue" confirmation, etc.
    for (let i = 0; i < 8; i++) {
      await sleep(450);
      const currentUrl = page.url();
      if (!currentUrl.includes('accounts.google.com')) break;

      if (await pageHasCaptchaLikeChallenge(page)) {
        console.log('  Step 5b: CAPTCHA / verification shown — waiting for manual solve (no Continue clicks).');
        await waitForManualCaptchaIfNeeded(page);
        continue;
      }

      // Password re-confirmation challenge
      if (currentUrl.includes('challenge/pwd')) {
        console.log('  Step 5b: Handling password re-confirmation challenge...');
        await waitForManualCaptchaIfNeeded(page);
        await page.waitForSelector('input[name="Passwd"]', { visible: true, timeout: 60000 });
        await microWait();
        await page.focus('input[name="Passwd"]');
        await page.type('input[name="Passwd"]', GOOGLE_PASSWORD, { delay: 35 });
        await page.keyboard.press('Enter');
        continue;
      }

      // TOTP: use strict selectors only — broad patterns like input[jsname="YPqjbf"] also match
      // unrelated controls (e.g. checkbox) and re-entering this block after OAuth "Continue"
      // caused Runtime.callFunctionOn "same JavaScript world" errors with $$eval + navigation.
      const onTotpUrl =
        currentUrl.includes('challenge/totp') ||
        currentUrl.includes('challenge/ipp') ||
        /\/signin\/challenge\/totp/i.test(currentUrl);
      const totpHandle = await page.$(
        'input[name="totpPin"], input[name="Pin"], input[autocomplete="one-time-code"], ' +
        'input[id="totpPin"][type="tel"], input[type="tel"][id*="totp"]'
      );
      // Do not treat stray totpPin nodes on oauth/consent screens as TOTP — causes re-entry and
      // Runtime.callFunctionOn "same JavaScript world" errors after Continue navigates.
      const shouldRunTotpStep =
        onTotpUrl ||
        /\/challenge\/(totp|ipp|ootp)\b/i.test(currentUrl) ||
        (totpHandle &&
          /challenge/i.test(currentUrl) &&
          !/oauth/i.test(currentUrl) &&
          !/\/consent/i.test(currentUrl));
      if (shouldRunTotpStep) {
        if (totpHandle) await totpHandle.dispose().catch(() => {});
        console.log('  Step 5c: Entering TOTP code...');
        const code = generateTOTPCode();
        if (!code) throw new Error('TOTP required but TOTP_SECRET not set');
        const inputInfo = await clickInPage(page, () =>
          [...document.querySelectorAll('input')].map(i => ({
            type: i.type,
            id: i.id,
            name: i.name,
            ariaLabel: i.getAttribute('aria-label'),
            jsname: i.getAttribute('jsname'),
            autocomplete: i.autocomplete,
          }))
        ).catch(() => []);
        console.log('  Step 5c: inputs on page: ' + JSON.stringify(inputInfo));
        const target =
          (await page.$('input[name="totpPin"]')) ||
          (await page.$('input[name="Pin"]')) ||
          (await page.$('input[autocomplete="one-time-code"]')) ||
          (await page.$('input#totpPin[type="tel"]')) ||
          (await page.$('input[type="tel"]'));
        if (!target) {
          // No input found — page is likely mid-transition after a previous TOTP submission
          console.log('  Step 5c: No TOTP input found, page may be transitioning — waiting...');
          await sleep(1200);
          continue;
        }
        // Determine the best CSS selector for the TOTP field (in priority order)
        const sel = (await page.$('input[name="totpPin"]'))     ? 'input[name="totpPin"]'
                  : (await page.$('input[name="Pin"]'))         ? 'input[name="Pin"]'
                  : (await page.$('input[autocomplete="one-time-code"]')) ? 'input[autocomplete="one-time-code"]'
                  : (await page.$('input[type="tel"]'))         ? 'input[type="tel"]'
                  : 'input:not([type="hidden"]):not([type="submit"]):not([type="button"])';
        await page.focus(sel);
        await sleep(80);
        // Select all + delete to clear
        await page.keyboard.down('Meta');
        await page.keyboard.press('a');
        await page.keyboard.up('Meta');
        await page.keyboard.press('Backspace');
        await sleep(80);
        // Type code via keyboard so JS input events fire correctly
        await page.keyboard.type(code, { delay: 55 });
        await sleep(400);
        // Log length of code for verification (not the code itself)
        console.log('  Step 5c: TOTP code entered (' + code.length + ' digits)');
        // Submit via Enter key
        await page.keyboard.press('Enter');
        await sleep(500);
        // Wait for page to move away from TOTP challenge (headless can be slower — 30s)
        try {
          await page.waitForFunction(
            () => !window.location.href.includes('challenge/totp'),
            { timeout: 25000 }
          );
        } catch (e) {
          await sleep(2500);
        }

        // Give the next screen (OAuth consent / Chromium overlay) time to fully render
        await sleep(700);

        // Step 5e: Dismiss "Sign in to Chromium?" overlay if present
        const overlayDismissed = await clickInPage(page, () => {
          const btns = [...document.querySelectorAll('button')];
          const btn = btns.find(b => {
            const t = (b.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            return t.includes('use chromium without') || t.includes('continue without') || t.includes('no thanks');
          });
          if (btn) { btn.click(); return true; }
          return false;
        }).catch(() => false);
        if (overlayDismissed) {
          console.log('  Step 5e: Dismissed "Sign in to Chromium" overlay');
          await sleep(500);
        }

        // Step 5f: Click "Continue" on OAuth consent page (evaluate only — no ElementHandle crossing worlds)
        const oauthContinued = await clickInPage(page, () => {
          const nodes = [...document.querySelectorAll('button, a[role="button"]')];
          const btn = nodes.find(b => {
            const t = (b.textContent || '').replace(/\s+/g, ' ').trim().toLowerCase();
            return t === 'continue' || t.startsWith('continue as') || /^(next|allow|yes)$/i.test(t);
          });
          if (btn) { btn.click(); return true; }
          return false;
        }).catch(() => false);
        if (oauthContinued) {
          console.log('  Step 5f: Clicked Continue on OAuth consent page');
          await sleep(2000);
          // Leave the loop — re-entry sees stale handles during redirect and triggers DOM.resolveNode.
          break;
        }

        continue; // loop one more time to catch any remaining screens
      }

      const chromiumDismissed = await clickInPage(page, () => {
        const btns = [...document.querySelectorAll('button')];
        const btn = btns.find(b => /use chromium without|continue without|no thanks/i.test(b.textContent || ''));
        if (btn) { btn.click(); return true; }
        return false;
      }).catch(() => false);
      if (chromiumDismissed) {
        console.log('  Step 5e: Dismissed "Sign in to Chromium" overlay...');
        await sleep(500);
        continue;
      }

      const continueClicked = await clickInPage(page, () => {
        const nodes = [...document.querySelectorAll('button, input[type="submit"]')];
        const btn = nodes.find(b => /continue|next|yes|allow/i.test(b.textContent || b.value || ''));
        if (btn) { btn.click(); return true; }
        return false;
      }).catch(() => false);
      if (continueClicked) {
        console.log('  Step 5d: Clicking Continue/Next button...');
        continue;
      }

      // No known challenge found — may have redirected already
      break;
    }

    // Step 5g: OAuth sometimes navigates to challenge/pwd AFTER the loop (late async redirect)
    if (page.url().includes('challenge/pwd')) {
      console.log('  Step 5g: Late challenge/pwd — re-entering password');
      await waitForManualCaptchaIfNeeded(page);
      await page.waitForSelector('input[name="Passwd"]', { visible: true, timeout: 60000 });
      await microWait();
      await page.focus('input[name="Passwd"]');
      await page.type('input[name="Passwd"]', GOOGLE_PASSWORD, { delay: 28 });
      await page.keyboard.press('Enter');
      await page.waitForNetworkIdle({ idleTime: 400, timeout: 15000 }).catch(() => sleep(900));
    }

    await clickInPage(page, () => {
      const btns = [...document.querySelectorAll('button')];
      const btn = btns.find(b => /use chromium without|continue without|no thanks/i.test(b.textContent || ''));
      if (btn) btn.click();
    }).catch(() => {});
    await microWait();

    // Step 6: Wait for redirect back to Unbxd console
    console.log('  Step 6: Waiting for redirect to console...');
    try {
      await page.waitForFunction(
        () => window.location.href.includes('unbxd.io') && !window.location.href.includes('accounts.google.com'),
        { timeout: 28000 }
      );
    } catch (e) {
      await sleep(1800);
    }

    let finalUrl = page.url();
    const isOnConsole = finalUrl.includes('unbxd.io') && !finalUrl.includes('accounts.google.com');
    if (!isOnConsole) {
      throw new Error('Login failed - did not reach console. Final URL: ' + finalUrl);
    }
    if (finalUrl.includes('admin-oauth-callback')) {
      try {
        await page.waitForNavigation({ waitUntil: 'load', timeout: 12000 });
        finalUrl = page.url();
      } catch (e) {
        await sleep(800);
        finalUrl = page.url();
      }
    }

    // Step 7: Navigate to the regional console URL to establish a regional session cookie.
    const CONSOLE_TARGET_URL = process.env.CONSOLE_TARGET_URL;
    if (CONSOLE_TARGET_URL) {
      console.log(`  Step 7: Navigating to regional console: ${CONSOLE_TARGET_URL}`);
      try {
        await page.goto(CONSOLE_TARGET_URL, { waitUntil: 'load', timeout: 45000 });
        await page.waitForNetworkIdle({ idleTime: 400, timeout: 6000 }).catch(() => sleep(500));
        console.log(`  Regional URL after navigation: ${page.url()}`);
      } catch (e) {
        console.log(`  Warning: regional navigation issue: ${e.message}`);
        await sleep(800);
      }
    }

    // Extract ALL cookies (includes regional session cookies set during Step 7)
    const cookies = await page.cookies();
    const cookieString = cookies.map(c => `${c.name}=${c.value}`).join('; ');
    const userId = `u_${Date.now()}`;

    console.log('✓ Console login successful');

    return {
      success: true,
      cookies: cookieString,
      rawCookies: cookies,
      userId,
      sessionId: null,
      browser,
      page,
    };
  } catch (error) {
    try { if (browser) await browser.close(); } catch (e) {}
    throw error;
  }
}
