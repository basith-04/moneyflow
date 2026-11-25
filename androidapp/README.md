# MoneyFlow for Android

MoneyFlow is a native Android companion for the existing MoneyFlow web and macOS clients. The Android Studio project lives in this directory and uses the same accounts, transactions, and categories through the production HTTPS API.

## Implementation plan

1. Verify the installed SDK and the Gradle, Android Gradle Plugin, and JDK compatibility. Inspect the web API services, Express routes and SQL schema, and macOS client.
2. Build typed Retrofit integration, exact monetary and date models, and a Keystore-backed session. Exercise the wire contract with a mock server.
3. Build the adaptive Compose workspace, transaction editors, categories, dashboard, authentication, and settings. Refresh API data after writes and when returning to the app.
4. Run JVM tests, Android lint, and signed debug packaging. Verify package metadata and the APK signature before device testing.

## Production API integration

API root is `https://moneyflow-czo7.onrender.com/`. The public website is `https://moneyflow-two-khaki.vercel.app/`.

| Method and path | Request | Response |
| --- | --- | --- |
| GET `/alive` | None | `status` |
| POST `/auth/login` | `userName`, `password` | Five-day JWT in `token` |
| POST `/auth/register` | `fullName`, `userName`, `email`, `password` | `message`, `user_id` |
| GET `/expenses` | Optional `min`, `max`, `category_id`, `payment_mode`, `from`, `to` | Array of `expense_id`, `title`, `amount`, `date`, `name`, `category_id`, `note`, `payment_mode` |
| POST `/expenses` | `title`, `amount`, `date`, `category_id`, optional `note`, optional `payment_mode` | `message`, `expense_id` |
| PATCH `/expenses/{id}` | `title`, `amount`, `date`, `category_id`, nullable `note` | `message` |
| DELETE `/expenses/{id}` | Path ID | `message` |
| GET `/income` | Optional `min`, `max`, `category_id`, `credited_to`, `from`, `to` | Array of `income_id`, `amount`, `date`, `name`, `category_id`, `credited_to`, `note` |
| POST `/income` | `amount`, `date`, `category_id`, `credited_to`, optional `note` | `message`, `income_id` |
| PATCH `/income/{id}` | `amount`, `date`, `category_id`, `credited_to`, nullable `note` | `message` |
| DELETE `/income/{id}` | Path ID | `message` |
| GET `/category` | Use `?type=income` for income categories | Array containing `id` and `name` |
| POST `/category` | `name`, with the same type query | `message`, `category_id` |
| DELETE `/category/{id}` | Path ID, with the same type query | `message` |

Protected requests send `Authorization: Bearer <JWT>`. Express verifies the signature and scopes data access by user ID. The app loads complete lists, then applies inclusive date and amount filters, search, and sorting locally. The backend sorts lists by date and ID descending. Dashboard metrics and charts aggregate these API records locally. Net cash flow is recorded income minus expenses for the selected range.

Expense payment values are `Credit Card`, `upi`, and `cash`. Android requires a payment method for new expenses, consistent with the web and macOS forms. Missing or historical values remain readable. The expense API also accepts an optional `group_id` write field, which the existing editors do not expose. Income is credited to `Bank Account` or `Cash`. Dates use Gregorian `YYYY-MM-DD`, with the device's local date used for “today” and UTC conversion only at the date picker boundary. Amounts use decimal arithmetic and display in INR with Indian grouping.

## Architecture and security

The single app module uses Kotlin, Jetpack Compose, Material 3 Expressive, and Jetpack Navigation. A ViewModel exposes immutable UI state via StateFlow, and the activity collects it with lifecycle awareness. Screens emit actions to the ViewModel. A small application-level factory wires the Retrofit repository, Keystore session vault, and preferences without a dependency injection framework.

- `data/` contains the typed Retrofit API, Kotlinx Serialization adapters, repository, session vault, and DataStore preferences.
- `domain/` contains ledger models, validation, date filtering, and exact financial calculations using BigDecimal.
- `ui/` contains the ViewModel, navigation shell, design system, and screens. The layout uses a bottom bar on compact windows and a navigation rail when space permits. It supports edge-to-edge rendering, system insets, light and dark appearance, and Material You colours.
- The repository uses standard HTTPS certificate verification, 20-second connection and write timeouts, a 65-second read timeout, and a 90-second call limit. Redirects and automatic financial mutation retries are disabled. A write reports success after an API acknowledgement, then refreshes financial data. Account snapshots are refreshed on sign-in, pull-to-refresh, and foreground return after a short freshness window.
- The JWT and username are encrypted with AES-GCM using Android Keystore. The ciphertext uses the app's no-backup directory. A protected HTTP 401 clears the local session, as does sign-out. Password entry uses the platform autofill service and app memory for the current form. The app does not persist passwords or log API payloads. Internet is the only permission requested directly by the app.
- DataStore stores appearance, dynamic colour, and the last expense payment method. Financial records are held in memory during the session.

