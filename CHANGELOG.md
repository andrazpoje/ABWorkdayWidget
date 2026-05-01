# Changelog

All notable changes to this project will be documented in this file.

## v3.0

### Added
- Added `WorkLogSessionStateResolver` as the shared Work Log state foundation for ordering, status, active break, meal state, worked minutes, and action flags
- Added Work Log accounting foundation with `WorkLogAccountingCalculator`, `WorkLogAccountingRulesFactory`, and `WorkSettingsPrefs.toAccountingRules()`
- Added Break Accounting Mode in Work Log Settings:
  - unpaid break
  - fully paid break
  - paid allowance
  - custom employer policy
- Added conditional “Credited time / Priznan čas” display on the Work Log dashboard
- Added active break dashboard labels for elapsed, remaining, and exceeded break states
- Added `WorkLogWidgetBalanceCalculator` for pure Kotlin widget balance parity coverage
- Added `WorkLogCsvExporter` and first Work Log CSV export flow in Work Log Settings
- Added date range support for Work Log CSV export
- Added local JVM tests for Work Log resolver, accounting calculator, accounting rules factory, widget balance calculator, and CSV exporter
- Added Work Log CSV export with raw Work Log events and manual edit audit metadata
- Added full local ZIP backup export for WorkCycle data and settings
- Added full backup export in the main Settings backup section
- Added backup manifest, payload, writer, Room JSON mapper, SharedPreferences JSON mapper, and backup collector foundation
- Added backup validation and preview foundation with ZIP preflight diagnostics
- Added Premium / FeatureGate foundation with central premium feature tier models and gate decisions


### Improved
- Dashboard balance now uses the Work Log accounting rules
- Work Time widget balance now matches dashboard accounting behavior
- Work Time widget now refreshes immediately after relevant Work Log Settings changes:
  - Break Accounting Mode
  - daily target
  - default break minutes
  - widget info mode
- Work Log dashboard now distinguishes effective work from credited time
- Improved active break display so the value shows only duration while the label carries elapsed / remaining / exceeded meaning
- Improved Work Log Help and Settings wording around break accounting, meal time, effective work, and balance
- Work Log CSV export now offers Export all and Export date range from one entry point
- Backup export is now located in the main Settings Backup section instead of Work Log Settings
- Added explicit backup filtering so debug, transient, and session snapshot state is excluded from exported backups


### Technical
- Dashboard and Work Time widget now share resolver-based status handling
- Dashboard and Work Time widget now share the same accounting layer for balance/saldo
- Work Log CSV export currently writes raw `WorkEvent` data plus manual edit audit metadata without derived accounting columns
- Added read-only date-range query support for Work Log CSV export
- Kept `WorkLogCsvExporter` unchanged and reused it with filtered event lists
- Room schema and migrations were not changed in this v3.0 development work
- Added export-only backup foundation without restore/import behavior
- Added backup ZIP structure with `manifest.json`, `room/work_events.json`, `room/work_logs.json`, and `prefs/*.json`
- Added pure Kotlin backup ZIP validator, ZIP reader, JSON parser, and preview/summary model
- Added structured backup validation result with warnings/errors separation
- Added central entitlement / gating API with `FeatureTier`, `PremiumFeature`, `GateDecision`, `EntitlementRepository`, and `FeatureGate`
- Added entitlement repository chain foundation with `OverrideEntitlementRepository` and `CompositeEntitlementRepository`
- Added local JVM tests for premium feature tier mapping and default locked entitlement behavior
- Added local JVM tests for entitlement override and repository priority-chain behavior
- Kept Room schema and migrations unchanged
- Play Billing is not included yet
- SharedPreferences entitlement storage is not included yet
- UI gating is not included yet
- Existing backup/export functionality is not gated or locked yet
- Existing runtime behavior is unchanged
- Restore/import and Premium gating are not included yet

---

## v2.9

### ✨ Added

- Added debug-only Developer Tools behind a hidden Settings easter egg for faster development testing
- Added debug-only onboarding reset tool for testing first-run setup without emulator wipe data
- Added debug-only local data reset tool for clearing preferences and Work Log test data during development
- Added dedicated legacy settings migration helper to protect users upgrading from older versions
- Added architecture documentation with KDoc for schedule, template, status, Work Log, widgets, onboarding, and settings components
- Added roadmap notes for future Room schema export, migration testing, backup/export, work profiles, and multiple Work Log sessions

### 🎨 Improved

- Improved first-run onboarding text and layout clarity
- Improved onboarding toolbar title to use Setup / Nastavitev instead of repeating Welcome to WorkCycle
- Improved onboarding step cards by removing duplicated Step X of Y text
- Improved onboarding CTA wording with Next / Naprej and shorter final Slovenian action Začni
- Improved locked-template onboarding explanation
- Improved final onboarding summary with reminder that settings can be changed later
- Improved startup routing so onboarding is always checked before What’s New
- Improved Home/Teden weekly preview to read persisted settings and resolved schedule data instead of hidden configuration fields
- Improved Help text to match the current Week / Month / Settings structure
- Improved terminology for Work Cycle widget and Work Time / Delovni čas widget
- Improved Help wording to separate secondary labels from status labels

