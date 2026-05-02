# WorkCycle Roadmap

Ta dokument vsebuje načrt razvoja, backlog ideje, monetizacijsko smer in tehnične opombe za prihodnje verzije aplikacije WorkCycle.

**Trenutni status:** v2.9 je objavljen na Google Play. Aktivni razvoj poteka na **v3.0**.

**Glavna smer v3.0:** Work Log foundation, obračun delovnega časa/odmorov, stabilizacija podatkov in priprava za kasnejše premium funkcije.

---

## Roadmap načela

- Osnovna aplikacija mora ostati uporabna brez plačila.
- Premium naj najprej odklepa offline / lokalne power-user funkcije, ne pa blokirati osnovnega Work Log poteka.
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
- [x] Break Accounting Mode
- [x] Multiple work sessions / split shifts runtime support
- [x] Start new session dashboard action
- [x] Session-aware resolver, validator, accounting, widget, and notification
- [x] Dashboard/widget balance parity prek skupnega accounting layerja
- [x] Credited time / Priznan čas UI
- [x] Effective work / Efektivno delo label
- [x] Active break elapsed / remaining / exceeded display
- [x] WorkLogWidgetBalanceCalculator + testi
- [x] Work Log CSV export
- [x] Date range CSV export
- [x] Export all / Export date range CSV flow
- [x] Local full backup export foundation
- [x] ZIP + JSON backup writer
- [x] Backup manifest / payload / collector
- [x] Backup validation foundation
- [x] Backup preview model
- [x] Backup ZIP preflight validation
- [x] Premium / FeatureGate foundation
- [x] Free / Premium one-time / Subscription tier model
- [x] Tested premium feature tier mapping
- [x] Entitlement repository chain foundation
- [x] Override entitlement model for future debug/tester/founder unlocks
- [x] Debug-only SharedPreferences entitlement storage
- [x] Debug-only Premium override controls in Developer tools
- [x] PremiumProvider runtime FeatureGate chain foundation
- [x] Developer tools Premium state diagnostics
- [x] Full backup export moved to main Settings Backup card
- [x] SharedPreferences backup filtering
- [x] Manual edit audit metadata included v CSV/backup exportih
- [x] Workday rollover calculator foundation
- [x] Rollover-aware event ordering foundation
- [x] Workday rollover setting foundation

v3.0 se ne sme obravnavati kot en sam ogromen feature release. Najprej mora zaključiti Work Log foundation, nato lahko doda uporabniške funkcije nad stabilno arhitekturo.

### v3.0 cilji

- Stabilen Work Log state resolver
- Ločen accounting sloj za delovni čas, odmore in saldo
- Mednarodno prilagodljiva pravila odmora
- Jasnejši Work Log dashboard
- Podpora za več delovnih sej na dan / split shifts
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
- [ ] Ne spreminjati validator semantics brez testov za malformed timelines
- [ ] Pred sessionId/schema evolution ali recent events redesign dodati jasen session grouping design

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
- [x] Work Time widget saldo uporablja `WorkLogAccountingCalculator.balanceMinutes`
- [x] Privzeti mode je `UNPAID`, da obstoječe vedenje ostane nespremenjeno
- [x] `PAID_ALLOWANCE` omogoča slovenski/poštarski obračun:
  - 30 min pri 8h
  - sorazmernost
  - 4h prag
  - presežek odmora zmanjša saldo
- [x] Brez Room/schema sprememb

### Future – Expected start/end times by schedule layer

- [ ] Dodati nastavitev za vir pričakovanih začetkov/končanj dela:
  - Primary cycle
  - Secondary cycle / assignment layer
- [ ] Nastavitev naj bo v Work Log Settings / Work Rules kot radio group:
  - `Use primary cycle labels`
  - `Use secondary labels`
- [ ] Če je izbran Secondary layer, manjkajoča sekundarna oznaka naj varno pade nazaj na Primary cycle ali prikaže jasno opozorilo
- [ ] Pričakovani začetni in končni časi naj se nastavljajo po labelih, ne globalno samo za cel dan
- [ ] Defaulti za 3-shift template:
  - Morning: `06:00–14:00`
  - Afternoon: `14:00–22:00`
  - Night: `22:00–06:00`
