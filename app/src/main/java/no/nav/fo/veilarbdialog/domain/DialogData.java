package no.nav.fo.veilarbdialog.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Comparator.naturalOrder;
import static java.util.Optional.ofNullable;
import static no.nav.apiapp.util.ObjectUtils.max;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

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
    private Date venterPaSvarTidspunkt;
    private Date ferdigbehandletTidspunkt;
    private Date ubehandletTidspunkt;

    private Date sisteStatusEndring;
    private Date opprettetDato;
    private boolean historisk;

    private List<HenvendelseData> henvendelser;

    private List<EgenskapType> egenskaper;

    public List<EgenskapType> getEgenskaper() {
        return ofNullable(egenskaper).orElseGet(Collections::emptyList);
    }

    public List<HenvendelseData> getHenvendelser() {
        return ofNullable(henvendelser).orElseGet(Collections::emptyList);
    }

    public Date getUbehandletTidspunkt() {
        return ofNullable(ubehandletTidspunkt).orElseGet(() -> getHenvendelser().stream()
                .filter(HenvendelseData::fraBruker)
                .map(HenvendelseData::getSendt)
                .filter(s -> ferdigbehandletTidspunkt == null || s.after(ferdigbehandletTidspunkt))
                .min(naturalOrder())
                .orElse(ubehandletTidspunkt)
        );
    }

    public Date getSisteEndring() {
        return max(sisteStatusEndring, getHenvendelser()
                .stream()
                .map(HenvendelseData::getSendt)
                .max(naturalOrder())
                .orElse(sisteStatusEndring)
        );
    }

    public boolean erFerdigbehandlet() {
        if (ferdigbehandletTidspunkt != null) {
            return erEtterBrukerHenvendelser(ferdigbehandletTidspunkt);
        } else {
            return getUbehandletTidspunkt() == null;
        }
    }

    public boolean erUbehandlet() {
        return !erFerdigbehandlet();
    }

    public boolean venterPaSvar() {
        return erEtterBrukerHenvendelser(venterPaSvarTidspunkt);
    }

    public boolean erLestAvBruker() {
        return erEtterAlleHenvendelser(lestAvBrukerTidspunkt);
    }

    public boolean erLestAvVeileder() {
        return erEtterAlleHenvendelser(lestAvVeilederTidspunkt);
    }

    private boolean erEtterAlleHenvendelser(Date date) {
        return erEtter(date, h -> true);
    }

    private boolean erEtterBrukerHenvendelser(Date date) {
        return erEtter(date, h -> h.avsenderType == BRUKER);
    }

    private boolean erEtter(Date date, Predicate<HenvendelseData> henvendelseDataPredicate) {
        return date != null && henvendelser.stream()
                .filter(henvendelseDataPredicate)
                .noneMatch(h -> h.sendt.after(date));
    }

}