### 🧹 Cleanup

- Removed legacy Home/Teden cycle configuration Kotlin wiring
- Removed hidden legacy cycle configuration XML from Home/Teden
- Removed obsolete Home-side What’s New routing logic
- Removed obsolete HomeFragmentForm and HomeFragmentSettings files
- Removed unused Home/dropdown-era strings
- Removed unused Work Log expected-start row layout
- Removed unused hidden dateResultText from cycle settings layout
- Removed obsolete onboarding active step title format string
- Cleaned up Home/Teden so it is focused on the operational weekly overview

### 🛠 Technical

- Moved legacy settings migration out of Home and into a dedicated migration helper
- MainActivity now runs legacy settings migration during startup
- Centralized launch routing through MainActivity and LaunchPrefs
- Confirmed Settings remains the owner of cycle, template, start date, first label, and skipped-day rules
- Confirmed onboarding remains the owner of first-run setup
- Confirmed Home/Teden no longer contains legacy cycle configuration dependencies
- Confirmed Gradle/dependency audit has no active build warnings
- Confirmed Room destructive migration fallback is not used
- Confirmed Work Log manual edit audit metadata is preserved in the current event model

### 🐛 Fixed

- Fixed fresh-install issue where What’s New could appear instead of onboarding after clearing app data
- Fixed duplicated launch logic between Home/Teden and MainActivity
- Fixed onboarding/What’s New ordering so What’s New is suppressed until onboarding is completed
- Fixed stale Home/Teden dependencies on hidden cycle configuration views

### ✅ Verification

- Verified `assembleDebug --warning-mode all` succeeds without warnings
- Verified no remaining active references to removed Home legacy configuration files, views, or layouts
- Verified Settings still uses cycle/rules include layouts correctly
- Verified widgets still compile and keep their resources
- Verified startup migration remains available for existing users

## v2.8

---

### ✨ Added

- Added first-run setup onboarding for new users
- Added guided cycle setup with template selection, start date, first label, and setup summary
- Added Work Log manual edit audit safety
- Added future-time warning for manually edited Work Log times
- Added Edited / Urejeno indicator and audit metadata for corrected Work Log events
- Added upcoming events card to the Week view
- Added dismiss option for the widget tip
- Added label length guidance for cycle and status labels
- Added localized built-in system status labels and template cycle labels for Slovenian and English

### 🎨 Improved

- Renamed main navigation from Home / Calendar to Week / Month
- Replaced Home cycle configuration with an operational Week view
- Improved Week view label spacing and status badges
- Improved Month view calendar cells with primary, secondary, and status rows
- Improved status colors with a semantic palette independent from cycle colors
- Improved Settings structure for Primary cycle, Secondary cycle, and Status labels
- Improved Single Shift setup by hiding unnecessary start date and first label settings
- Improved Work Cycle widget upcoming-days display on larger widgets
- Updated Material Components to 1.13.0 for Android 15 compatibility improvements
- Removed DEV from the debug app label
- Removed destructive Room migration fallback for safer Work Log data handling

### 🛠 Fixed

- Fixed fresh install routing so onboarding opens before What’s New
- Fixed onboarding being skipped by Work Log launch intents
- Fixed Off / Prosto sometimes using the wrong cycle color
- Fixed finished Work Log days incorrectly allowing a new Start Work action
- Fixed duplicate Start / Prihod events after Finish / Odhod
- Fixed Work Log manual edits keeping correct event type and audit metadata
- Fixed hardcoded status color matching based on localized text
- Fixed built-in status labels not displaying localized names
- Fixed outdated Settings hints and several Settings layout issues
- Fixed Home version label appearing on the Week view
- Fixed unused parameter warning in SlideToConfirmView

### 🧭 Planned / Not yet included

- In-app language selector
- Dynamic primary cycle color palette for 4+ primary cycle labels
- Multiple work sessions per day
- Further Work Log statistics and summaries

---

## v2.7

---

### ✨ Added

- Added weekly navigation to the Home 7-day preview
- Added previous / next week controls for the Home weekly overview
- Added quick return-to-current-week home icon in the weekly preview
- Added secondary label support for Work Log expected start/end times
- Added fallback order for expected work times:
  - Secondary label time
  - Primary cycle time
  - Global default work rules
- Added safety warning before starting Work Log on:
  - Off day
  - Vacation
  - Sick leave
- Added option to remove Vacation / Sick leave status and start work
- Added combined warning handling when multiple warning reasons apply
- Added `StatusSemantics` helper for business-level status behavior

