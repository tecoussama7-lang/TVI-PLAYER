# TVI Player — Samsung Tizen TV

This package targets Samsung Smart TVs running Tizen. It does not target older Samsung Orsay platforms.

## Compatibility approach
- Manifest baseline set to Tizen 2.3 as a broad legacy baseline for Samsung Tizen TVs.
- Application JavaScript is written in ES5 syntax and uses XMLHttpRequest instead of fetch, URL and URLSearchParams.
- The interface includes non-Grid layout fallbacks and D-pad/Back remote navigation.
- Features unsupported by a TV browser may still vary by model and firmware.

## Build
1. Install Samsung Tizen Studio and the Samsung TV extension.
2. Import this folder as a Tizen TV Web Application.
3. Select the TV profile and sign using your own Samsung certificate profile.
4. Build a .wgt package and test on representative Tizen TV generations.

## Important limitations
- The source bundle is not a signed .wgt package.
- No physical Samsung TV or emulator validation has yet been completed.
- Xtream server CORS policy, video codecs, HLS/TS support, subtitles, audio tracks and seeking must be tested on target devices.
- If the provider blocks browser requests, a secure server-side proxy or provider CORS support is needed.
- Tizen 2.3 is a manifest compatibility baseline, not a guarantee that every API, codec or Samsung model behaves identically.
- Pre-Tizen Samsung Orsay TVs require a separate legacy application and are outside this target.
- Never commit signing certificates, private keys or provider credentials.
