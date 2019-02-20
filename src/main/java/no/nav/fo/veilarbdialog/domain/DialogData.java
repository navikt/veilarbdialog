package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

@Value
@Builder(toBuilder = true)
@Wither
public class DialogData {

    private long id;
    private String aktorId;
    private String overskrift;
    private String aktivitetId;

    private Date lestAvBrukerTidspunkt;
    private Date lestAvVeilederTidspunkt;
    private Date venterPaSvarFraBrukerSiden;
    private Date venterPaNavSiden;
    private Date oppdatert;

    private Date opprettetDato;
    private boolean historisk;

    private boolean harUlestParagraf8Henvendelse;
    private String paragraf8VarselUUID;

    private String kontorsperreEnhetId;

    private Date eldsteUlesteTidspunktForVeileder;
    private Date eldsteUlesteTidspunktForBruker;

    private List<HenvendelseData> henvendelser;

    private List<EgenskapType> egenskaper;

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

    public boolean venterPaSvar() {
        return venterPaSvarFraBrukerSiden != null;
    }

    public boolean erUlestForBruker() {
        return !erLestAvBruker();
    }

    public boolean erLestAvBruker() {
        return eldsteUlesteTidspunktForBruker == null;
    }

    public boolean erLestAvVeileder() {
        return eldsteUlesteTidspunktForVeileder == null;
    }

}

