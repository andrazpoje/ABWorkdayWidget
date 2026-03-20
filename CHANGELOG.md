# Changelog

All notable changes to this project will be documented in this file.

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