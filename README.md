# Arena AI - Android WebView Wrapper

A lightweight, open-source Android app that wraps [arena.ai](https://arena.ai/) in a WebView, providing a native app experience for comparing AI models side-by-side.

## Features

- 🌐 **Full arena.ai experience** in a native Android app
- 🔒 **Encrypted session storage** — login once, stay logged in
- 🎤 **Voice input support** with just-in-time permissions
- 📁 **File upload support** for conversations
- 📸 **Camera access** for photo uploads
- 🔐 **OAuth login** (Google, Apple, Microsoft) handled in-app
- 🌙 **Dark theme** matching arena.ai's UI
- ⚡ **Lightweight** — under 3 MB APK size
- 📱 **Backwards compatible** — Android 5.0+ (API 21)

## Installation

### From Release APK

1. Download the latest `.apk` from the [Releases](https://github.com/your-org/arena-ai-app/releases) page
2. Install on your Android device
3. Open "Arena AI" from your app drawer

### Build from Source

**Requirements:** Android Studio with SDK 35 installed

```bash
git clone https://github.com/your-org/arena-ai-app.git
cd arena-ai-app
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

This app requests permissions **only when needed**:

| Permission | When Requested |
|---|---|
| Internet | Automatic (required) |
| Camera | When you tap photo upload |
| Microphone | When you use voice input |
| Storage/Media | When you upload a file |

## Architecture

```
com.arenaproject.ai.app/
├── ArenaApplication.java       — App-level cookie init
├── MainActivity.java           — WebView host + permission coordination
├── ArenaWebViewClient.java     — URL filtering, error handling
├── ArenaWebChromeClient.java   — File upload, media permissions
├── PermissionHelper.java       — Runtime permission utilities
└── CookieHelper.java           — Encrypted cookie persistence
```

**Dependencies:** AndroidX AppCompat, Core SplashScreen, Security Crypto, WebKit

## Security

- All cookies encrypted via `EncryptedSharedPreferences` (Android Keystore)
- HTTPS enforced — mixed content blocked
- SSL errors always rejected (never `handler.proceed()`)
- External URLs opened in system browser

## License

Apache License 2.0 — see [LICENSE](LICENSE)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

**Disclaimer:** This app is an independent wrapper and is not affiliated with or endorsed by arena.ai or LMSYS.