### 🎨 Improved

- Improved Home 7-day preview into a clearer weekly overview
- Improved primary + secondary label display in Home preview:
  - `B • Š1`
  - status shown separately below
- Improved secondary label readability in Home preview cards
- Improved Work Log widget layout with clearer hierarchy
- Improved Work Log widget by showing Balance / Saldo as an additional quick info item
- Improved Work Log widget compact spacing for smaller widget sizes
- Improved Work Log Dashboard status card hierarchy
- Improved Work Log Dashboard state-specific visual tones:
  - Working
  - Break
  - Finished
  - Not started
- Improved Work Log Dashboard secondary actions with compact quick-action layout
- Improved Recent events from plain log text into structured timeline rows
- Improved Recent events empty state with clearer title and helper text
- Improved Work Log UI spacing, typography, and dark mode readability
- Improved expected start/end display behavior on off-days
- Improved Bottom Sheet day summary to show effective day state, including Off days

### 🛠 Fixed

- Fixed secondary labels being too visually weak compared to primary cycle/status
- Fixed Home preview status layout being too stacked and harder to read
- Fixed Work Log expected time fallback when secondary labels are used
- Fixed expected start/end showing incorrectly on Off days without explicit work time
- Fixed Start Work warning being shown for normal working statuses such as Terrain / Standby
- Fixed warning logic so normal secondary labels like Š1 / O1 / K1 do not trigger warnings
- Fixed Work Log start warning logic being tied to UI status visuals instead of business semantics
- Fixed Vacation / Sick leave removal flow so only removable non-working statuses are removed
- Fixed Recent events debug-like display style
- Fixed long Recent event notes/details safely truncating instead of breaking layout

---

## v2.6

---

- Added dedicated Work Log widget (Work Time) for quick work session overview
- Added live Work Log widget refresh mode for active sessions ("Today completed")
- Added battery-saving static widget mode ("Work start")
- Added new Work Log widget setting for choosing active session info mode
- Added separate widget previews for Work Cycle and Work Time widgets
- Added dedicated template picker bottom sheet instead of dropdown selection
- Added new Single Shift preset for users without rotating schedules
- Added restored advanced presets: 4-on / 4-off and Panama 2-2-3
- Added third primary cycle color support for 3-shift schedules
- Added new Cycle Colors settings section with unified color control across app
- Added status icons inside Calendar month view for faster visual overview
- Added expanded system status support:
  - Sick leave
  - Vacation
  - Standby
  - Reduction
  - Replacement
  - Meeting
  - Terrain

### 🎨 Improved

- Improved template selection UX with grouped General / Special sections
- Improved template card layout with cleaner compact preset cards
- Improved template naming consistency (Pismonoša A/B)
- Improved cycle color consistency between 7-day preview, Calendar, and widget
- Improved Work Log widget support for minimal and classic widget styles
- Improved status labels UI with clearer helper text for exclusive statuses
- Improved settings terminology by replacing old assignment wording with secondary labels
- Improved calendar compactness by showing status icons instead of long status text
- Improved widget naming clarity:
  - Work Cycle
  - Work Time
- Improved widget picker previews for clearer widget distinction
- Improved dark preset cycle colors for better readability and stronger contrast

### 🛠 Fixed

- Fixed Work Log widget not respecting minimal/classic widget style changes
- Fixed Work Log widget background not restoring correctly after switching styles
- Fixed Work Log widget not refreshing correctly during active work sessions
- Fixed status helper text visibility and empty spacing issues
- Fixed leftover "System label" text appearing in status labels screen
- Fixed template naming inconsistencies across Home, picker, and summary views
- Fixed cycle color mismatch between app views and widget rendering
- Fixed calendar rendering consistency for off-days and status priority display
- Fixed widget preview loading issues inside Android widget picker

---

## v2.5

### ✨ Added

- Added dedicated Work Log settings section for work rules configuration
- Added configurable daily target hours (daily work norm)
- Added configurable default break duration
- Added overtime tracking ON/OFF toggle
- Added expected start time per primary cycle label
- Added expected end time per primary cycle label
- Added per-cycle time picker configuration for start and end expectations
- Added session snapshot system for active work sessions (cycle label + expected start/end are locked during active work)
- Added compact deviation display for start/end time comparison with business meaning
- Added support for overnight and night-shift deviation handling across midnight
- Added improved Work Log dashboard header showing active cycle label with date
- Added slider-based primary action flow for Start / Finish / End Break actions

### 🎨 Improved

