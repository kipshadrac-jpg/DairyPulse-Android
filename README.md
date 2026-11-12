# DairyPulse

A dairy farm record and profit app for Android. Everything runs on the phone:
there is **no server and no account to create**.

## Features

| Tab | What it does |
|---|---|
| **Today** | Today's net profit (sales vs feed vs other costs), milk per session compared with the morning, milk/feed totals, head milked and dry cows. Log milking (per cow or as a bulk total) and feeding rounds. |
| **Feed** | Ingredient library (price per kg from pack size and price), ration recipes with automatic cost per kg, and recurring overheads (monthly costs are split across the days of each month). |
| **Herd** | Cows with status, days in milk and today's yield; red warning when a cow is due to calve within 21 days; calves and heifers with their rearing costs (feed vs upkeep). |
| **Schedule** | Upcoming calvings, dry-off reminders (≈60 days before calving) and your own tasks. A daily notification lists what is due. |
| **Money** | Profit & loss for Today / 7 days / 30 days, other income and expenses, and a PDF report you can share by Gmail, WhatsApp or save to Drive. |

## Where the data lives and how it is backed up

* All records are stored in a local database on the phone (Room / SQLite).
* **Android Auto Backup** copies the database and settings to the user's
  Google account (Google Drive app data, not visible in the Drive file list)
  roughly once a day when the phone is **idle, charging and on Wi-Fi**.
* When the same Google account is set up on a new phone, or the app is
  reinstalled, Android restores the records automatically.
* The user must have *Backup by Google One* / *Back up to Google Drive* turned on
  in the phone's settings. Backups are limited to 25 MB per app, which is
  years of farm records.

Backup rules: `app/src/main/res/xml/backup_rules.xml` (Android 6–11) and
`app/src/main/res/xml/data_extraction_rules.xml` (Android 12+).

### Testing the backup with adb

```sh
adb shell bmgr enable true
adb shell bmgr backupnow com.dairypulse.app      # force a backup now
adb uninstall com.dairypulse.app && adb install app-debug.apk  # data comes back
```

## Building

Requirements: JDK 17+, Android SDK 35.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # calculation tests + Robolectric UI walkthrough
```

Every push also builds the APK on GitHub Actions (**Actions → Build APK →
artifact `DairyPulse-debug-apk`**).

To publish on Google Play you will need a release signing key; see
<https://developer.android.com/studio/publish/app-signing>.

## Look and feel

Cream background, deep green accents, Fraunces serif headings and Manrope text,
rounded cards and figure tiles, slide-up form sheets and a compact bottom
navigation bar. Money shows as `KSh` by default (changeable in Settings).

Fonts are bundled in `app/src/main/res/font/` so the app works fully offline:
[Fraunces](https://github.com/undercasetype/Fraunces) and
[Manrope](https://github.com/sharanda/manrope), both under the SIL Open Font License 1.1.

## Tech

Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose, WorkManager.
Minimum Android 8.0 (API 26).

Code map:

* `data/` – database tables, queries, settings
* `domain/Calculations.kt` – profit & loss, ration costing, overhead day-splitting, calving dates (unit tested)
* `ui/` – one folder per tab
* `work/ReminderWorker.kt` – daily on-device reminder notification