- [ ] Defaulti za 2-shift template:
  - Morning: `06:00–14:00`
  - Afternoon: `14:00–22:00`
- [ ] A/B template naj ne dobi agresivnih privzetih ur, razen če template izrecno definira delovne čase
- [ ] Custom/free-form cikli naj uporabniku pustijo ročni vnos pričakovanih časov
- [ ] Expected start/end times naj uporabljajo stabilne label/template keye, kjer obstajajo, ne samo lokaliziranega display teksta
- [ ] Pri spremembi template-a naj aplikacija ponudi uporabo privzetih časov template-a, ne pa tiho prepisati uporabniških nastavitev
- [ ] Pri spremembi jezika se expected time mapping ne sme pokvariti zaradi prevedenih labelov
- [ ] Night shift / cross-midnight expected end time, kjer je end time manjši od start time, zahteva runtime overnight handling
- [ ] Ne uporabljati `22:00–06:00` za deviation računanje, dokler rollover/runtime overnight logic ni pravilno priklopljen
- [ ] Expected-time deviation logic mora uporabljati pravilno timeline datetime logiko, ne samo `LocalDate + LocalTime`
- [ ] Ročni popravki Work Log dogodkov morajo ostati audit-safe tudi pri expected-time deviation izračunih
- [ ] Widget/notification naj expected-time podatke uporabljata šele po ločenem parity auditu

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

## Future – Payroll-ready Work Log results

- [ ] Dodati dnevni števec prehrane kot ločen obračunski rezultat, ne kot navaden Work Log dogodek
- [ ] Ločiti:
  - odmor kot časovni dogodek
  - malico kot uporabniški dogodek
  - prehrano kot dnevni obračunski rezultat
- [ ] Nastavljiv prag za prehrano, npr. 4h prisotnosti
- [ ] Nastavljiv znesek ali samo oznaka “prehrana dosežena”
- [ ] Prehrana naj bo vključena v export/report, ne nujno v glavni timer
- [ ] Ne hardcodati slovenskih pravil; dodati country/sector preset kasneje

## Future – Payroll export templates

- [ ] Dodati export templates za payroll/accounting uporabo
- [ ] Začetni template: generic payroll CSV/XLSX
- [ ] Kasnejši template kandidati:
  - SAOP
  - PANTHEON
  - Minimax
  - Vasco / Birokrat
- [ ] Export naj vsebuje:
  - datum
  - prihod
  - odhod
  - efektivno delo
  - priznan čas
  - odmor
  - saldo
  - prehrana dosežena
  - opombe
  - ročni popravki / audit indicator
- [ ] Raw evidence export naj ostane ločen od payroll summary exporta
- [ ] Ne uvajati direktne payroll integracije pred stabilnim CSV/XLSX template sistemom

## Future – Work Log reminders and forgotten clock-out

- [ ] Opozorilo za pozabljeno odjavo
- [ ] Opozorilo za predolg odmor
- [ ] Opozorilo za doseženo planirano dolžino odmora
- [ ] Opozorilo pred pričakovanim koncem izmene
- [ ] Reminder-first pristop, brez samodejnega popravljanja evidence
- [ ] Auto-close naj ostane kasnejša opcija in mora biti audit-safe
- [ ] Nastavitve reminderjev naj bodo v Work Log Settings / Work Rules

## Future – End-of-day Work Log summary

- [ ] Po `Zaključi delo` prikazati kratek povzetek dneva
- [ ] Povzetek naj vsebuje:
  - prihod
  - odhod
  - efektivno delo
  - priznan čas
  - odmor/malica
  - saldo
  - prehrana dosežena / ni dosežena
  - opombe
- [ ] Uporabnik naj lahko odpre ročni popravek iz povzetka
- [ ] Ročni popravki iz povzetka morajo ostati audit-safe
- [ ] Povzetek naj ne spreminja raw dogodkov brez uporabnikove potrditve

