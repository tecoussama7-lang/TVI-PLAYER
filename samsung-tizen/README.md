# TVI Player — Samsung Tizen TV target

This folder is the Samsung Smart TV packaging target for the shared TV web client.

## Included
- `config.xml`: Tizen TV widget metadata, app identity, TV profile and required network/remote-input privileges.
- `index.html`, `styles.css`, `app.js`: self-contained Samsung TV web client assets (the JavaScript includes Samsung remote-key registration and D-pad focus navigation).

## Build a Samsung TV package
1. Install Samsung Tizen Studio and the TV extension for the target TV SDK.
2. Create/import a Tizen TV Web Application project.
3. Copy the contents of this directory into the project root.
4. In Tizen Studio, select the TV profile and sign the package using your own Samsung certificate profile.
5. Build a `.wgt` package and install it on a compatible Samsung TV/emulator for testing.

The repository does not contain a signing certificate or private key. The package cannot be signed for store distribution until the owner configures their certificate.

## Device compatibility
The manifest currently targets Tizen TV 5.5 or later. Older Samsung models may require a lower required_version and a separate compatibility test. TV model/year and firmware determine supported codecs and APIs.

## Known release blockers
- Test login and Xtream CORS behavior on actual Samsung TV models. If the provider blocks browser CORS, a secure backend proxy or provider-side CORS support is required.
- Validate HLS/TS/MP4 codecs, subtitles, audio tracks and seek behavior on each target model.
- Implement platform-specific DRM only if the licensed provider requires it.
- Add the final 117x117 PNG app icon and Samsung store metadata before submission.
- Never commit signing certificates, private keys, or provider credentials.
