# TVI Player — Smart TV web foundation

This is the first cross-platform web client foundation for TV-class browsers (targeting Samsung Tizen, LG webOS and supported VIDAA web-app environments). It is intentionally separate from the existing native Android application so Android playback and Kids Mode are not disturbed.

## Run locally

Serve this directory over HTTP(S), rather than opening `index.html` as a file. For example:

```sh
python3 -m http.server 8080
```

Then open the host from a browser on the same network. The client currently supports Xtream-compatible login, Live/Movies/Series category retrieval, per-category in-memory caching, poster grids, remote/keyboard focus basics and direct video playback for Live and Movies.

## Current limitations — not store-ready

- This is a foundation, not a release. It has not been tested on a physical Samsung, LG, VIDAA, Apple TV or Roku device.
- Some TV browser engines block cross-origin requests (CORS) or have codec limitations. A production release needs platform-specific playback adapters and device testing.
- Series browsing currently lists series; episode detail/playback via `get_series_info` is not implemented yet.
- Credentials are not persisted; only server URL and username are stored locally. Use only trusted providers and HTTPS where available.
- The client does not contain or proxy any media content.
- Samsung Tizen and LG webOS require their own package metadata, signing and store submission. VIDAA access and distribution depend on vendor approval. Apple tvOS and Roku require separate native implementations; this web client is not a universal package for those systems.

## Next milestones

1. Add series detail and episode selection.
2. Add bounded/persistent category caching and resilient refresh.
3. Add platform wrappers, remote-key mapping and platform-specific playback.
4. Test on device/emulators, then prepare each vendor submission.
