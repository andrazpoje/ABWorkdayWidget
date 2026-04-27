# AGENTS.md

## Project

This repository contains the Android app WorkCycle, formerly ABWorkdayWidget / AB Switch Widget.

Primary app purpose:
- Work cycle planning
- Android home screen widgets
- Work Log / work time tracking
- Calendar and weekly overview
- Secondary labels / statuses
- Slovenian and English localization

Repository:
https://github.com/andrazpoje/ABWorkdayWidget

## Technology

Use:
- Kotlin
- Android Studio Panda 2025+
- AndroidX
- Material 3 / Material Components
- XML layouts / Views
- Navigation Component
- AppWidget / RemoteViews
- SharedPreferences-based persistence

Do not introduce:
- Jetpack Compose
- Room
- Firebase
- major new architecture
- new production dependencies without explicit confirmation

## Packages and Play Store compatibility

Current code namespace may use:

com.dante.workcycle

Play Store compatibility may still require:

com.dante.abworkdaywidget

Do not rename package, namespace, or applicationId unless explicitly requested.

## Architecture

The app uses a single-activity + fragments architecture.

Important areas:
- Home: daily overview and weekly preview
- Calendar: monthly overview
- Settings: app configuration
- Work Log Dashboard: time tracking
- Widgets:
  - Work Cycle widget
  - Work Time / Work Log widget

Keep UI changes production-safe and avoid aggressive refactors.

## Coding rules

Prefer:
- minimal, focused patches
- file-by-file changes
- preserving existing SharedPreferences keys
- preserving existing resolver logic unless explicitly asked
- preserving existing widget update behavior
- localized strings in XML resources
- Material theme colors instead of hardcoded hex colors

Avoid:
- broad rewrites
- changing public data formats without migration
- changing app/widget refresh frequency by accident
- hardcoded user-facing strings in Kotlin
- deleting old code unless clearly safe
- mixing unrelated changes in one patch

## Localization

The app supports at least:
- Slovenian
- English

All user-facing text must be string resources.

Add strings to both:
- app/src/main/res/values/...
- app/src/main/res/values-sl/...

Do not hardcode dialog text, button text, labels, warnings, or helper text in Kotlin.

When logic depends on status meaning, use internal keys/semantics, not localized display text.

## Work Log safety rules

Work Log is sensitive because it can be used as work time evidence.

Manual edits must be audit-safe:
- do not silently overwrite important time data without trace
- record manual corrections
- record old value, new value, edited time, and event id/type when possible
- edits to future times must show an extra warning
- do not change Work Log persistence format aggressively without compatibility

Do not change:
- Work Log widget refresh/scheduler behavior
- AlarmManager behavior
- update interval preferences
- expected time resolver behavior

unless explicitly asked.

## Expected work time rules

Expected start/end fallback order:

1. Secondary label expected time
2. Primary cycle expected time
3. Global default work rules

Off days should not show global default expected time unless there is an explicit working label/time.

## Start Work warning rules

Starting work should warn only for true non-working/exclusive states:
- Off day
- Vacation
- Sick leave

Do not warn for normal working secondary labels/statuses:
- Field work / Teren
- Standby / Dežurstvo
- Š1 / O1 / K1
- custom working labels

Business logic should use StatusSemantics, not UI-only status visual helpers.

## Widgets

Widgets use RemoteViews.

Do not use Compose in widgets.

Preserve:
- widget refresh logic
- live vs battery-saving Work Log widget mode
- minimal/classic widget style behavior
- AppWidgetProvider update behavior

When changing widget UI:
- keep compact sizes in mind
- use short strings
- avoid complex unsupported layouts
- test minimal and classic styles

## Edge-to-edge / Android 15

The app targets modern Android versions and must handle edge-to-edge safely.

Do not use deprecated system bar APIs in app code:
- Window.setStatusBarColor
- Window.setNavigationBarColor
- window.statusBarColor
- window.navigationBarColor

Prefer AndroidX edge-to-edge and inset handling.

If warnings come from Material Components internals, prefer dependency update over hacks.

Always preserve toolbar, bottom navigation, bottom sheet, and fragment inset behavior.

## UI / UX style

Use a premium Material 3 style:
- clear hierarchy
- card-based layout
- good spacing
- readable typography
- light/dark mode support
- compact but not cramped widgets
- no debug-looking UI

Home should be operational:
- today status
- weekly preview
- quick summary

Settings should contain configuration:
- cycle settings
- rules
- templates
- colors
- Work Log settings
- widget settings

## Release workflow

Before release:
- update versionCode/versionName
- update CHANGELOG.md
- update What’s New strings
- build signed AAB
- upload to Play Store
- commit
- tag release as vX.X
- push commits and tags

## Build and validation

Prefer Android Studio:
- Build > Make Project
- Code > Inspect Code

If terminal build is available:
- gradlew.bat assembleDebug on Windows
- ./gradlew assembleDebug on Unix/macOS

If the environment cannot run Gradle or Kotlin compiler, still make static code edits but clearly report that build was not verified.

After every patch, report:
- changed files
- what changed
- whether business logic changed
- new/changed string resources
- manual test checklist
- risks