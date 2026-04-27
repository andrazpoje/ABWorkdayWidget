# WorkCycle

Android aplikacija za upravljanje delovnega cikla, izmen in Work Log beleženja.

Prej znana kot **AB Switch Widget / ABWorkdayWidget**, zdaj razširjena v celoten sistem za spremljanje delovnega urnika.

---

## Funkcije

### Delovni cikel

- prikaz trenutnega delovnega cikla (A/B, 2-shift, 3-shift, custom)
- 7-dnevni tedenski pregled z navigacijo po tednih
- mesečni koledarski pregled
- podpora za vikende in praznike
- podpora za več držav praznikov (SI, IT, HU)
- assignment / secondary labels (npr. Dopust, Bolniška, Teren, Dežurstvo, Š1, O1 ...)
- prednastavitve / templates (npr. Pošta Slovenije workflow)
- widget za domači zaslon

### Work Log

- beleženje začetka in konca dela
- break / meal / note sistem
- daily balance (+/-)
- expected start/end time glede na delovni cikel
- recent events pregled
- Work Log dashboard
- ločen Work Log widget za hiter pregled

---

## Namestitev

Aplikacija je na Google Play:

https://play.google.com/store/apps/details?id=com.dante.workcycle

---

## Tehnologija

Projekt je narejen v:

- Android Studio Panda (2025+)
- Kotlin
- AndroidX
- Material 3
- Navigation Component
- App Widgets
- SharedPreferences

---

## Build konfiguracija

- minSdk: 26
- targetSdk: 36
- compileSdk: 36

Package:

```text
com.dante.workcycle