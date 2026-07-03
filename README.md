# SinKey — Sinhala / English Keyboard

Android IME (Input Method Editor) app supporting Sinhala (phonetic/singlish
transliteration) and English typing, with light/dark/system theme support.

Built and signed 100% via GitHub Actions — no local Android build environment
required. Push to `main` or trigger the workflow manually to produce a debug
APK artifact; add release-signing secrets to produce a signed release APK.

## Project layout
- `app/src/main/kotlin/.../ime/` — the `InputMethodService` (keyboard engine)
- `app/src/main/kotlin/.../keyboard/` — key layouts + Sinhala transliteration
- `app/src/main/kotlin/.../ui/` — Compose UI: keyboard view + settings app
- `app/src/main/kotlin/.../data/` — DataStore-backed preferences (theme, language, sound, vibration)

## GitHub Actions secrets (optional, for signed release builds)
| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks`/`.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

Without these secrets, release builds fall back to debug signing so the
workflow still succeeds (useful for early testing).
