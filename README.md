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

