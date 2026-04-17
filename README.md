# Safe Exam Browser — Android

> Aplikasi CBT (Computer-Based Test) Android dengan sistem anti-cheat berlapis dan CI/CD pipeline via GitHub Actions.

---

## 📋 Fitur

| Fitur | Deskripsi |
|---|---|
| Daftar Ujian | Daftar Google Form tersimpan secara persisten |
| Input URL Manual | Validasi URL Google Form |
| QR Scanner | CameraX + ML Kit → auto-isi URL |
| Secure WebView | Navigasi terbatas, JS aktif, tanpa akses file |
| Kiosk Mode | Lock Task Mode, blokir tombol hardware |
| Anti-Cheat Suite | Sistem deteksi pelanggaran 10 lapis |
| Violation Counter | Badge live `n/3` dengan auto-lock di batas |
| Security Gate | Cek root, dev-mode, VPN sebelum ujian dimulai |
| Room Database | Penyimpanan offline untuk ujian & pelanggaran |

---

## 🛡️ Lapisan Anti-Cheat

| # | Lapisan | Implementasi |
|---|---|---|
| A | Kiosk Mode | `ActivityManager.startLockTask()` + `DevicePolicyManager` |
| B | Screen Security | `WindowManager.FLAG_SECURE` — blokir screenshot & recording |
| C | App-Switch Detection | Hook lifecycle `onResume` mencatat pelanggaran saat kembali |
| D | Split-Screen Detection | Cek `Activity.isInMultiWindowMode()` |
| E | Overlay Detection | Listener perubahan fokus window |
| F | Developer Mode | Cek `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` |
| G | Root Detection | Binary `su` + path root umum + cek mount Magisk |
| H | VPN Detection | `NetworkCapabilities.TRANSPORT_VPN` |
| I | Clipboard Monitoring | `ClipboardManager.OnPrimaryClipChangedListener` |
| J | Violation System | Counter dengan batas `MAX_VIOLATIONS = 3` yang bisa diatur |

---

## 🏗️ Arsitektur

```
SafeExamBrowser/
├── data/
│   ├── db/
│   │   ├── dao/          ExamDao, ViolationDao
│   │   ├── entity/       Exam, Violation (Room)
│   │   └── AppDatabase
│   └── repository/       ExamRepository, ViolationRepository
├── di/                   Hilt AppModule
├── security/
│   ├── AntiCheatManager  Koordinator utama
│   ├── RootDetector
│   ├── DevModeDetector
│   ├── VpnDetector
│   └── ExamDeviceAdminReceiver
└── ui/
    ├── MainActivity       NavHost
    ├── home/              HomeFragment + HomeViewModel + ExamListAdapter
    ├── addexam/           AddExamFragment + AddExamViewModel
    ├── qrscanner/         QrScannerFragment (CameraX + ML Kit)
    └── exam/              ExamActivity + ExamViewModel (kiosk/secure)
```

**Pattern:** MVVM + Repository + Hilt DI + Kotlin Coroutines + StateFlow/SharedFlow

---

## ⚙️ Tech Stack

| Layer | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Min SDK | 24 (Android 7.0) |
| Arsitektur | MVVM + Repository |
| DI | Hilt 2.50 |
| Database | Room 2.6 |
| Kamera | CameraX 1.3 |
| QR Scanning | ML Kit Barcode Scanning 17.2 |
| Navigasi | Jetpack Navigation 2.7 |
| Build | Gradle 8.4, JDK 17 |
| CI/CD | **GitHub Actions** (tanpa Android Studio) |

---

## 🚀 Build via GitHub Actions (Cara Utama)

Proyek ini di-build sepenuhnya di GitHub Actions — **tidak perlu Android Studio**.

### Cara Dapat APK

**Debug APK** — tersedia otomatis di setiap push ke `main`/`develop`:
1. Buka tab **Actions** di GitHub
2. Klik workflow run terbaru → **Build & Unit Test**
3. Scroll ke bawah → **Artifacts** → download `debug-apk-*`

