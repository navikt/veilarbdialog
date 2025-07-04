package no.nav.fo.veilarbdialog.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(chain = true)
public class NyMeldingDTO {

    @Schema(description = "Påkrevd. Innholdet i meldingen, brukes også når man oppretter en ny dialog-tråd", example = "Hei, jeg har et spørsmål angående min søknad.")
    private String tekst;
    @Schema(description = "DialogId (long). Hvis dialogId er null opprettes en ny dialog-tråd med 1 melding. Hvis dialogId ikke er null lages det en ny melding på den eksisterende dialog-tråden.")
    private String dialogId;
    @Schema(description = "Bruker bare hvis dialogId ikke er satt, da blir det overskriften på dialog-tråden.", example = "Oppfølging av søknad")
    private String overskrift;
    @Schema(description = "AktivitetId (long). Hvis dialog-tråden tilhører en aktivitet kan dette feltet settes.", example = "4141121")
    private String aktivitetId;
    @Schema(description = "Brukes internt av ansatte for å finne brukere (med dialog-tråder) som venter på svar fra Nav.")
    private Boolean venterPaaSvarFraNav;
    @Schema(description = "Brukes internt av ansatte for å finne dialog-tråder som venter på svar fra brukere.")
    private Boolean venterPaaSvarFraBruker;
    @Schema(description = "Egenskaper for spesielle typer dialog-meldinger (skal ikke brukes av eksterne konsumenter)", allowableValues =  {"ESKALERINGSVARSEL","PARAGRAF8"})
    private List<Egenskap> egenskaper = Collections.emptyList();
    private String fnr;

}
