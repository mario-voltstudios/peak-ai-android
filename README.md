# Peak AI Fitness — Android App

**Forge Mountain aesthetic. On-device AI. Zero cloud.**

Dark background (`#09090b`) + amber accents (`#f59e0b`). Built for Pixel 8+ and Samsung S24+ with Gemini Nano + StrongBox.

---

## Features

| Feature | Status |
|---------|--------|
| Readiness Score (1-10) | ✅ Full algorithm |
| Health Connect: HRV, RHR, Sleep, SpO₂, Steps | ✅ |
| Gemini Nano on-device coaching | ✅ With reflection-based SDK integration |
| Rule-based fallback coach | ✅ Full rules engine |
| Morning briefing with 3 action items | ✅ |
| Check-in notifications (4h interval) | ✅ WorkManager |
| Ask Coach conversational UI | ✅ |
| Water / Caffeine / Supplement / Med logging | ✅ |
| StrongBox Keymaster encryption | ✅ With TEE fallback |
| Local-only data (no cloud) | ✅ |
| Material 3 dark theme | ✅ |

---

## Build Instructions

### Prerequisites

1. **Android Studio Ladybug (2024.2.1)** or newer
2. **JDK 17+**
3. **Android SDK** — API 34 + API 35 installed

### Quick Start

```bash
# Clone the repo
git clone https://github.com/mario-voltstudios/peak-ai-android
cd peak-ai-android

# Build debug APK
./gradlew assembleDebug

# APK location
ls app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB

```bash
# With phone connected via USB (USB debugging enabled)
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Sideload via Android

1. Transfer APK to phone (AirDrop → Android, email to self, or USB)
2. Open Files → navigate to APK → tap to install
3. Allow "Install unknown apps" when prompted

---

## Device Requirements

- **Android 14+ (API 34)** minimum
- **Health Connect** app installed (pre-installed on Pixel 8+, available on Play Store)
- **Gemini Nano**: Pixel 8/8 Pro/8a/9 or Samsung Galaxy S24 series
  - If Gemini Nano not available, rule-based coach activates automatically

---

## Architecture

```
app/
├── coaching/
│   ├── GeminiNanoCoach.kt      ← On-device Gemini Nano via AI Edge SDK
│   └── RuleBasedCoach.kt       ← Deterministic fallback
├── data/
│   ├── health/HealthConnectManager.kt  ← All HC reads
│   ├── local/                  ← Room DB: log entries + coaching messages
│   └── repository/HealthRepository.kt ← Single source of truth
├── domain/
│   ├── model/                  ← BiometricSnapshot, ReadinessScore, LogEntry
│   └── usecase/ComputeReadinessUseCase.kt ← Scoring algorithm
├── notifications/
│   ├── workers/                ← WorkManager: morning briefing + check-ins
│   └── PeakNotificationManager.kt
├── security/
│   └── StrongBoxKeyManager.kt  ← AES-256-GCM, StrongBox-backed
└── ui/
    ├── screens/                ← Dashboard, Coach, Log (Compose)
    ├── theme/                  ← Forge Mountain dark theme
    └── viewmodel/              ← Hilt ViewModels
```

---

## Readiness Algorithm

```
Score (1–10) = composite × 9 + 1

composite = 
  HRV_score    × 0.40   (vs 7-day baseline)
  sleep_score  × 0.30   (duration + deep/REM quality)
  RHR_score    × 0.20   (vs 7-day baseline, lower = better)
  activity_score × 0.10 (steps vs daily average)
```

**Labels:**
- 9-10 → PEAK
- 7-8 → HIGH  
- 5-6 → MODERATE
- 3-4 → LOW
- 1-2 → RECOVERY

---

## Rule-Based Coaching Triggers

| Condition | Message |
|-----------|---------|
| HRV < 70% of baseline | "Recovery day. Light movement only." |
| Sleep < 6h | "Sleep deficit. Delay caffeine 90min. Front-load protein." |
| No water logged by 10am | Push notification: hydration reminder |
| Caffeine > 300mg | "Caffeine cap reached. Switch to water." |
| RHR > 110% baseline | "Elevated resting HR. Possible illness or stress." |

---

## Gemini Nano Setup

The app uses reflection to integrate Gemini Nano (Google AI Edge SDK) to avoid hard compile-time dependency while the SDK matures.

When `com.google.ai.edge.aicore.GenerativeModel` is available on device, it's used automatically. Otherwise, rule-based coach activates.

To add full SDK dependency when GA:
```kotlin
// In app/build.gradle.kts
implementation("com.google.ai.edge.aicore:aicore:0.0.1-exp01")
```

---

## Privacy & Security

- ❌ No internet permission declared
- ❌ No analytics or tracking
- ✅ All health data stays on device
- ✅ Room DB encrypted with AES-256-GCM key
- ✅ Key stored in Android Keystore (StrongBox-backed on Pixel 8+)
- ✅ Cloud backup disabled for health DB
- ✅ UUID-only identifier if future cloud needed

---

## Testing

### Health Connect Mock Data
For testing without a real Wear OS device, inject mock biometrics:

```kotlin
// In DashboardViewModel or a test, inject BiometricSnapshot directly
val testSnapshot = BiometricSnapshot(
    hrv = HrvData(sdnn = 55.0, timestamp = Instant.now()),
    restingHeartRate = 58,
    sleep = SleepData(
        durationMinutes = 450, // 7.5h
        stages = listOf(
            SleepStage(SleepStageType.DEEP, 90),
            SleepStage(SleepStageType.REM, 100)
        ),
        startTime = Instant.now().minusSeconds(8 * 3600),
        endTime = Instant.now()
    ),
    spo2 = 98.5,
    steps = 4200
)
```

### Readiness Scoring Test Cases
```kotlin
// Perfect day: HRV at baseline, 8h sleep, normal RHR → score ≈ 9
// Recovery: HRV 60% baseline, 5h sleep, RHR +12% → score ≈ 2
// Moderate: HRV 85% baseline, 7h sleep → score ≈ 6
```

---

## Roadmap (Post-MVP)

- [ ] Wearable companion app (Wear OS)
- [ ] Workout session tracking
- [ ] HRV trend charts (7/30 day)
- [ ] Google Fit / Samsung Health Bridge
- [ ] Customizable supplement presets
- [ ] Export health report (PDF)
- [ ] Widget (1x1 readiness score on home screen)
