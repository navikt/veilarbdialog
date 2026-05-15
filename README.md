# Veilarbdialog
Backend applikasjon for arbeidsrettet-dialog. Tilbyr REST tjenester for å hente/opprette/endre dialoger.

### Komme i gang

```sh
mvn clean install
```

### Kontakt og spørsmål

Opprett en issue i GitHub for eventuelle spørsmål.

### Lokal kjøring
1. Legg inn dine egne testdata (hvis du har noen) i `dialog.sql` (se `TestApplication`). Det opprettes en tom "dialog" med ID 123 ved oppstart.
2. Start `TestApplication.main`.
3. Vent på "Application ready" i loggen.
4. Bruk http://localhost:8080/veilarbdialog/swagger-ui/index.html.

## Dokumentasjon for håndtering av dialog-statuser 
Sist oppdatert 12.05.26

### Ny melding (inkl ny dialogtråd) fra veileder
- Insert `NY_HENVENDELSE_FRA_VEILEDER` i EVENT tabell
- Sett ferdigBehandlet til true (`false` når den gjøres om til `Venter på svar fra Nav`)
  - Ferdigbehandlet finnes ikke i databasen, bare feltet `VENTER_PA_NAV_SIDEN`
  - Blir kun oppdatert hvis tråden ikke allerede er satt til ferdigBehandlet
    - Å sette venterPaNav til `false` betyr å sette VENTER_PA_NAV_SIDEN til null
    - Å sette venterPaNav til `true` betyr å sette VENTER_PA_NAV_SIDEN til nå-tidspunkt
- Sett **eldste** uleste for bruker
  - Bruk tidspunkt fra innkommende melding hvis hele tråden er leset av bruker
  - Bruk eksisterende eldste ulest tidspunkt hvis ikke

### Ny melding (inkl ny dialogtråd) fra bruker
- Insert `NY_HENVENDELSE_FRA_BRUKER` i `EVENT` tabell
- Insert eventer `VENTER_PAA_NAV` og `BESVART_AV_BRUKER` basert på om tilstanden til dialog-tråden er endret
- Sett venterPåNavSiden til innkommende melding tidspunkt hvis ferdigBehandlet er true (venterPaaSvarFraNav == false)
- Sett **nyeste** ulest av veileder

## Ting å passe på når man jobber med denne koden
- `ferdigBehandlet` er `venterPaaSvarFraNav` fra DTO men invertert
- `ferdigBehandlet` er `venterPaNavSiden` (fra databasen) == null
- Koden holder styr på **eldste uleste** for brukere men **nyeste uleste** for veiledere
- "Funksjonelle metrikker" funker ikke etter at vi gikk over til GCP