## Future – Event-based location for Work Log

- [ ] Dodati strogo opt-in event-based location support
- [ ] Lokacijo shraniti samo ob izbranih dogodkih:
  - začetek dela
  - konec dela
  - začetek/konec odmora
  - začetek/konec malice
- [ ] Brez stalnega GPS tracking-a poti
- [ ] Če je uporabnik izven pričakovane lokacije, prikazati mehko opozorilo in možnost vnosa razloga
- [ ] Lokacijski podatki naj bodo jasno označeni in izvozljivi samo, če jih uporabnik omogoči
- [ ] GPS/geofencing suggestions naj ostanejo Premium subscription kandidat

## Future – Work Log dashboard polish

- [ ] Na vrhu Work Log dashboarda prikazati današnji kontekst:
  - današnji cikel / izmena
  - pričakovani čas
  - status prehrane
  - saldo
- [ ] Glavni CTA naj ostane zelo jasen:
  - Začni delo
  - Zaključi delo
  - Končaj odmor
- [ ] Med aktivnim delom prikazati lepljiv status z mini timerjem
- [ ] Hitre akcije naj ostanejo pod glavnim statusom:
  - Odmor
  - Malica
  - Opomba
- [ ] Ne preobremeniti dashboarda z nastavitvami ali dolgimi seznami

## v3.0 – Break / meal reminder foundation

### Status

Ni še implementirano. Accounting in active break display sta pripravljena osnova.

### Plan

- [ ] Reminder-first pristop
- [ ] Brez auto-end v prvem koraku
- [ ] Notification ob doseženi dolžini odmora
- [ ] Auto-end samo kot kasnejša opcija
- [ ] Auto-end mora biti audit-safe
- [ ] Samodejno ustvarjen dogodek naj ima source, npr. `auto_break_end`
- [ ] Če uporabnik sam konča odmor pred timerjem, se scheduled reminder/auto-end prekliče
- [ ] Če uporabnik ročno popravi auto event, mora ostati auditiran
- [ ] Ločiti Break in Meal reminder logiko, če bo potrebno

---

## v3.0 – Multiple sessions / split shifts design

### Status

Runtime support je implementiran brez Room schema sprememb. Seje so trenutno derived iz urejenih raw Work Log eventov.

### Zakaj je pomembno

Več sej na dan in split shifts so ena ključnih funkcionalnih vrzeli v Free core, ker jih veliko uporabnikov potrebuje za osnovno beleženje dela.

### Completed

- [x] Phase 0 setting: `Dovoli več delovnih sej na dan`
- [x] Default: OFF
- [x] Resolver session segmentation
- [x] Mode-aware validator
- [x] Manual edit mode wiring
- [x] Accounting session parity
- [x] Widget session parity
- [x] Dashboard `Začni novo sejo` runtime
- [x] Dnevni total sešteje vse derived seje
- [x] Saldo uporablja accounting summary čez vse derived seje

### Remaining / future

- [ ] Recent events session grouping / index
- [ ] Optional `sessionId` / schema evolution
- [ ] Richer session summary UI
- [ ] PDF/reporting awareness
- [ ] Restore/import session-aware policy
- [ ] Kasneje dodati poštni split-shift profil, npr. 08:00–10:00 in 12:00–18:00

### Notes

- [x] Split shifts / multiple sessions remain Free core
- [x] No Room schema change yet
- [x] Sessions are currently derived from ordered raw events
- [x] Runtime overnight handling is not enabled yet
- [x] Default `00:00` preserves current behavior


### Future – Overnight shifts / workday rollover

Runtime overnight handling is not enabled yet. The current setting only prepares future support.

### Completed foundation

- [x] Workday rollover calculator foundation
- [x] Rollover-aware event ordering foundation
- [x] Workday rollover setting foundation
- [x] Default: `00:00`
- [x] Optional presets: `03:00`, `04:00`, `05:00`, `06:00`

### Remaining / future

