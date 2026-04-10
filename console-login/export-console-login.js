/**
 * Export mode for console.unbxd.io login.
 * Outputs JSON to stdout for Java consumers.
 * Uses async run() instead of top-level await for Node 12 / older Jenkins agents.
 */
import { loginToConsole } from './console-login.js';

const originalLog = console.log;
const originalError = console.error;
const originalWarn = console.warn;

async function run() {
  console.log = (...a) => process.stderr.write('[login] ' + a.join(' ') + '\n');
  console.error = (...a) => process.stderr.write('[login:err] ' + a.join(' ') + '\n');
  console.warn = (...a) => process.stderr.write('[login:warn] ' + a.join(' ') + '\n');

  try {
    const result = await loginToConsole();

    console.log = originalLog;
    console.error = originalError;
    console.warn = originalWarn;

    if (!result.success) {
      process.stderr.write(JSON.stringify({ success: false, error: 'Login failed' }) + '\n');
      process.exit(1);
    }

    const cookieList = (result.rawCookies || []).map((c) => ({
      name: c.name,
      value: c.value,
      domain: c.domain || '.unbxd.io',
      path: c.path || '/',
      secure: !!c.secure,
      httpOnly: !!c.httpOnly,
      expires: typeof c.expires === 'number' ? c.expires : -1
    }));

    if (!cookieList.length) {
      process.stderr.write(
        JSON.stringify({
          success: false,
          error: 'Login finished but no browser cookies were captured (empty session).',
        }) + '\n'
      );
      if (result.browser) {
        await result.browser.close().catch(() => {});
      }
      process.exit(1);
    }

    process.stdout.write(JSON.stringify({
      success: true,
      cookies: result.cookies,
      userId: result.userId,
      sessionId: result.sessionId,
      cookieList
    }) + '\n');

    if (result.browser) {
      await result.browser.close();
    }
    process.exit(0);
  } catch (err) {
    console.log = originalLog;
    console.error = originalError;
    console.warn = originalWarn;
    process.stderr.write(JSON.stringify({ success: false, error: err.message }) + '\n');
    process.exit(1);
  }
}

run().catch((err) => {
  console.log = originalLog;
  console.error = originalError;
  console.warn = originalWarn;
  process.stderr.write(JSON.stringify({ success: false, error: err.message }) + '\n');
  process.exit(1);
});