The dashboard uses compact financial typography and tonal surfaces. The shell uses familiar Android list and navigation patterns, with Calendar-style date selection and an amount-first expense sheet. The adaptive icon carries forward the chart mark from MoneyFlow for macOS. Design references include [Material 3](https://m3.material.io/), [Android mobile design](https://developer.android.com/design/ui/mobile), [Compose](https://developer.android.com/compose), [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps), and [Android architecture](https://developer.android.com/topic/architecture).

## Open in Android Studio

Open the **androidapp** directory as a Gradle project. Select Android Studio's bundled JDK under **Settings → Build, Execution, Deployment → Build Tools → Gradle** and let Studio sync using the included wrapper. The SDK location can be set in Android Studio or supplied through `ANDROID_HOME`.

- Compile and target SDK 37.0 (Android 17), Build Tools 36.0.0, minimum SDK 31 (Android 12).
- Verified on Apple Silicon with Android Studio 2026.2, its bundled JetBrains Runtime 25.0.4, Android Gradle Plugin 9.2.1, and Gradle 9.4.1. The app compiles Kotlin and Java to JVM 17 bytecode.
- The Compose BOM is pinned to the installed SDK 37.0 toolchain, with stable Material 3 Expressive 1.5.0 selected explicitly. The November 2026 Compose BOM requires SDK 37.1. [AGP 9.2 compatibility](https://developer.android.com/build/releases/agp-9-2-0-release-notes) · [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html) · [Compose November release](https://developer.android.com/blog/posts/jetpack-compose-november-2026-release)

## Build and test

From the repository root on the development Mac, run the following. The project-local Gradle and Android user directories keep caches and the generated debug keystore separate from the rest of the repository. Keep the same debug keystore to update an existing personal installation.

```bash
cd androidapp
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export GRADLE_USER_HOME="$PWD/.gradle-user-home"
export ANDROID_USER_HOME="$PWD/.android"

./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is `androidapp/app/build/outputs/apk/debug/app-debug.apk` relative to the repository root. The application ID is `com.moneyflow.android`. Gradle generates the local debug signing key on first build. The v0.1.0 testing prerelease uses the existing debug key and distributes the APK as `MoneyFlow-v0.1.0.apk` through GitHub Releases.

JVM tests use a local MockWebServer and in-memory fakes for auth, network mapping, error handling, validation, state transitions, and refresh after writes. Test reports land in `app/build/reports/tests/testDebugUnitTest/` and lint in `app/build/reports/lint-results-debug.html`, relative to this project. Android Studio previews use fixtures from `app/src/debug/`.

## Install on a Samsung Galaxy S23

**USB with ADB**

1. Open **Settings → About phone → Software information** and tap **Build number** seven times. Turn on **USB debugging** under **Settings → Developer options**.
2. Connect a data-capable USB cable, unlock the phone, and approve the debugging prompt.
3. Run the following from the repository root, then open **MoneyFlow** and sign in to an existing account.

```bash
"$HOME/Library/Android/sdk/platform-tools/adb" devices
"$HOME/Library/Android/sdk/platform-tools/adb" install -r androidapp/app/build/outputs/apk/debug/app-debug.apk
```

**Install from the file**

Transfer the APK to the phone, open it in **My Files**, and allow installation from that source if Samsung prompts you. Samsung's Auto Blocker can block installation or USB commands. If it blocks your own APK, consult Samsung's instructions and re-enable it after installation. [Android ADB documentation](https://developer.android.com/tools/adb) · [Samsung Auto Blocker](https://www.samsung.com/us/support/answer/ANS10003636/)

An in-place update requires the same signing key as the installed build. If Android reports a signature mismatch, uninstall the earlier build before installing this one. Uninstalling clears the local session and preferences. Account records remain on the MoneyFlow server.

## Known limitations

- The API supports category creation and deletion. It has no category rename endpoint. Referenced categories are protected by the backend. Expense payment methods are read-only after creation. Income credited-to values can be edited.
- There is no pagination, token refresh, token revocation, password recovery, or profile endpoint. Account settings show the username used to sign in. Search, sorting, and summaries operate on the API's full lists.
- The trend chart shows full monthly history for the six months ending at the selected range's end month, capped at the current month. Summary totals and recent transactions use the precise selected range.
- The backend source has existing request-body logging in the registration and expense-create handlers. Android adds no such logging. The backend and deployment configuration are outside this project's scope.
- A phone or emulator was unavailable for runtime testing. The first on-device pass should cover sign-in, keyboard and autofill, creating and editing records with a test account, rotation and split screen, gesture and button navigation, TalkBack, and the launcher shortcut.
