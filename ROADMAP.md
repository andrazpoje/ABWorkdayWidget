# WorkCycle Roadmap

Ta dokument vsebuje načrt razvoja, backlog ideje, monetizacijsko smer in tehnične opombe za prihodnje verzije aplikacije WorkCycle.

**Trenutni status:** v2.9 je objavljen na Google Play. Aktivni razvoj poteka na **v3.0**.

**Glavna smer v3.0:** Work Log foundation, obračun delovnega časa/odmorov, stabilizacija podatkov in priprava za kasnejše premium funkcije.

---

## Roadmap načela

- Osnovna aplikacija mora ostati uporabna brez plačila.
- Premium naj najprej odklepa offline / lokalne power-user funkcije.
- Naročnina naj bo rezervirana za funkcije, ki imajo stalni strošek ali stalno storitev: sync, cloud backup, GPS/geofencing suggestions, web/live sharing.
- Work Log podatki so občutljivi; brez tihih destructive migracij.
- Ročni popravki morajo ostati audit-safe.
- Future-time edits morajo ostati posebej označeni/opozorjeni.
- Raw časi dogodkov se morajo hraniti; zaokroževanje in obračun sta ločena accounting plast.
- Custom/user labels se ne smejo samodejno prevajati.
- Built-in labels naj uporabljajo stabilne keye, ne display teksta.

---

## v2.9 – Released / Google Play

**Status:** zaključeno in objavljeno na Google Play.

v2.9 je bila foundations / cleanup verzija. Namen je bil stabilizirati aplikacijo po večjih v2.x spremembah in pripraviti teren za v3.0.

### Zaključeno v v2.9

- [x] KDoc / dokumentacija pomembnih Kotlin datotek
- [x] Cleanup legacy Home/Teden cycle config logike
- [x] Home/Teden očiščen kot operativni tedenski pregled
- [x] Onboarding startup / What’s New routing popravljen
- [x] Help in widget terminologija očiščena
- [x] Room / destructive migration safety audit
- [x] ROADMAP.md dodan in uporabljen kot razvojni backlog
- [x] Debug-only Developer Tools za hitrejše testiranje onboardinga/resetov
- [x] Changelog / What’s New / Google Play novosti pripravljene

---

## v3.0 – Active development

**Status:** aktivni razvoj.

### Completed so far

- [x] Work Log resolver foundation
- [x] Work Log accounting foundation
- [x] Dashboard/widget balance parity prek skupnega accounting layerja
- [x] Break Accounting Mode v Work Log Settings
- [x] Credited time UI na dashboardu
- [x] Active break elapsed / remaining / exceeded prikaz
- [x] Work Log CSV export prek SAF CreateDocument

v3.0 se ne sme obravnavati kot en sam ogromen feature release. Najprej mora zaključiti Work Log foundation, nato lahko doda uporabniške funkcije nad stabilno arhitekturo.

### v3.0 cilji

- Stabilen Work Log state resolver
- Ločen accounting sloj za delovni čas, odmore in saldo
- Mednarodno prilagodljiva pravila odmora
- Jasnejši Work Log dashboard
- Priprava za split shifts / multiple sessions
- Priprava za export/backup/premium smer
- Brez nepotrebnih Room schema sprememb, dokler model več sej ni potrjen

---

## v3.0 – Work Log state foundation

### Zaključeno

- [x] Dodan `WorkLogSessionStateResolver`
- [x] Resolver centralizira:
  - canonical ordering: `time ASC, id ASC`
  - status dneva/seje
  - active break start
  - meal logged state
  - worked/effective minutes
  - action availability osnove
- [x] Dashboard status uporablja resolver
- [x] Work Time widget status uporablja resolver
- [x] WorkLogEventValidator uporablja resolver ordering
- [x] Dashboard uporablja resolver za:
  - `Efektivno delo`
  - active break
  - meal state
  - secondary action availability
- [x] Persistent notification worked text uporablja resolver semantics
- [x] Dodani JUnit testi za resolver
- [x] Brez Room/schema sprememb

### Preostalo / previdno

- [ ] Ne centralizirati recent event totalov, dokler ni multi-session design
- [ ] Ne centralizirati widget accounting/balance brez ločenega parity audita
- [ ] Ne spreminjati validator semantics brez testov za malformed timelines
- [ ] Pred multi-session dodati session grouping design

---

## v3.0 – Work Log accounting foundation

### Zaključeno

- [x] Dodan `WorkLogAccountingCalculator`
- [x] Dodani modeli:
  - `BreakAccountingMode`
  - `WorkLogAccountingRules`
  - `WorkLogAccountingSummary`
  - `BreakAllowanceBasis`
- [x] Podprti načini obračuna odmora:
  - `UNPAID`
  - `FULLY_PAID`
  - `PAID_ALLOWANCE`
  - `EMPLOYER_POLICY_CUSTOM`
- [x] Dodani testi za:
  - unpaid break
  - fully paid break
  - paid allowance
  - sorazmerni odmor
  - prag 4h
  - active work
  - active break
  - finished totals