- Improved Work Log dashboard UX by replacing button-based primary actions with slider confirmation
- Improved dashboard clarity by hiding secondary actions before work starts
- Improved active session state visibility with cleaner status-first layout
- Improved compact display of start/end deviation without adding large extra cards
- Improved business logic for deviation colors and labels (Late start, Early finish, Overtime, etc.)
- Improved expected time handling for multi-shift and night-shift workflows
- Improved consistency of placeholders and dashboard state rendering
- Improved accessibility and haptic behavior for slider interactions
- Improved Slovenian and English translations for Work Log dashboard, widget, and notifications

### 🛠 Fixed

- Fixed slider staying disabled after finishing work due to lock-expiry UI refresh issue
- Fixed incorrect primary cycle source in Expected Start settings (O1–O5 issue from secondary cycle prefs)
- Fixed incorrect deviation calculation for night shifts showing absurd values like +1406 min
- Fixed overnight expected end handling for shifts crossing midnight
- Fixed notification permission safety for Android 13+ POST_NOTIFICATIONS flow
- Fixed safer notification update handling with SecurityException guard
- Fixed stale legacy resources and removed old unused dashboard/button leftovers
- Fixed obsolete Work Log header adapter and deprecated primary-button code paths

### 🧹 Cleanup

- Removed old 1-minute action guard logic (replaced by slider confirmation flow)
- Removed obsolete primary action button flow and related dead code
- Removed unused Work Log resources, strings, and old dashboard layout leftovers
- Cleaned Work Log string resources and normalized placeholders
- Cleaned old legacy UI remnants from previous button-based dashboard versions

---

## v2.4

### ✨ Added

- Added Work Log dashboard for daily work time tracking
- Added quick actions for Start Work / Finish Work flow
- Added break handling with active break state and resume flow
- Added meal logging with single-entry daily protection
- Added note logging for manual workday notes
- Added recent events timeline with clearer event history
- Added break start time and break duration tracking
- Added daily target hours (8h default) and balance (+ / - hours)
- Added improved clock-out event summary with total worked time
- Added one-minute action protection for work start/finish and break actions
- Added toast feedback when actions are blocked too early

### 🎨 Improved

- Improved Work Log dashboard layout with fixed action area and scrollable recent events list
- Improved status card UI for clearer working / break / finished states
- Improved break state visuals with highlighted active break card styling
- Improved spacing and top layout alignment for a cleaner Material 3 appearance
- Improved action cards with centered labels and better visual consistency
- Improved recent events formatting with date + time visibility
- Improved note events to display actual note content instead of generic label only
- Improved finished-day summary with clearer “Finished” state and clock-out context

### 🛠 Fixed

- Fixed accidental double-tap issues causing duplicated work events
- Fixed illogical rapid action sequences in work start/finish flow
- Fixed break action inconsistencies during active sessions
- Fixed meal action availability before starting work
- Fixed meal card alignment after meal logging
- Fixed excessive top spacing caused by duplicated toolbar handling
- Fixed outdated hardcoded strings by moving Work Log texts to string resources
- Fixed multiple old resource warnings and cleaned unused legacy strings

---

## v2.3

### ✨ Added
- Added full Status layer (Bolniška, Dopust, Dežurstvo) separated from secondary schedule
- Added dedicated Status labels screen for managing system status labels
- Added clear separation between Secondary labels and Status labels in Settings
- Added improved toolbar titles for Secondary and Status screens
- Added scroll hint (fade overlay) for better navigation on long screens

### 🎨 Improved
- Improved Settings structure by separating Secondary and Status label management
- Improved day editor by simplifying layout and removing redundant actions
- Improved UI spacing and layout consistency across settings screens
- Improved dark theme visuals (fixed incorrect fade overlay color)
- Improved toolbar title handling for better context awareness

### 🛠 Fixed
- Fixed incorrect toolbar title showing "Delovni cikel" on label screens
- Fixed excessive top spacing on label management screens
- Fixed duplicate UI elements in Settings layout
- Fixed incorrect resource usage in settings layouts

---

## v2.2

### Added
- Added persistent secondary schedule layer groundwork for combined schedule support  
- Added improved 7-day preview layout with clearer primary and secondary label hierarchy  
- Added improved calendar day cells with layered primary and secondary information  
- Added richer day editor summary and state cards for primary and secondary layers  
- Added color-coded cycle override chips in day editor  
- Added improved visual styling for active cycle and secondary state cards  
- Added support groundwork for cleaner 1x1 widget behavior with compact labels  
- Added full-height bottom sheet behavior for the day editor  
- Added fixed bottom action bar in the day editor so Save and Cancel remain visible  
- Added safer bottom sheet background and inset handling for better rendering across devices  
- Added global edge-to-edge handling with proper status bar and navigation bar insets  
- Added dynamic calendar label shortening (min. 3 characters) for better multi-language support  