- [ ] Runtime overnight handling
- [ ] Effective work date integration
- [ ] Resolver/validator/accounting rollover integration
- [ ] Widget rollover refresh scheduling
- [ ] Events after midnight but before the rollover time can still belong to the previous work date
- [ ] Expected start/end times should support overnight shifts when expected end time is earlier than expected start time
- [ ] Do not auto-guess overnight work only from event times; use an explicit setting or clear user confirmation
- [ ] ActualDateTime / workDate schema decision
- [ ] Restore/import overnight semantics

---

## v3.0 – Local backup/export foundation

### Status

Export-only foundation je narejen. Restore/import še ne obstaja.

### Zakaj je pomembno

Pred plačljivo verzijo mora imeti uporabnik občutek, da so Work Log podatki varni in prenosljivi.

### Zaključeno

- [x] Work Log CSV export prek SAF `CreateDocument("text/csv")`
- [x] CSV export uporablja raw `WorkEvent` podatke
- [x] CSV export vključuje manual edit audit metadata
- [x] CSV export podpira `Export all`
- [x] CSV export podpira `Export date range`
- [x] Date range CSV export uporablja inclusive date range in range-specific filename
- [x] Dodani:
  - `WorkCycleBackupManifest`
  - `WorkCycleBackupPayload`
  - `WorkCycleBackupWriter`
  - `WorkCycleBackupRoomJsonMapper`
  - `WorkCycleBackupPrefsJsonMapper`
  - `WorkCycleBackupPrefsSpec`
  - `WorkCycleBackupPayloadCollector`
- [x] Full local backup export uporablja SAF `CreateDocument("application/zip")`
- [x] Full backup export je prestavljen v glavni Settings `Backup / Varnostne kopije` kartico
- [x] ZIP vsebuje:
  - `manifest.json`
  - `room/work_events.json`
  - `room/work_logs.json`
  - `prefs/*.json`
- [x] SharedPreferences backup filtering izključi:
  - debug state
  - transient UI hints
  - session snapshot state
- [x] Dodani pure Kotlin backup validation foundation razredi:
  - `WorkCycleBackupValidationResult`
  - `WorkCycleBackupPreview`
  - `WorkCycleBackupValidator`
  - `WorkCycleBackupZipReader`
  - `WorkCycleBackupJsonParser`
- [x] Backup validator preverja ZIP strukturo, manifest, required segmente, prefs segmente in audit field prisotnost
- [x] Brez Room/schema sprememb

### Preostalo

- [ ] Restore/import
- [ ] Backup validation UI
- [ ] Backup preview dialog / summary UI
- [ ] Backup summary preview
- [ ] Encryption
- [ ] Cloud sync / cloud backup
- [ ] Scheduled backup
- [ ] Premium gating
- [ ] PDF export
- [ ] Share intent
- [ ] Replace-all restore policy
- [ ] Restore/import mora uporabiti validator kot preflight korak
- [ ] Restore/import mora imeti ločen audit, validation layer, warning dialog in explicit replace-all policy
- [ ] Pred prihodnjimi Room schema spremembami omogočiti schema export in migration tests

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
- [ ] Jezik uporabiti prek sistemskih string resources, ne prek ročnega menjavanja tekstov
- [ ] Po spremembi jezika osvežiti glavne zaslone:
  - Home / Tedenski pregled
  - Calendar / Mesečni koledar
  - Settings
  - More
  - Help
  - What’s New
  - Work Log Dashboard
  - Work Log Settings
- [ ] Po spremembi jezika osvežiti widgete:
  - Work Cycle widget
  - Work Time widget
