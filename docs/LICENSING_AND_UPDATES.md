# OATEC PlayerTv — Licensing and updates

This document describes the client scaffolding and the server contract required before commercial enforcement.

## Licensing model

The intended access rules are:

1. A first-time user receives one 7-day trial.
2. A valid OATEC Premium code grants app access only until that Premium subscription expires (monthly, 3-month, 6-month, or yearly).
3. A separate app Lifetime activation code grants permanent app access.
4. When the trial or Premium entitlement expires, the app must show the app activation screen. A Lifetime entitlement remains valid.
5. IPTV source credentials and app licensing are separate concepts. A Premium code must never be interpreted as a Lifetime app license.

### Client code currently present

- `AppEntitlementRepository` records the local first-run time and calculates a 7-day trial window.
- It reuses the stable `device_id` used by the existing Premium activation flow.
- This local timestamp is only a development/UI scaffold. Clearing app data, reinstalling, changing the device clock, or modifying the client can defeat it. Do not use it as the production source of truth.
- No app activation screen or access lock is enabled until the server contract below is implemented. This avoids accidentally blocking existing customers while the website is not ready.

## Required OATEC API contract

Base URL: `https://admin.oatec.store/api/player`

### POST `/entitlement/status`

Request:

```json
{
  "device_id": "stable-installation-id",
  "platform": "android",
  "app_version": "1.0.0"
}
```

Response:

```json
{
  "ok": true,
  "status": "trial",
  "plan": "trial",
  "server_time": "2026-10-31T12:00:00Z",
  "trial_started_at": "2026-10-31T12:00:00Z",
  "expires_at": "2026-11-07T12:00:00Z",
  "lifetime": false
}
```

Allowed status values: `trial`, `premium`, `lifetime`, `expired`, `revoked`.

### POST `/entitlement/activate`

Request:

```json
{
  "code": "APP-LIFETIME-CODE",
  "device_id": "stable-installation-id",
  "platform": "android",
  "app_version": "1.0.0"
}
```

A successful Lifetime activation returns `status: "lifetime"`, `lifetime: true`, and no expiry. A failed, expired, revoked, or device-conflicted code must return a suitable 4xx response and a machine-readable `error`.

### Premium entitlement

The existing `/activate-by-code` endpoint currently provisions an IPTV source. Extend the server response or provide a separate entitlement-status endpoint that reports the Premium subscription's actual expiry. Do not infer subscription expiry from the IPTV playlist or from a locally saved Premium code.

### Server-side requirements

- The server is the source of truth for trial start, expiry, payment, revocation, and device binding.
- Start the 7-day trial once per customer/device according to the business policy; do not trust a client-supplied timestamp.
- Store activation codes as secure hashes, make codes high entropy, and support revocation and audit logs.
- Lifetime and Premium code namespaces must be distinct.
- Verify payment server-side before issuing a Lifetime code.
- Return server time and entitlement expiry in UTC.
- Decide and document the device-transfer policy before enabling device binding.
- When offline, use a bounded signed entitlement cache with a documented grace period; do not grant indefinite access merely because the API is unreachable.

## App updates

The Android client checks the latest GitHub Release on app startup:

`https://api.github.com/repos/tecoussama7-lang/TVI-PLAYER/releases/latest`

To publish an update:

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build APKs for the supported ABIs.
3. Create a GitHub Release with a tag such as `v1.0.1`.
4. Attach the matching ABI APKs (names should include `arm64-v8a`, `armeabi-v7a`, `x86`, or `x86_64`). The app selects the APK matching the device ABI.
5. The app shows an update prompt on its next launch. The download opens in the browser and the user installs the APK manually.

### Signing requirement — before public rollout

Android only accepts an in-place update when the APK is signed with the same signing certificate as the installed app. The current CI release build uses the runner's debug signing configuration; GitHub-hosted runners do not guarantee a stable debug key. Therefore, releases are not yet safe for seamless customer upgrades.

Before publishing customer updates, configure a permanent private release keystore in GitHub Actions Secrets, configure Gradle release signing to use it, and install the first APK signed with that key as the customer baseline. Never commit the keystore or passwords. If an already-distributed APK was signed with a different key, it cannot be upgraded in place; customers would need a clean reinstall unless the original signing key is recovered.

The update checker is a basic release-notification/download flow. It does not silently install APKs and does not bypass Android's package-installer confirmation.