### Improved
- Improved terminology across the app from assignment-focused wording toward secondary label terminology  
- Improved day editor UX by clearly separating primary cycle editing from secondary label editing  
- Improved disabled-state behavior so cycle override editing still works even when secondary labels are disabled  
- Improved summary text and helper text for secondary labels  
- Improved dark mode rendering for cycle and secondary state cards  
- Improved light mode card tint balance and spacing  
- Improved calendar compactness and readability for secondary labels  
- Improved preview presentation so secondary information feels like a real second schedule layer  
- Improved chip styling for cycle override selection  
- Improved widget sizing logic for compact and minimal layouts  
- Improved widget layout adaptability across 1x1, 2x1 and larger sizes  
- Improved system bar icon contrast handling based on theme  
- Improved overall layout consistency across different devices (Pixel, gesture navigation, etc.)  

### Fixed
- Fixed issue where day editor could not be opened when secondary labels were disabled  
- Fixed issue where cycle override editing was blocked by disabled secondary-label settings  
- Fixed bottom sheet rendering glitches caused by transparent or unstable backgrounds  
- Fixed bottom sheet height behavior causing clipped content and hidden actions  
- Fixed navigation-bar / gesture-area overlap in the day editor  
- Fixed spacing inconsistencies between cards, dividers, and action areas  
- Fixed multiple UI issues affecting dark/light mode consistency  
- Fixed 1x1 widget text overflow issue for long labels like off-day labels  
- Fixed several compact widget display edge cases for small sizes  
- Fixed calendar and preview presentation inconsistencies for layered schedule data  
- Fixed widget not updating when changing cycle template  
- Fixed system bar overlap issues on devices with display cutouts (e.g. Pixel 6)  

### Changed
- Changed secondary label handling from simple override-only behavior toward a persistent layered schedule model  
- Changed `DefaultScheduleResolver` flow to better support primary and secondary combined schedule resolution  
- Changed day editor wording and UX to better match the future combined schedule direction  
- Changed 1x1 widget direction toward compact symbolic display instead of full translated words  
- Changed behavior so cycle overrides are reset when switching cycle templates (with confirmation dialog when needed)  
- Changed template switching logic to behave more predictably and safely  

### Notes
- This release continues the transition from the original A/B-focused model toward a more flexible multi-layer WorkCycle system  
- Combined schedule architecture is now much better prepared for future additions such as route/work-type layers, richer day editing, and advanced schedule presets  
- UI and widget behavior are now significantly more consistent across different screen sizes, languages, and devices  

---

## v2.1

### ✨ Added
- New cycle templates (4 days work / 4 days off)
- New cycle templates (Panama 2-2-3 shift schedule)
- Continuous shift support for templates (no weekend/holiday skipping)
- Visual indicator for continuous shifts in template description
- Cycle block indicators (e.g. Work 3/7, Off 2/7) in 7-day preview
- Improved chip labels with position indicators (e.g. Work 1/7)
- App logo displayed in toolbar (adaptive to light/dark theme)

### 🎨 Improved
- Toolbar redesigned with left-aligned title (better for long text)
- App logo added for stronger branding
- Better readability of cycle blocks in preview
- Improved consistency between cycle chips and preview indicators
- More accurate visual representation of shift-based schedules
- Cleaner spacing and alignment in top app bar

### 🐞 Fixed
- Fixed incorrect cycle block labels (mismatch between Work and Off)
- Fixed preview inconsistencies when using skip rules with templates
- Fixed duplicate validation errors when using templates with repeated labels
- Fixed chip selection issues (wrong index being saved)
- Fixed "Save" button flickering or not appearing correctly
- Fixed incorrect reset of selected cycle day after saving
- Fixed dropdown rendering issues for template sections
- Fixed resource compilation issues in layouts (missing namespace)

### ⚙️ Technical
- Improved cycle index calculation for preview (template-aware logic)
- Added support for duplicate labels in template mode
- Introduced block-position calculation (e.g. 3/7 logic)
- Refactored toolbar handling (logo + title behavior)
- Improved template configuration (continuous vs non-continuous)
- Better separation between base cycle and resolved cycle
- UI stability improvements across fragments
- Minor code cleanup and consistency improvements

🚀 Version 2.1 expands template support, improves accuracy of cycle visualization, and refines the overall UI experience.

---

## v2.0

## ✨ Added

- Assignment system (manual day overrides)
- Assignment labels (Sick, Vacation, Standby, Field + custom labels)
- Assignment icons and color indicators
- Click on day → edit assignment (bottom sheet)
- Assignment support in 7-day preview
- Assignment support in Calendar
- Template system (foundation)
- Pošta Slovenije preset (fixed A/B schedule)
- Template-based restrictions (locked cycle/rules)
- Template-specific assignment labels (O1, Š1)
- Automatic cleanup of template-specific data
- Assignment usage tracking (usageCount, lastUsedAt)

## 🎨 Improved