- [ ] Custom/user labels se ne prevajajo samodejno
- [ ] Uporabniško vpisana imena ciklov, sekundarnih oznak in statusov morajo ostati nespremenjena
- [ ] Built-in system labels se lokalizirajo prek stabilnih `iconKey`
- [ ] Built-in template labels se lokalizirajo prek stabilnih `templateKey` / label keyev
- [ ] Built-in cycle labels naj uporabljajo stabilne ključe in lokaliziran prikaz, ne samo shranjen display tekst
- [ ] Ob spremembi jezika naj se varno osvežijo built-in/template cycle labele, kjer aplikacija zanesljivo ve, da niso uporabniško spremenjene
- [ ] Obstoječih shranjenih tekstovnih labelov se ne sme slepo prevajati; mapirati se smejo samo znane built-in/template labele
- [ ] Pri custom/free-form ciklih aplikacija ne sme samodejno spreminjati imen ciklov
- [ ] A/B oznake lahko ostanejo nespremenjene, pomožna imena templateov pa naj se lokalizirajo, kjer so vezana na built-in keye
- [ ] Preveriti widgete po spremembi jezika, posebej:
  - Danes / Today
  - Jutri / Tomorrow
  - Prosto / Off
  - built-in cycle labels
  - built-in status labels
- [ ] Dodati regression test ali ročni smoke-test checklist za menjavo jezika

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

## v3.0 / Future – Settings UI polish

- [ ] Dodati expand/collapse možnost na vse večje Settings kartice, ne samo na Primarni cikel in Rules
- [ ] Uporabnik naj lahko razširi ali skrči posamezne sklope nastavitev:
  - Primarni cikel
  - Sekundarne oznake / statusi
  - Work Log settings
  - Work Rules
  - Widget settings
  - Backup / export
  - Developer tools
  - Premium / future features, ko bodo dodane
- [ ] Stanje razširjenih/skrčenih kartic naj se shrani lokalno v SharedPreferences
- [ ] Privzeto naj ostanejo odprti samo najpomembnejši ali pogosto uporabljeni sklopi
- [ ] Redkeje uporabljeni in napredni sklopi naj bodo privzeto skrčeni
- [ ] Ne skrivati nevarnih/destructive akcij brez jasnega naslova in opozorila
- [ ] Uporabiti enoten header UI za vse collapsible kartice:
  - naslov
  - kratek opis
  - expand/collapse ikona
- [ ] To obravnavati kot UI/UX polish, brez sprememb scheduling, Work Log accounting, backup ali premium logike

### Settings schedule cleanup

- [ ] Obdržati samo en `Active template` summary prikaz
- [ ] Prednostno obdržati zgornji summary card pod `Schedule`, da je aktiven template viden tudi, ko je `Primary cycle` zaprt
- [ ] Odstraniti podvojen `Active template` summary znotraj `Primary cycle` vsebine
- [ ] `Primary cycle` naj vsebuje samo dejanske nastavitve: izbira template-a, začetni datum, dnevi cikla in prvi cikel na začetni datum
- [ ] Pri izbiri `Cycle day on start date` ne prikazovati `1/1`, kadar se oznaka v ciklu pojavi samo enkrat
- [ ] Prikaz `1/2`, `2/2`, `1/3` itd. uporabiti samo, kadar se ista oznaka pojavi večkrat v ciklu
- [ ] To je UI cleanup brez spremembe scheduling logike


---

## Premium readiness – minimum before paid launch

Raziskava trga kaže, da je WorkCycle že močan osebni utility, vendar mora Free core ostati bogat in uporaben. Premium naj doda convenience, power-user funkcije, napreden export/reporting, profile, customization in backup orodja, ne pa blokirati bistvenega Work Log beleženja.

### Minimum za Premium one-time

- [ ] Multiple active work profiles ali vsaj Work Profiles foundation
- [ ] Export CSV/PDF
- [ ] Lokalni backup/restore
- [ ] Osnovna statistika
- [ ] Reminders
- [ ] Widget customization
- [ ] Advanced Work Log rules

### Free naj ostane

- [ ] Free core should remain generous
- [ ] Split shifts / multiple sessions are currently planned as Free core, not Premium
- [ ] Essential work logging must not be blocked behind Premium
- [ ] Osnovni delovni cikel
- [ ] Tedenski in mesečni pregled
- [ ] Osnovni statusi
- [ ] Osnovni Work Log
- [ ] Osnovni widgeti
- [ ] Language selector
- [ ] Osnovna varnost podatkov in audit-safe manual edits

