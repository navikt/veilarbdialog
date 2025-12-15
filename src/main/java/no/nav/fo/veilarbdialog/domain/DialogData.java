package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import no.nav.common.types.identer.EnhetId;
import no.nav.fo.veilarbdialog.kvp.NoeMedKontorEnhet;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.Optional.ofNullable;

@Value
@Builder(toBuilder = true)
@With
public class DialogData implements NoeMedKontorEnhet {
    @Nullable
    long id;
    @Nullable
    String aktorId;
    @Nullable
    String overskrift;
    @Nullable
    AktivitetId aktivitetId;

    @Nullable
    Date lestAvBrukerTidspunkt;
    @Nullable
    Date lestAvVeilederTidspunkt;
    @Nullable
    Date venterPaSvarFraBrukerSiden;
    @Nullable
    Date venterPaNavSiden;
    @Nullable
    Date oppdatert;

    @Nullable
    Date opprettetDato;
    @Nullable
    boolean historisk;

    @Nullable
    boolean harUlestParagraf8Henvendelse;
    @Nullable
    String paragraf8VarselUUID;

    @Nullable
    String kontorsperreEnhetId;

    @Nullable
    Date sisteUlestAvVeilederTidspunkt;
    @Nullable
    Date eldsteUlesteTidspunktForBruker;
    @Nullable
    UUID oppfolgingsperiode;

    @Nullable
    List<HenvendelseData> henvendelser;
    @Nullable
    List<EgenskapType> egenskaper;
    @Nullable
    public List<EgenskapType> getEgenskaper() {
        return ofNullable(egenskaper).orElseGet(Collections::emptyList);
    }

    @Nullable
    public List<HenvendelseData> getHenvendelser() {
        return ofNullable(henvendelser).orElseGet(Collections::emptyList);
    }
    @Nullable
    public boolean erFerdigbehandlet() {
        return venterPaNavSiden == null;
    }
    @Nullable
    public boolean erUbehandlet() {
        return !erFerdigbehandlet();
    }
    @Nullable
    public boolean venterPaSvarFraBruker() {
        return venterPaSvarFraBrukerSiden != null;
    }
    @Nullable
    public boolean erUlestForBruker() {
        return !erLestAvBruker();
    }
    @Nullable
    public boolean erLestAvBruker() {
        return eldsteUlesteTidspunktForBruker == null;
    }
    @Nullable
    public boolean erNyesteHenvendelseLestAvVeileder() {
        return sisteUlestAvVeilederTidspunkt == null;
    }
    @Nullable
    public boolean erUlestAvVeileder() {
        return !erNyesteHenvendelseLestAvVeileder();
    }
    @Nullable
    @Override
    public Optional<EnhetId> getKontorEnhet() {
        return Optional.ofNullable(getKontorsperreEnhetId()).map(EnhetId::of);
    }
}

