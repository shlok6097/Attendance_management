# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project identity: Android app "uvce_faculty" (Attendance management), single Gradle module: :app, Kotlin DSL.

Common commands (Windows PowerShell)

- Build debug/release
```powershell path=null start=null
./gradlew.bat assembleDebug
./gradlew.bat assembleRelease
```
- Install/run on device or emulator
```powershell path=null start=null
./gradlew.bat installDebug
# Explicitly start an Activity (two LAUNCHER activities are declared)
adb shell am start -n com.example.uvce_faculty/.Splash_screen
# or
adb shell am start -n com.example.uvce_faculty/.MainActivity
```
- Clean and list tasks
```powershell path=null start=null
./gradlew.bat clean
./gradlew.bat tasks --all
```
- Lint (Android Lint)
```powershell path=null start=null
./gradlew.bat :app:lintDebug
# Reports: app/build/reports/lint/lint-report.html
```
- Unit tests and a single test
```powershell path=null start=null
# All unit tests for debug
./gradlew.bat :app:testDebugUnitTest

# Single test (class or method)
# Replace with your package/class/method
./gradlew.bat :app:testDebugUnitTest --tests "com.example.uvce_faculty.SomeTestClass"
./gradlew.bat :app:testDebugUnitTest --tests "com.example.uvce_faculty.SomeTestClass.someTestMethod"
```
- Instrumented tests (requires a connected device/emulator)
```powershell path=null start=null
./gradlew.bat :app:connectedDebugAndroidTest
```

Architecture and code map

- Modules
  - :app is the only module (settings.gradle.kts includes only ":app").
- UI layer
  - Activity/Fragment-based UI with ViewBinding enabled (no Jetpack Compose).
  - MainActivity hosts a NavHostFragment and BottomNavigationView, wiring actions either via Navigation Component destinations or manual fragment transactions.
  - Key screens include Splash_screen, MainActivity, GetIn, LogIn, SignUp, plus feature fragments like AttendanceSessionFragment and AttendanceSheetFragment.
  - Multiple RecyclerView adapters (e.g., AttendanceBookAdapter, StudentAdapter, AttendanceSheetAdapter) render lists of domain models (Student, AttendanceBook, AttendanceRow).
- Data and services
  - Firebase is the primary backend. Dependencies include Auth, Firestore, Realtime Database, and Storage; Firestore is used extensively in the attendance flows.
  - OkHttp and Gson are available; an ApiConfig.kt exposes a BASE_URL for optional Cloud Functions integration. When BASE_URL is empty, Firestore is used as the fallback.
- Attendance domain model (as implemented in AttendanceSessionFragment.kt)
  - Collections and documents (Firestore):
    - attendanceBooks/{bookId}
      - sessions/{sessionId}
    - students/{studentId}
    - attendanceRecords/{bookId_sessionId_studentId}
  - Session lifecycle
    - createSession() creates a session document under attendanceBooks/{bookId}/sessions with startTime and active=true.
    - observeAttendanceRecords() listens on attendanceRecords filtered by bookId and sessionId and updates UI counts.
    - markAttendance(studentId, status, mode) writes/merges a record in attendanceRecords keyed by bookId_sessionId_studentId, guarded against duplicate concurrent updates.
    - endSession() updates the session doc with active=false, endTime, and totalPresent.
  - Modes
    - Manual vs Auto: toggling Auto generates a short session code persisted in the session doc with a server timestamp and expiry.

Build system and tooling

- Gradle wrapper present; Kotlin DSL with a version catalog (libs.* aliases).
- Android configuration (app/build.gradle.kts):
  - compileSdk=36, targetSdk=36, minSdk=24; Java/Kotlin set to 11 (sourceCompatibility/targetCompatibility/jvmTarget).
  - Google Services plugin is applied; Firebase BoM is used for dependency management.

Environment and setup notes

- JDK: Use Java 11 for builds to match jvmTarget and source/target compatibility.
- Android SDK: Ensure API level 36 is installed for compile/target SDKs.
- Firebase: Place a valid google-services.json in app/ to build with the Google Services plugin.
- Launching: Manifest declares two MAIN/LAUNCHER activities (Splash_screen and MainActivity). Use explicit component start commands (see above) if needed.

Key files

- settings.gradle.kts: defines rootProject.name and includes :app.
- build.gradle.kts (root): plugin aliases and Google Services classpath.
- app/build.gradle.kts: Android config, ViewBinding, Firebase/AndroidX deps, and apply com.google.gms.google-services.
- app/src/main/AndroidManifest.xml: app permissions and activity declarations.