### Premium one-time kandidati

- [ ] Multiple active work profiles
- [ ] Full local backup/export
- [ ] Date range CSV export
- [ ] PDF export
- [ ] Advanced Work Log rules
- [ ] Advanced statistics
- [ ] Advanced reports
- [ ] Custom templates
- [ ] Advanced widget customization
- [ ] Local backup/export
- [ ] Local backup/restore

### Premium subscription kandidati

- [ ] Cloud sync
- [ ] Cloud backup
- [ ] GPS/geofencing suggestions
- [ ] Live/web sharing
- [ ] Future web companion
- [ ] Napredni reminderji, če vključujejo cloud/sync logiko

### Opombe

- [ ] Free core should remain generous.
- [ ] Split shifts / multiple sessions are currently planned as Free core, not Premium.
- [ ] Premium should add convenience, power-user features, advanced export/reporting, profiles, customization, and backup tools, not block essential work logging.
- [x] Premium / FeatureGate foundation is in place without Play Billing or active UI gating.
- [ ] Tester/founder unlock flow
- [ ] Play Billing integration
- [ ] Premium screen
- [ ] Real entitlement storage
- [ ] Debug/tester unlock
- [ ] UI gating for specific features
- [ ] Restore purchases
- [ ] Entitlement/debug/test state must stay out of backup ZIP.
- [ ] Existing features are not gated yet.

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
- [ ] Klik na upcoming special day naj odpre mesečni koledar na izbranem datumu
- [ ] Calendar naj zna sprejeti initial/target date argument iz Home/upcoming events kartice
- [ ] Pri prehodu iz upcoming special day naj se ciljni datum vizualno označi ali vsaj izbere
- [ ] Možnost override-a prostega dne v delovni dan
- [ ] Manual mode kot premium/future feature

- [ ] Work Log start on Off day handling:
  - Če uporabnik začne delo na dnevu, ki je po urniku označen kot `Off / Prosto`, aplikacija ne sme tiho spreminjati urnika.
  - Prikazati dialog: `Ta dan je v urniku označen kot prost. Ali želiš ta dan označiti kot delovni dan?`
  - Možnosti:
    - samo začni beleženje dela
    - označi dan kot delovni dan
    - prekliči
  - Če uporabnik izbere označitev kot delovni dan, shraniti one-day schedule override za ta datum.
  - Ne spreminjati osnovnega cikla, template-a ali prihodnjih dni brez izrecne potrditve.
  - Work Log raw event mora ostati zabeležen neodvisno od tega, ali uporabnik spremeni urnik.
  - Kasneje razmisliti o posebni oznaki/statusu, npr. `Izredno delo`, `Delo na prost dan` ali `Delo na praznik`.

---

## Future – Widgets

- [ ] Prenova glavnega WorkCycle widgeta v bolj sodoben card-style prikaz
- [ ] Današnji cikel naj bo glavni poudarek z večjo barvno oznako cikla, nazivom in pričakovanim časovnim razponom
- [ ] Jutrišnji ali naslednji relevanten dan naj bo prikazan kot sekundarna vrstica za hiter pregled
- [ ] Widget naj ostane osredotočen na cikel/urnik in naj bo ločen od Work Log widgeta
- [ ] Nadaljnji size-aware layout za Work Cycle widget
- [ ] Boljši 1x1 widget:
  - compact today-only cycle view
  - brez prenatrpanih dodatnih informacij
- [ ] 2x1 widget:
  - danes + jutri
  - jasna barvna oznaka cikla
  - pričakovani čas izmene, če je nastavljen
- [ ] 2x2 in večji widget:
  - danes + prihajajoči dnevi
  - opcijsko sekundarne oznake/statusi
- [ ] Work Time widget z break/meal remaining time
- [ ] Widget color system poenotiti z aplikacijo
- [ ] Ohraniti podporo za lastne barve ciklov in prihodnji skupni sistem barv za aplikacijo in widgete
- [ ] Kasneje razmisliti o opcijskem prikazu sekundarnih oznak/statusov:
  - dopust
  - bolniška
  - teren
  - dežurstvo
  - prazniki