- Redesigned 7-day preview layout (3-zone layout: date / assignment / cycle)
- Better visual hierarchy (assignment now clearly visible)
- Icons added to assignments for faster recognition
- Reduced visual dominance of cycle when assignment is present
- Improved card balance and spacing (Material 3 style)
- Cleaner and more consistent UI across screens
- Improved label sorting (by usage + recency)
- Better handling of empty states (no assignment)

## 🐞 Fixed

- Fixed assignment labels not updating correctly after template change
- Fixed leftover template labels (O1, Š1) after switching template
- Fixed stale assignment data still showing in preview/calendar
- Fixed multiple UI inconsistencies in preview list
- Fixed icon/dot rendering conflicts
- Fixed edge cases with empty/invalid labels
- Fixed crashes related to missing label references

## ⚙️ Technical

- Introduced TemplateManager system
- Added template-aware configuration layer
- Implemented assignment label persistence (JSON-based)
- Added cleanup logic for template-specific data
- Improved ManualScheduleRepository (bulk cleanup support)
- Refactored CyclePreviewAdapter for new layout
- Simplified assignment rendering logic (single source of truth)
- Improved separation between cycle and assignment logic
- Prepared architecture for combined schedule (cycle + manual)
- Codebase cleanup and consistency improvements

---

🚀 Version 2.0 is a major update introducing assignment tracking, templates, and a redesigned UI.

---

## v1.9

## ✨ Added

- New Settings screen (configuration moved out of Home)
- New Help screen with instructions and FAQ
- New Statistics screen (overview of A / B / Off days)
- First cycle day selection with chips UI
- Toolbar icons for Settings and Help
- Support for additional countries: Italy (IT) and Hungary (HU)
- Added fallback mechanism for daily updates

## 🎨 Improved

- Refactored to single-activity + fragments architecture
- Cleaner and more focused Home screen
- Improved What’s New screen (no duplicate titles)
- Clearer settings labels and descriptions
- Updated wording (e.g. "Select a label")
- Removed redundant "More" subtitles
- Improved dropdown behavior
- Better top bar titles
- Improved layout spacing (Save/Revert)
- General UI polish

## 🐞 Fixed

- Fixed Save / Revert buttons showing incorrectly
- Fixed false detection of unsaved changes
- Fixed navigation state issues
- Fixed preset selection marking changes incorrectly
- Fixed dropdown not showing items
- Fixed chip color rendering
- Fixed resource linking errors
- Fixed toolbar/navigation inconsistencies
- Fixed status bar alignment
- Fixed notification permission handling
- Fixed multiple crashes

## ⚙️ Technical

- Introduced proper form state comparison logic
- Improved HomeFragment lifecycle handling
- Refactored change listeners
- Centralized navigation and toolbar
- Cleaned string resources (EN + SL)
- Improved SharedPreferences handling
- Removed unused code
- Prepared for future features

---

## v1.8

### ✨ Added
- New **Statistics screen** (overview of A / B / Off days)
- Support for additional countries: **Italy (IT)** and **Hungary (HU)**
- Country selection now includes **flag indicators** for better clarity
- Preparation for **manual language selection** (foundation implemented)

### 🎨 Improved
- **Calendar redesign**
  - Removed empty/placeholder cells for cleaner layout
  - Added **swipe navigation** between months
- Improved **country selection UI**
  - Auto-detected country clearly labeled
  - Localized country names (based on app language)
- Updated **What’s New screen**
  - Localized content (EN / SL)
  - Cleaner version display format
- Improved **icon consistency** across the app (Material style alignment)
- General UI polish and consistency improvements

### ⚙️ Technical
- Refactored holiday handling to support multiple countries more cleanly
- Added caching improvements for holiday calculations
- Introduced base structure for future **app language switching**
- Code cleanup and preparation for upcoming 2.0 features

---

## v1.7.1

### 🐛 Fixed
- Fixed dropdown selection issues (presets, start day, holiday country)
- Fixed incorrect initial values in cycle configuration
- Fixed widget layout compatibility issues (RemoteViews)
- Fixed launcher icon configuration and adaptive icon behavior
- Fixed minor resource and configuration inconsistencies

### 🎨 Improved
- Improved launcher icon rendering across different Android versions and launchers
- Better consistency of dropdown behavior (non-editable + full list display)
- Minor UI polish and spacing adjustments

### ⚙️ Technical
- Continued refactor of MainActivity into smaller helper classes
- Moved utility logic into dedicated util package
- Cleaned up unused resources and reduced lint warnings
- Improved resource structure (mipmap, icons, themes)

---

## v1.7

### ✨ Added
- Added bottom navigation with 3 sections (Home, Calendar, More)
- Added new Calendar screen for full month overview
- Added Statistics button (disabled for now)
- Added theme switch (System, Light, Dark)

