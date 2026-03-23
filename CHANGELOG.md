# Changelog

All notable changes to this project will be documented in this file.

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

## [1.6] - 2026-03-22

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

## [1.5] - 2026-03-21

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

## [1.4.1] - 2026-03-20

### 🐛 Fixed
- Fixed issue where "First cycle day" was not saved correctly
- Fixed incorrect value (e.g. "A,B") shown after app update
- Fixed migration from older versions to preserve selected cycle start day

### ⚙️ Technical
- Added dedicated storage for first cycle day preference
- Improved legacy settings migration logic

---

## [1.4.0] - 2026-03-19

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

## [1.3.0] - 2026-03-17

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

## [1.2.0] - 2026-03-16

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

## [1.1.0] - 2026-03-11

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

## [1.0.0] - 2026-03-07

### ✨ Initial release
- Widget displays **A/B work cycle**
- Weekend skipping
- Public holiday skipping
- Manual cycle shift
- Prefix text before cycle label
- Automatic widget update at midnight