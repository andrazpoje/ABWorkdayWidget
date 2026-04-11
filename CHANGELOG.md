# Changelog

All notable changes to this project will be documented in this file.

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