**Release APK** — otomatis saat push tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```
APK yang sudah ditandatangani akan muncul di tab **Releases** GitHub.

---

## 🔐 Setup Secrets GitHub (Untuk Release APK)

Sebelum bisa membuat release APK yang ditandatangani, tambahkan secrets berikut di:
**GitHub Repo → Settings → Secrets and variables → Actions**

| Secret | Deskripsi |
|---|---|
| `KEYSTORE_BASE64` | Keystore yang di-encode Base64 |
| `KEYSTORE_PASSWORD` | Password keystore |
| `KEY_ALIAS` | Alias key di dalam keystore |
| `KEY_PASSWORD` | Password key |

### Cara Buat Keystore & Encode

```bash
# 1. Buat keystore baru
keytool -genkey -v \
  -keystore release.keystore \
  -alias safeexam \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# 2. Encode ke Base64 untuk disimpan sebagai secret
base64 -w 0 release.keystore
# Salin output-nya → paste sebagai nilai KEYSTORE_BASE64

# ⚠️  JANGAN pernah commit file .keystore ke repository!
```

---

## 🔄 CI/CD Pipeline

### CI Workflow (`android-ci.yml`)

Trigger: push ke `develop`/`main` dan pull request ke `main`

| Job | Aksi |
|---|---|
| Build & Unit Test | Build debug APK + jalankan unit test |
| Lint | Android Lint check |
| Static Analysis | Detekt + ktlint |
| CI Pass | Gate: semua job harus sukses |

### CD Workflow (`android-cd.yml`)

Trigger: push ke `main` atau tag `v*`

| Job | Trigger | Aksi |
|---|---|---|
| Build & Sign Release | push `main` / tag | Build release APK+AAB, sign dengan `apksigner` |
| Create GitHub Release | tag `v*` saja | Upload APK+AAB ke GitHub Releases |

---

## 🖥️ Build Lokal (Opsional)

Jika ingin build secara lokal tanpa Android Studio, cukup gunakan:

```bash
# Persyaratan: JDK 17 & Android SDK terinstall
git clone https://github.com/your-username/SafeExamBrowser.git
cd SafeExamBrowser

# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Jalankan unit test
./gradlew testDebugUnitTest

# Lint
./gradlew lint

# Detekt
./gradlew detekt
```

---

## 🔐 Kiosk Mode (Device Owner)

Untuk kiosk mode **penuh** (menekan status bar, mencegah pergantian app):

1. **Factory reset** perangkat (diperlukan untuk provisioning Device Owner).
2. Set Device Owner via ADB:
   ```bash
   adb shell dpm set-device-owner com.safeexam.browser/.security.ExamDeviceAdminReceiver
   ```
3. App akan memanggil `dpm.setLockTaskPackages()` + `startLockTask()`.

Tanpa Device Owner, app fallback ke screen-pinning (user harus approve sekali).

---

## 📝 Konfigurasi

### Batas Pelanggaran

Di `AntiCheatManager.kt`:

```kotlin
companion object {
    const val MAX_VIOLATIONS = 3  // Ubah sesuai kebutuhan
}
```

### Domain yang Diizinkan di WebView

Di `ExamActivity.kt` → `setupWebView()`:

```kotlin
return if (requestUrl.contains("docs.google.com") ||
           requestUrl.contains("accounts.google.com")) {
    // Tambahkan domain lain di sini jika perlu
}
```

---

## ⚠️ Catatan Penting

- **Anti-cheat bersifat best-effort** — model keamanan Android OS membatasi apa yang bisa di-enforce oleh app.
- **FLAG_SECURE** efektif mencegah sebagian besar upaya screenshot/recording.
- **Full kiosk mode** memerlukan app diset sebagai Device Owner (MDM/enterprise).
- **Root detection** mencakup metode umum, namun pengguna lanjutan mungkin bisa bypass.
- Jangan pernah commit keystore atau credential ke source control.

---

## 📄 Lisensi

MIT License — lihat [LICENSE](LICENSE).
