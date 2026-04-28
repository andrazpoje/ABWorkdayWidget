# WorkCycle Roadmap

Ta dokument vsebuje načrt razvoja, backlog ideje in tehnične opombe za prihodnje verzije aplikacije WorkCycle.

---

## v2.8 – Release / Stabilizacija

### Release checklist

- [ ] Full regression test
- [ ] Final Play Store screenshots
- [ ] Posodobiti `CHANGELOG.md`
- [ ] Posodobiti in-app What's New stringe SL/EN
- [ ] Pripraviti Google Play novosti SL/EN
- [ ] Build signed AAB
- [ ] Upload v Play Console
- [ ] Preveriti Android 15 edge-to-edge / Material warning v Play Console
- [ ] Commit, tag `v2.8`, push tags

### Ročni QA

- [ ] Fresh install odpre onboarding pred What's New
- [ ] Existing install ne dobi onboardinga
- [ ] Locked template preskoči start date / first label
- [ ] Single Shift preskoči start date / first label
- [ ] 2-shift / 3-shift prikažeta potrebne onboarding korake
- [ ] SL/EN built-in status labels
- [ ] SL/EN built-in template labels ob izbiri template-a
- [ ] Custom labels ostanejo nespremenjeni
- [ ] Bottom nav: Teden / Mesec / Več in Week / Month / More
- [ ] Week view: primary + secondary spacing, status badges, bližajoči dogodki
- [ ] Month view: primary, secondary badge, status icon circle
- [ ] Settings: primarni cikel, sekundarni cikel, statusne oznake, barve, videz
- [ ] Work Log: Start, Break, End Break, Finish
- [ ] Work Log manual edit audit
- [ ] Future-time warning
- [ ] Finished Work Log day ne ponudi novega Start
- [ ] Work Cycle widget: upcoming days na večjem widgetu
- [ ] Work Time widget
- [ ] Light/dark mode
- [ ] Manjši zaslon

---

## v2.9 – Foundations / Cleanup

Cilj v2.9 je pripraviti varne osnove za večje funkcije v v3.0, brez agresivnega širjenja funkcionalnosti tik pred naslednjim večjim sklopom.

### Koda in arhitektura

- [ ] Dodati file-level KDoc komentarje pomembnim Kotlin datotekam
- [ ] Dodati KDoc za ne-očitne public/internal funkcije
- [ ] Brez komentarjev na trivialne funkcije
- [ ] Preveriti in očistiti stare legacy Home cycle config bindinge, če niso več potrebni
- [ ] Pregledati Settings strukturo in odstraniti podvojene helperje
- [ ] Preveriti unused resources po v2.8 UI spremembah
- [ ] Pregledati Gradle deprecation warninge pred prihodnjo Gradle 9 nadgradnjo

### Prioritetne datoteke za KDoc

- [ ] `DefaultScheduleResolver`
- [ ] `TemplateManager`
- [ ] `ScheduleTemplateProvider`
- [ ] `StatusVisuals`
- [ ] `StatusSemantics`
- [ ] `WorkLogDashboardViewModel`
- [ ] Work Log storage/repository sloj
- [ ] `WorkCycleWidgetProvider`
- [ ] Work Time widget provider
- [ ] `CycleSetupOnboardingFragment`
- [ ] `PrimaryCycleSettingsController`

### Varnost podatkov

- [ ] Dodatno preveriti Room migracije
- [ ] Preveriti backup/export smer za Work Log podatke
- [ ] Ne uporabljati destructive migration fallback za Work Log podatke
- [ ] Pripraviti temelje za profile oziroma arhiviranje službe

---

## v3.0 – Večji funkcionalni sklop

### In-app language selector

- [ ] Dodati izbiro jezika neposredno v aplikaciji
- [ ] Možnosti:
  - System default / Samodejno
  - Slovenščina
  - English
  - Deutsch kasneje
- [ ] Uporabiti AndroidX/AppCompat per-app language support
- [ ] Custom/user labels se ne prevajajo samodejno
- [ ] Built-in system labels se lokalizirajo prek stabilnih `iconKey`
- [ ] Built-in template labels se lokalizirajo prek stabilnih `templateKey` / label keyev
- [ ] Preveriti widgete po spremembi jezika

### Work Log: več delovnih sej / deljeni delovni čas

- [ ] Dodati nastavitev: `Dovoli več delovnih sej na dan`
- [ ] Default: OFF
- [ ] Po zaključenem dnevu prikaži `Začni novo sejo` samo, če je funkcija vklopljena
- [ ] Vsaka seja ima svoj Start / Finish
- [ ] Dnevni total sešteje vse seje
- [ ] Saldo podpira več sej
- [ ] Recent events podpira več sej
- [ ] Widget podpira več sej
- [ ] Manual edit audit podpira več sej
- [ ] Future-time safety podpira več sej
- [ ] Kasneje dodati poštni split-shift profil, npr. 08:00–10:00 in 12:00–18:00