- [x] `WorkSettingsPrefs` podpira Break Accounting Mode
- [x] Work Log Settings UI ima `Obračun odmora`
- [x] `WorkSettingsPrefs -> WorkLogAccountingRules` mapper dodan
- [x] Dashboard saldo uporablja `WorkLogAccountingCalculator.balanceMinutes`
- [x] Privzeti mode je `UNPAID`, da obstoječe vedenje ostane nespremenjeno
- [x] `PAID_ALLOWANCE` omogoča slovenski/poštarski obračun:
  - 30 min pri 8h
  - sorazmernost
  - 4h prag
  - presežek odmora zmanjša saldo
- [x] Brez Room/schema sprememb

### Preostalo

- [ ] Notification accounting parity audit, če bo prikazoval saldo
- [ ] Recent event total accounting audit
- [ ] Advanced Work Rules UI:
  - proportional allowance ON/OFF
  - minimum threshold
  - allowance basis
  - base paid break at 8h
- [ ] Country/sector presets za Work Rules
- [ ] Pojasnilo v onboarding Work Log setup koraku

---

## v3.0 – Work Log dashboard clarity

### Zaključeno

- [x] `Total / Skupaj` preimenovan v `Effective work / Efektivno delo`
- [x] Dodan pogojni prikaz `Credited time / Priznan čas`
- [x] `Priznan čas` se prikaže samo, ko je potreben:
  - overtime/balance display enabled
  - break accounting mode ni `UNPAID`
  - credited time se razlikuje od effective work time
- [x] Active break display uporablja jasne labele:
  - `Break elapsed / Trajanje odmora`
  - `Break remaining / Preostanek odmora`
  - `Break exceeded / Prekoračen odmor`
- [x] Break value prikazuje samo trajanje, brez dodatnih besed v vrednosti
- [x] Help/settings razlaga razliko med:
  - efektivno delo
  - priznan čas
  - saldo
  - plačan/priznan odmor
  - malica/povračilo prehrane

### Preostalo

- [ ] Small-screen polish po daljšem testiranju
- [ ] Razmisliti o razširjenem accounting details bottom sheet:
  - presence
  - effective work
  - paid break credited
  - break excess
  - balance
- [ ] Ne dodajati dodatnih polj na glavno kartico brez potrebe

---

## v3.0 – Break / meal reminder foundation

### Status

Ni še implementirano. Accounting in active break display sta pripravljena osnova.

### Plan

- [ ] Reminder-first pristop
- [ ] Brez auto-end v prvem koraku
- [ ] Notification ob doseženi dolžini odmora:
  - `Odmor traja že 30 min. Zaključi odmor?`
- [ ] Auto-end samo kot kasnejša opcija
- [ ] Auto-end mora biti audit-safe
- [ ] Samodejno ustvarjen dogodek naj ima source, npr. `auto_break_end`
- [ ] Če uporabnik sam konča odmor pred timerjem, se scheduled reminder/auto-end prekliče
- [ ] Če uporabnik ročno popravi auto event, mora ostati auditiran
- [ ] Ločiti Break in Meal reminder logiko, če bo potrebno

---

## v3.0 – Multiple sessions / split shifts design

### Status

Ni še implementirano. Pripravljamo z resolver/accounting foundation.

### Zakaj je pomembno

Več sej na dan in split shifts so ena ključnih funkcionalnih vrzeli pred prihodnjo Premium verzijo.

### Plan

- [ ] Najprej design brez Room sprememb
- [ ] Odločitev: `sessionId`, `sessionIndex` ali derived grouping
- [ ] Nastavitev: `Dovoli več delovnih sej na dan`
- [ ] Default: OFF
- [ ] Po zaključenem dnevu prikaži `Začni novo sejo` samo, če je funkcija vklopljena
- [ ] Vsaka seja ima svoj Start / Finish
- [ ] Dnevni total sešteje vse seje
- [ ] Saldo uporablja accounting summary čez vse seje
- [ ] Recent events podpira več sej
- [ ] Widget podpira več sej
- [ ] Manual edit audit podpira več sej
- [ ] Future-time safety podpira več sej
- [ ] Kasneje dodati poštni split-shift profil, npr. 08:00–10:00 in 12:00–18:00

---

## v3.0 – Local backup/export foundation

### Status

Ni še implementirano.

### Zakaj je pomembno

Pred plačljivo verzijo mora imeti uporabnik občutek, da so Work Log podatki varni in prenosljivi.

### Plan

- [ ] Read-only audit obstoječih podatkov:
  - Room Work Events
  - legacy Work Logs
  - SharedPreferences
  - Cycle settings
  - Assignment/status labels
  - Work Settings
- [ ] Določiti export format:
  - CSV za Work Log events
  - JSON backup za app settings
  - PDF report kasneje
- [ ] Lokalni backup/restore pred cloud sync
- [ ] Backup mora vključevati Room + SharedPreferences
- [ ] Restore mora imeti jasna opozorila
- [ ] Export naj bo kandidat za Premium one-time

---

## v3.0 – In-app language selector

### Status

Ni še implementirano.

### Plan

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

Language selector naj ostane Free funkcija, ker podpira širitev na mednarodni trg.

---