- [ ] Per-profile widgets, če se uvede Work Profiles
- [ ] Widget guidance po onboardingu
- [ ] Advanced widget customization:
  - izbira compact / classic / card-style widgeta
  - prikaz today-only ali danes + jutri
  - prikaz/skritje expected time
  - prikaz/skritje sekundarnih oznak
  - custom barve in style presets
- [ ] Widgeta ne preobremeniti s podatki iz evidence dela; Work Log naj ostane ločena widget izkušnja
- [ ] To obravnavati kot vizualno/UX nadgradnjo, ne kot prepis scheduling logike

## Future – Sync / Backup

- [ ] Cloud sync med napravami
- [ ] Cloud backup
- [ ] Export/backup pred resetom ali zamenjavo službe
- [ ] Restore backup
- [ ] Sync naj bo Premium subscription kandidat
- [ ] Konfliktna pravila za Work Log podatke pri več napravah
- [ ] Brez cloud obljub v lifetime/offline Premium brez jasne omejitve

---

## Future – Statistics

- [ ] Daily / weekly / monthly work totals
- [ ] Effective vs credited work breakdown
- [ ] Break usage overview
- [ ] Overtime trends
- [ ] Export-friendly statistics summaries

---

## Future – Enterprise / B2B ideas

Te funkcije niso del trenutnega v3.0/v3.1 fokusa. So dolgoročne ideje za primer, če WorkCycle kasneje preraste iz osebnega utility orodja v rešitev za manjše ekipe, podjetja ali profesionalno evidenco delovnega časa.

### Approval workflow

- [ ] Dodati možnost potrjevanja Work Log zapisov s strani nadrejenega ali odgovorne osebe
- [ ] Uporabnik zaključi dan, odgovorna oseba pa lahko zapis:
  - potrdi
  - zavrne
  - vrne v popravek
- [ ] Potrditve morajo biti audit-safe
- [ ] Ročni popravki po potrditvi morajo biti posebej označeni
- [ ] Funkcija zahteva uporabniške račune, role/permissions in verjetno sync/backend
- [ ] Ni primerno za osnovni osebni offline WorkCycle model

### Kiosk mode

- [ ] Razmisliti o kiosk načinu za skupno napravo, npr. tablico pri vhodu
- [ ] Več uporabnikov se lahko prijavi/odjavi na isti napravi
- [ ] Identifikacija uporabnika prek:
  - PIN kode
  - NFC kartice
  - QR kode
  - drugega varnega načina
- [ ] Kiosk mode je B2B funkcija in zahteva jasen admin/team model
- [ ] Ni prioriteta za osebno različico aplikacije

### NFC / Bluetooth beacon support

- [ ] Razmisliti o NFC prijavi/odjavi prek fizičnega NFC taga na lokaciji
- [ ] Razmisliti o Bluetooth beacon zaznavi za predlog začetka ali konca dela
- [ ] Uporabiti samo kot opt-in funkcijo
- [ ] NFC/beacon naj sproži predlog ali potrjen dogodek, ne tiho spreminjati evidence
- [ ] Funkcija zahteva dodatno testiranje naprav, dovoljenj, zanesljivosti in zasebnosti
- [ ] Primerna predvsem za B2B/field-service scenarije

### Biometrics

- [ ] Biometrija naj ostane samo dolgoročna opcijska B2B ideja
- [ ] Možni scenariji:
  - selfie ob prijavi
  - preverjanje identitete pri kiosk mode
  - preprečevanje prijave za drugo osebo
- [ ] Ne dodajati v osebni WorkCycle brez jasne pravne potrebe
- [ ] Zahteva ločen pravni/GDPR audit
- [ ] Zahteva jasno politiko hrambe podatkov, privolitev, varnost in možnost izbrisa
- [ ] Biometrija ne sme biti privzeto omogočena

### API / webhooks

