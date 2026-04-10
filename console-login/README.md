# console-login

Login to **console.unbxd.io** (Google sign-in + optional 2FA TOTP). This folder is used by the Java framework to obtain session cookies for Selenium tests.

## 1. Setup

Copy `.env.example` to `.env` and set:

- `GOOGLE_EMAIL` - Google account for Unbxd Console
- `GOOGLE_PASSWORD` - Password
- `TOTP_SECRET` - TOTP secret from your authenticator app

## 2. Install dependencies

```bash
cd console-login
npm install
```

## 3. Run

```bash
cd console-login
npm run export
```

Outputs JSON to stdout:
`{ success, cookies, userId, sessionId, cookieList }`

If Google shows a CAPTCHA, use `HEADLESS=false` and optionally `CAPTCHA_MANUAL_WAIT_MS` (default 2 minutes). The script waits for you to solve it and only types the account password into `input[name="Passwd"]`, not into CAPTCHA fields.
