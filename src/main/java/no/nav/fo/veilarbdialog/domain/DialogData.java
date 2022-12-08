package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

import java.util.*;

import static java.util.Optional.ofNullable;

@Value
@Builder(toBuilder = true)
@With
public class DialogData {

    long id;
    String aktorId;
    String overskrift;
    AktivitetId aktivitetId;

    Date lestAvBrukerTidspunkt;
    Date lestAvVeilederTidspunkt;
    Date venterPaSvarFraBrukerSiden;
    Date venterPaNavSiden;
    Date oppdatert;

    Date opprettetDato;
    boolean historisk;

    boolean harUlestParagraf8Henvendelse;
    String paragraf8VarselUUID;

    String kontorsperreEnhetId;

    Date sisteUlestAvVeilederTidspunkt;
    Date eldsteUlesteTidspunktForBruker;
    UUID oppfolgingsperiode;

    List<HenvendelseData> henvendelser;

    List<EgenskapType> egenskaper;

    public List<EgenskapType> getEgenskaper() {
        return ofNullable(egenskaper).orElseGet(Collections::emptyList);
    }

    public List<HenvendelseData> getHenvendelser() {
        return ofNullable(henvendelser).orElseGet(Collections::emptyList);
    }

    public boolean erFerdigbehandlet() {
        return venterPaNavSiden == null;
    }

    public boolean erUbehandlet() {
        return !erFerdigbehandlet();
    }

    public boolean venterPaSvarFraBruker() {
        return venterPaSvarFraBrukerSiden != null;
    }

    public boolean erUlestForBruker() {
        return !erLestAvBruker();
    }

    public boolean erLestAvBruker() {
        return eldsteUlesteTidspunktForBruker == null;
    }

    public boolean erNyesteHenvendelseLestAvVeileder() {
        return sisteUlestAvVeilederTidspunkt == null;
    }

    public boolean erUlestAvVeileder() {
        return !erNyesteHenvendelseLestAvVeileder();
    }
}