- [ ] Razmisliti o API-ju za branje Work Log podatkov, urnikov, profilov in export rezultatov
- [ ] Razmisliti o webhookih za dogodke:
  - clock-in
  - clock-out
  - break start/end
  - manual edit
  - daily summary completed
- [ ] API/webhooks zahtevajo backend, avtentikacijo, varnost, rate limiting in verzioniranje
- [ ] Najprej narediti stabilen CSV/XLSX export template sistem
- [ ] API naj pride šele po stabilnem podatkovnem modelu, syncu in jasni premium/subscription strategiji

### Multi-company / team admin

- [ ] Razmisliti o podpori za več podjetij ali ekip
- [ ] Dodati admin/team model samo, če WorkCycle kasneje postane B2B rešitev
- [ ] Možni koncepti:
  - podjetje
  - ekipa/oddelek
  - uporabniki
  - vodje
  - pravila po ekipi
  - lokacije
  - skupinski export
- [ ] Ločiti od osebnih Work Profiles
- [ ] Osebni Work Profiles naj ostanejo bližja in enostavnejša funkcija
- [ ] Team admin zahteva backend, uporabniške račune, permissions, sync in varnostni audit

### Continuous GPS tracking

- [ ] Stalno GPS sledenje naj ostane samo dolgoročna opt-in B2B/field-service ideja
- [ ] Privzeta smer WorkCycle naj ostane event-based location, ne stalno sledenje
- [ ] Stalni GPS je primeren samo za specifične terenske scenarije, kjer obstaja jasna potreba
- [ ] Zahteva:
  - ločen privacy/GDPR audit
  - jasno uporabniško privolitev
  - nastavitev hrambe podatkov
  - možnost izklopa
  - jasen prikaz, kdaj se lokacija zbira
- [ ] Ne uporabljati stalnega GPS kot osnovne funkcije osebnega WorkCycle

### Direct ERP / payroll integrations

- [ ] Razmisliti o direktnih integracijah s payroll/ERP sistemi šele po stabilnem export template sistemu
- [ ] Možni dolgoročni kandidati:
  - SAOP
  - PANTHEON
  - Minimax
  - Vasco / Birokrat
  - drugi lokalni payroll sistemi
- [ ] Najprej zgraditi generic payroll CSV/XLSX export
- [ ] Nato dodati ponudniško prilagojene export template
- [ ] Direktna integracija naj pride šele, če obstaja jasen API, uporabniško povpraševanje in vzdržljiv maintenance model
- [ ] Ne prenašati payroll odgovornosti v WorkCycle; aplikacija naj najprej ostane audit-ready input za payroll sistem

### Enterprise notes

- [ ] Te funkcije niso pogoj za Premium one-time launch
- [ ] Te funkcije niso del Free core
- [ ] Večina teh funkcij zahteva backend/cloud/subscription model
- [ ] Vsaka funkcija mora imeti ločen product, privacy, security in technical audit
- [ ] Ne uvajati enterprise funkcij, dokler osebni WorkCycle core ni stabilen:
  - cikli
  - Work Log
  - backup/restore
  - export templates
  - reminders
  - osnovna statistika
  - več osebnih work profilov

---

## Notes

- v2.9 je zaključena/released verzija.
- v3.0 je trenutni aktivni razvojni sklop.
- Work Log accounting je zdaj osrednji v3.0 foundation.
- Room schema ni bila spremenjena med trenutnim v3.0 accounting/export foundation delom.
- Room schema naj ostane nespremenjena, dokler ni sprejeta jasna `sessionId` / schema evolution odločitev.
- Pred prihodnjimi Room schema spremembami omogočiti schema export in migration tests.
- Restore/import je bolj tvegan sklop in mora imeti ločen audit, validation layer, warning dialog in explicit replace-all policy.
- Transport, hospitality split shifts in healthcare 12h pravila zahtevajo posebne profile; ne hardcodati v osnovno logiko.
- Meal reimbursement / prehrana med delom je ločen koncept od break time.
- Roadmap ni pravni dokument in ne predstavlja pravnega mnenja.