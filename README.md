# Brukerflate

## Oppsett lokalt

1. Kjør `mvn clean install`
2. Start jetty-test: `web/src/test/java/no/nav/fo/StartJetty`


## Metrikker  
grafan bords:
- [funkjsonelle metrikker](https://grafana.adeo.no/dashboard/db/fo-funksjonelt)

Appen får metrikker gjennom api-app  
Følgene metrikker er lagt til spesifikt:
- Veileder  
    - dialog.veileder.ny (paaAktivitet: bool)  
    - dialog.veileder.lest (ReadTime: long(ms))  
    - henvendelse.veileder.ny (paaAktivitet: bool)  
    - dialog.veileder.oppdater.VenterSvarFraBruker (venter: bool)  
    - dialog.veileder.oppdater.ferdigbehandlet (ferdigbehandlet: bool, behandlingsTid: long(ms))
- Bruker
    - dialog.bruker.ny (paaAktivitet: bool)  
    - dialog.bruker.lest (lest: bool, ReadTime: long(ms))  
    - henvendelse.bruker.ny (paaAktivitet: bool, erSvar: bool, svartid: long(ms))  