### 🎨 Improved
- Improved overall UI consistency and spacing
- Larger and more user-friendly dropdown/select inputs
- Cleaner and more structured settings layout
- Improved visual hierarchy on main screen
- Better consistency across different screen sizes
- Unified status bar and top layout across screens
- Improved alignment and spacing in Calendar screen
- New adaptive launcher icon (Material-style, consistent across devices)

### 🔁 Changed
- Introduced preset vs custom cycle logic
- Preset remains selected until user modifies values
- Custom state is shown when manually adjusted
- Removed "Preveri datum / Check date" feature
- Improved navigation flow between screens

### 🐛 Fixes
- Fixed bottom navigation active state (Home highlight)
- Fixed RemoteViews unsupported view issue in widget
- Fixed widget layout orientation warning
- Fixed receiver parameter warnings in Kotlin
- Fixed duplicate resource issue (launcher background)
- Fixed minor layout inconsistencies across screens

### ⚙️ Technical
- Introduced BaseActivity for shared UI behavior
- Refactored navigation into reusable helper
- Moved utility functions into util package (sanitizeLabel, parsing)
- Improved activity structure for better scalability
- Prepared foundation for future features (statistics, templates, manual mode)

---

## v1.6.2

### 🐛 Fixed
- Fixed app crash on launch caused by navigation setup
- Fixed screen flickering due to repeated activity reloads
- Fixed Main screen overlapping the Android status bar
- Fixed inconsistent bottom navigation height in MainActivity
- Fixed edge-to-edge inconsistencies across different screens

### ⚙️ Technical
- Implemented proper edge-to-edge (WindowInsets) handling
- Unified handling of status bar, navigation bar and IME (keyboard) insets
- Ensured bottom inset is applied only to BottomNavigationView
- Removed incorrect inset usage on parent containers
- Improved layout stability across devices and Android versions

---

## v1.6.1

### 🐛 Fixed
- Fixed **Main screen overlapping the Android status bar**
- Fixed **inconsistent edge-to-edge behavior** compared to Calendar and More screens
- Fixed issue where **saved settings were not loaded after app restart**
- Fixed crash caused by uninitialized `selectedDate`
- Fixed **start date showing placeholder (%1$s) instead of formatted value**

### ⚙️ Technical
- Unified window insets handling (status bar + navigation bar)
- Removed conflicting inset listeners causing layout issues
- Restored correct initialization flow in `onCreate()`
- Improved stability of settings persistence

---

## v1.6

### ✨ Added
- New **Calendar view** (monthly overview of cycle)
- **Bottom navigation** (Home / Calendar / More)
- New **More screen**:
  - Contact author
  - Report a bug
  - Open GitHub project
  - Support development (donate)
- **Cycle presets** for quick setup (A/B, shifts, etc.)
- Email integration for **feedback and bug reporting**

### 🎨 Improved
- Unified **header across all screens**
- Cleaner and more consistent **Material UI**
- Improved **More screen layout** (cards + navigation)
- Better visual hierarchy and spacing
- Icons consistency across app

### ⚙️ Technical
- Refactored navigation (removed deprecated transitions)
- Improved calendar refresh logic after cycle changes
- Cleaned unused resources (strings, layouts)
- Improved error handling (email / external intents fallback)
- Codebase cleanup and modularization

### 🐛 Fixed
- Calendar not updating after cycle change
- Missing translations in presets
- Multiple unresolved references in presets logic
- Various minor UI and stability fixes

---

## v1.5

### ✨ Added
- Support for **multiple countries (SI, AT, HR)** for public holidays
- **Country selector** for holiday calculation
- **Auto-detect country** (SIM / locale based)
- Auto-selected country is labeled as **(auto-detected)**
- **Fallback logic** for country detection
- **Holiday caching** for improved performance
- Upcoming days preview (cycle list)
- Improved **status display** with day + cycle (e.g. "Sat. A")
- Confirmation dialog when leaving with **unsaved changes**

### 🎨 Improved
- Redesigned **status card** (focus on today’s cycle)
- Replaced generic "Status" with contextual **"Today"**
- Improved **"Today / Tomorrow" display** with weekday
- Improved clarity of **cycle configuration inputs**
- Better UX for **start day selection**
- Cleaner and more modern **overall layout**
- Save bar:
  - Appears only when changes are made
  - Smooth fade in/out animation
  - Less intrusive design
- Widget improvements:
  - Better scaling across widget sizes
  - Compact display for small widgets (e.g. 1x1 → "X")
  - Improved consistency across devices
- Improved readability (e.g. "Sat. Off")

### 🐛 Fixed
- Fixed bug where **start day was saved as "A,B" instead of single value**
- Fixed empty cycle label in status display
- Fixed skipped day returning blank label
- Fixed **Save button not clickable** (navigation bar overlap)
- Fixed layout issues on Samsung devices (edge-to-edge)
- Fixed layout issues on smaller devices (e.g. A52)
- Fixed country dropdown not showing all options
- Fixed duplicate resource issues (app icon)
- Fixed unresolved references after refactor