### Break / meal reminder ali auto-end

- [ ] Dodati nastavitev trajanja odmora/malice
- [ ] Privzeta vrednost: 30 min
- [ ] Najprej podpreti reminder-first pristop
- [ ] Opcijsko dodati samodejni zaključek
- [ ] Auto-end mora biti audit-safe
- [ ] Samodejno ustvarjen dogodek naj ima source, npr. `auto_break_end`
- [ ] Če uporabnik sam konča odmor pred timerjem, se scheduled auto-end prekliče
- [ ] Če uporabnik ročno popravi auto event, mora ostati auditiran
- [ ] Prikaz preostalega časa:
  - `Malica: še 12 min`
  - `Odmor: še 8 min`
- [ ] Če čas poteče:
  - `Odmor presežen za 5 min`
  - ali notification/reminder

### Onboarding izboljšave

- [ ] Dodati opcijski Work Log / time tracking setup korak
- [ ] Vprašanje: ali želi uporabnik uporabljati beleženje časa
- [ ] Če da:
  - dnevni cilj
  - trajanje odmora/malice
  - overtime tracking
  - morebitno Work Time widget guidance
- [ ] Korak mora biti skippable
- [ ] Ne sme preobremeniti uporabnikov, ki želijo samo cikel/koledar

---

## Future – Work Profiles / Zamenjava službe

### Osnovni model

- [ ] Dodati koncept delovnega profila
- [ ] En aktiven profil v osnovni verziji
- [ ] Arhivirani profili za prejšnje službe
- [ ] Možnost: `Zamenjava službe`
- [ ] Arhiviraj trenutno službo in začni novo
- [ ] Ponastavi samo urnik in nastavitve
- [ ] Izvozi podatke pred ponastavitvijo
- [ ] Izbriši vse podatke samo z jasnim opozorilom

### Kaj naj profil vsebuje

- [ ] Primarni cikel
- [ ] Sekundarni cikel
- [ ] Statusne oznake
- [ ] Work Log pravila
- [ ] Work Log dogodki
- [ ] Statistika
- [ ] Widget nastavitve
- [ ] Export/report podatki

### Premium smer

- [ ] Več aktivnih delovnih profilov hkrati naj bo premium funkcija
- [ ] Ločeni urniki za vsako službo
- [ ] Ločena Work Log pravila
- [ ] Ločena statistika
- [ ] Izvoz po profilu
- [ ] Widgeti po profilu
- [ ] Jasno preklapljanje aktivnega profila, da uporabnik ne beleži časa pod napačno službo

---

## Future – Premium / Power-user Features

Power-user funkcije naj bodo kandidati za plačljivo verzijo.

- [ ] Multiple active work profiles
- [ ] Advanced Work Log rules
- [ ] Advanced statistics
- [ ] Exports
- [ ] Cloud sync
- [ ] Advanced templates
- [ ] Advanced widget customization
- [ ] Multiple work sessions per day
- [ ] Split-shift postal profile

Osnovna verzija mora ostati uporabna:

- [ ] Osnovni delovni cikel
- [ ] Tedenski in mesečni pregled
- [ ] Osnovni statusi
- [ ] Osnovni Work Log
- [ ] Osnovni widgeti

---

## Future – Calendar / Schedule

- [ ] Dynamic primary cycle color palette za 4+ primary labels
- [ ] Boljša podpora za dolge oznake
- [ ] Legenda statusnih ikon
- [ ] Naprednejši prikaz več statusov na isti dan
- [ ] Dolg pritisk na datum za hitro dodajanje izjeme/statusa
- [ ] Upcoming events card compact polish
- [ ] Možnost override-a prostega dne v delovni dan
- [ ] Manual mode kot premium/future feature

---

## Future – Widgets

- [ ] Nadaljnji size-aware layout za Work Cycle widget
- [ ] Boljši 1x1 widget
- [ ] Work Time widget z break/meal remaining time
- [ ] Widget color system poenotiti z aplikacijo
- [ ] Per-profile widgets, če se uvede Work Profiles
- [ ] Widget guidance po onboardingu

---

## Future – Sync / Backup

- [ ] Firebase/cloud sync med napravami
- [ ] Export/backup pred resetom ali zamenjavo službe
- [ ] Restore backup
- [ ] Sync naj bo premium kandidat
- [ ] Konfliktna pravila za Work Log podatke pri več napravah

---

## Notes

- Custom/user labels se ne smejo samodejno prevajati.
- Built-in labels naj uporabljajo stabilne keye, ne display teksta.
- Work Log podatki so občutljivi; izogibaj se tihim destructive migracijam.
- Manual edits morajo ostati audit-safe.
- Future-time edits morajo ostati posebej označeni/opozorjeni.
