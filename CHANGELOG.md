# Changelog

All notable changes to this project will be documented in this file.

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