### ⚙️ Technical
- Refactored `HolidayManager` for multi-country support
- Centralized holiday logic (`isHoliday(context, date)`)
- Added fallback logic for invalid/missing country selection
- Added holiday caching layer
- Improved SharedPreferences handling and defaults
- Codebase split into smaller files (`MainActivityUI`, `MainActivitySettings`, etc.)
- Improved date formatting using `TextStyle`

### 🌍 Localization
- Updated Slovenian and English translations
- Improved wording for cycle configuration
- Replaced "Skip holidays" with more accurate meaning (work-free days)

---

## v1.4.1

### 🐛 Fixed
- Fixed issue where "First cycle day" was not saved correctly
- Fixed incorrect value (e.g. "A,B") shown after app update
- Fixed migration from older versions to preserve selected cycle start day

### ⚙️ Technical
- Added dedicated storage for first cycle day preference
- Improved legacy settings migration logic

---

## v1.4

### ✨ Added
- Advanced **cycle validation system** (empty input, format, max items, label length)
- Improved **"Check date"** feature with selected date + cycle result
- Custom **override label for skipped days** (e.g. "Prosto")
- **Holiday country selector** (currently Slovenia, prepared for future expansion)

### 🚀 Improved
- Improved cycle input parsing and normalization
- More accurate terminology: **non-working days instead of holidays**
- Better UX for validation errors (clear feedback messages)
- Improved settings layout and logical grouping
- Improved date check readability

### ⚙️ Technical
- Introduced **HolidayManager dispatcher** based on selected country
- Refactored cycle calculation to support skip rules + override label
- Improved SharedPreferences structure and defaults
- Prepared architecture for **multi-country holiday support**

### 🐛 Fixed
- Fixed incorrect holiday logic (removed non-working days that are not official)
- Fixed notification behavior and reliability issues
- Fixed duplicate string resources
- Fixed edge cases in cycle calculation

### 🌍 Localization
- Updated wording for non-working days
- Improved Slovenian translations consistency
- Cleaned unused and duplicate string keys

---

## v1.3

### ✨ Added
- Notification when A/B cycle changes
- Notification shows **today and tomorrow cycle**
- Option for **silent notifications**
- **Minimal widget style**
- Sticky **Save button** with unsaved changes detection
- App remembers **last opened section** (cycle / rules / display)

### 🎨 Improved
- Improved UX with **unsaved changes tracking**
- Save button now only active when needed
- Cleaner **cycle preview** (no duplicate today/tomorrow)
- Better navigation between sections
- More intuitive settings flow

### ⚙️ Technical
- Centralized notification handling (`NotificationHelper`)
- Added new preferences for widget style and notifications
- Improved state handling in `MainActivity`
- Refactored settings save logic
- Improved widget update triggering

### 🐞 Fixed
- Fixed duplicate notification title text
- Fixed widget style not saving correctly
- Fixed multiple reference issues in `Prefs`
- Fixed crashes related to view bindings and layout types

---

## v1.2

### ✨ Added
- Widget now displays up to **6 upcoming cycle days**
- **Dynamic widget layout** depending on widget size
  - Small → today only
  - Medium → today + tomorrow
  - Large → today + next 6 days
- **Today / Danes label** above the main cycle indicator
- **Cycle color dots** for upcoming days
- **Left color bar** showing today's cycle color

### 🎨 Improved
- Improved widget layout and spacing
- Better alignment of cycle dots and day labels
- Fixed text wrapping issues (Tomorrow / day names)
- Improved cycle color display
- More consistent widget appearance across launchers

### 🌍 Localization
- Added localized labels for **Today / Tomorrow**

### ⚙️ Technical
- Widget updates correctly when **resized**
- Improved **RemoteViews compatibility**
- Refactored widget layout for better stability
- Improved cycle color management

### 🐞 Fixed
- Fixed widget loading issue in emulator
- Fixed color rendering issues in future cycle rows
- Fixed layout problems with longer day names

---

## v1.1

### ✨ Added
- Completely redesigned user interface
- Collapsible sections for more organized settings
- Status card showing **today's and tomorrow's cycle**
- Ability to **check A/B cycle for any date**
- Custom labels for **A, B and off day**
- Support for **skipping Saturdays and Sundays separately**
- Warning message if the widget is not yet added to the home screen
- Application **version display**

### 🎨 Improved
- Improved widget behavior
- Improved overall UI clarity and layout

### ⚙️ Technical
- Various stability and performance improvements

---

## v1.0

### ✨ Initial release
- Widget displays **A/B work cycle**
- Weekend skipping
- Public holiday skipping
- Manual cycle shift
- Prefix text before cycle label
- Automatic widget update at midnight
