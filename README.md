# Veilarbdialog
Backend applikasjon for aktivitetsplan. Tilbyr REST tjenester for å hente/opprette/endre dialoger.


### Komme i gang

```sh
mvn clean install
```

### Kontakt og spørsmål

Opprett en issue i GitHub for eventuelle spørsmål.

### Lokal kjøring
1. Legg inn testdata i `dialog.sql` (se `TestApplication`). Det opprettes en tom "dialog" med ID 123 ved oppstart.
1. Start `TestApplication.main`.
1. Denne kjører opp IBM MQ og Kafka vha. TestContainers og mocker ut noe annet.
1. Vent på "Application ready" i loggen.
1. Bruk http://localhost:8080/veilarbdialog/swagger-ui/index.html.