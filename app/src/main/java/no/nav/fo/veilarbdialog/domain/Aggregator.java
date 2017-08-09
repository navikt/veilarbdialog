package no.nav.fo.veilarbdialog.domain;

import java.util.Date;
import java.util.function.Predicate;

import static java.util.Comparator.naturalOrder;
import static no.nav.apiapp.util.ObjectUtils.max;
import static no.nav.fo.veilarbdialog.domain.AvsenderType.BRUKER;

public class Aggregator {

    public static boolean erDialogLestAvVeileder(DialogData dialogData) {
        return erEtterAlleHenvendelser(dialogData.lestAvVeilederTidspunkt, dialogData);
    }

    public static boolean erDialogLestAvBruker(DialogData dialogData) {
        return erEtterAlleHenvendelser(dialogData.lestAvBrukerTidspunkt, dialogData);
    }

    public static boolean venterPaSvar(DialogData dialogData) {
        return erEtterBrukerHenvendelser(dialogData.venterPaSvarTidspunkt, dialogData);
    }

    public static boolean erFerdigbehandlet(DialogData dialogData) {
        if (dialogData.ferdigbehandletTidspunkt == null) {
            return dialogData.ubehandletTidspunkt == null;
        } else {
            return erEtterBrukerHenvendelser(dialogData.ferdigbehandletTidspunkt, dialogData);
        }
    }

    private static boolean erEtterAlleHenvendelser(Date date, DialogData dialogData) {
        return erEtter(date, dialogData, h -> true);
    }

    private static boolean erEtterBrukerHenvendelser(Date date, DialogData dialogData) {
        return erEtter(date, dialogData, h -> h.avsenderType == BRUKER);
    }

    private static boolean erEtter(Date date, DialogData dialogData, Predicate<HenvendelseData> henvendelseDataPredicate) {
        return date != null && dialogData.henvendelser.stream()
                .filter(henvendelseDataPredicate)
                .noneMatch(h -> h.sendt.after(date));
    }

    public static Date sisteEndring(DialogData dialogData) {
        Date sisteStatusEndring = dialogData.getSisteStatusEndring();
        return max(sisteStatusEndring, dialogData.getHenvendelser()
                .stream()
                .map(HenvendelseData::getSendt)
                .max(naturalOrder())
                .orElse(sisteStatusEndring)
        );
    }

}