## v3.0 – Onboarding izboljšave

### Status

Ni še implementirano za Work Log setup.

### Plan

- [ ] Dodati opcijski Work Log / time tracking setup korak
- [ ] Vprašanje: ali želi uporabnik uporabljati beleženje časa
- [ ] Če da:
  - dnevni cilj
  - default break
  - break accounting mode
  - overtime tracking
  - morebitno Work Time widget guidance
- [ ] Korak mora biti skippable
- [ ] Ne sme preobremeniti uporabnikov, ki želijo samo cikel/koledar
- [ ] Kasneje country/sector preset:
  - Slovenia general
  - Slovenia postal/courier
  - Global default

---

## Premium readiness – minimum before paid launch

Raziskava trga kaže, da je WorkCycle že močan osebni utility, vendar pred poštenim Premium odklepom potrebuje nekaj funkcij, ki jih plačljive aplikacije običajno ponujajo: več profilov/služb, split shifts, export/backup, reminders, statistiko ali widget customization.

### Minimum za Premium one-time

- [ ] Multiple active work profiles ali vsaj Work Profiles foundation
- [ ] Split shifts / multiple sessions
- [ ] Export CSV/PDF
- [ ] Lokalni backup/restore
- [ ] Osnovna statistika
- [ ] Reminders
- [ ] Widget customization
- [ ] Advanced Work Log rules

### Free naj ostane

- [ ] Osnovni delovni cikel
- [ ] Tedenski in mesečni pregled
- [ ] Osnovni statusi
- [ ] Osnovni Work Log
- [ ] Osnovni widgeti
- [ ] Language selector
- [ ] Osnovna varnost podatkov in audit-safe manual edits

### Premium one-time kandidati

- [ ] Multiple active work profiles
- [ ] Advanced Work Log rules
- [ ] Advanced statistics
- [ ] Export PDF
- [ ] Date range export
- [ ] Custom templates
- [ ] Advanced widget customization
- [ ] Multiple work sessions per day
- [ ] Split-shift profiles
- [ ] Local backup/restore

### Premium subscription kandidati

- [ ] Cloud sync
- [ ] Cloud backup
- [ ] GPS/geofencing suggestions
- [ ] Live/web sharing
- [ ] Future web companion
- [ ] Napredni reminderji, če vključujejo cloud/sync logiko

### Predlagane cene

- Founder / early tester Premium one-time: **€9,99**
- Public Premium one-time launch: **€12,99**
- Regular Premium one-time kasneje: **€14,99–€15,99**
- Cloud subscription kasneje:
  - **€1,99 / mesec**
  - **€14,99 / leto**
- Lifetime/founder bundle:
  - **€24,99–€29,99**
  - velja za offline Premium
  - cloud naj bo le časovno omejen bonus, ne “free forever”

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

- [ ] Več aktivnih delovnih profilov hkrati naj bo Premium one-time funkcija
- [ ] Ločeni urniki za vsako službo
- [ ] Ločena Work Log pravila
- [ ] Ločena statistika
- [ ] Izvoz po profilu
- [ ] Widgeti po profilu
- [ ] Jasno preklapljanje aktivnega profila, da uporabnik ne beleži časa pod napačno službo

---

## Future – Location-assisted Work Log

### Status

Future feature. Ni del osnovnega v3.0 sklopa.

### Ideja

- [ ] Uporabnik nastavi lokacijo službe / baze
- [ ] Ob prihodu aplikacija predlaga začetek dela
- [ ] Pri terenskem delu aplikacija ne predlaga konca samo zato, ker uporabnik zapusti firmo
- [ ] Pri tipu dela `Teren` ali poštarskem delu aplikacija lahko predlaga zaključek, ko se uporabnik vrne na firmo
- [ ] Opcijsko shranjevanje lokacije Work Log dogodkov:
  - začetek odmora
  - konec odmora
  - začetek/konec malice
  - zaključek dela
- [ ] Strogo opt-in
- [ ] Brez stalnega tracking-a poti
- [ ] Paziti na permission, privacy in battery
- [ ] Kandidat za Premium subscription, če se razvije kot napredna storitev

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
- [ ] Work Time widget accounting parity po stabilizaciji dashboard accounting

---

## Future – Sync / Backup

- [ ] Firebase/cloud sync med napravami
- [ ] Export/backup pred resetom ali zamenjavo službe
- [ ] Restore backup
- [ ] Sync naj bo Premium subscription kandidat
- [ ] Konfliktna pravila za Work Log podatke pri več napravah
- [ ] Brez cloud obljub v lifetime/offline Premium brez jasne omejitve

---

## Notes

- v2.9 je zaključena/released verzija.
- v3.0 je trenutni aktivni razvojni sklop.
- Work Log accounting je zdaj osrednji v3.0 foundation.
- Room schema naj ostane nespremenjena, dokler ni sprejet multi-session model.
- Transport, hospitality split shifts in healthcare 12h pravila zahtevajo posebne profile; ne hardcodati v osnovno logiko.
- Meal reimbursement / prehrana med delom je ločen koncept od break time.
- Roadmap ni pravni dokument in ne predstavlja pravnega mnenja.
