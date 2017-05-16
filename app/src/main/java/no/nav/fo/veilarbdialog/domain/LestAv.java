package no.nav.fo.veilarbdialog.domain;

import java.util.Date;

public class LestAv {

    public static boolean erDialogLestAvVeileder(DialogData dialogData) {
        return sjekkLestFlagg(dialogData.lestAvVeileder, dialogData);
    }

    public static boolean erDialogLestAvBruker(DialogData dialogData) {
        return sjekkLestFlagg(dialogData.lestAvBruker, dialogData);
    }

    public static boolean erHenvendelseLestAvVeileder(HenvendelseData henvendelseData, DialogData dialogData) {
        return sjekkLestFlagg(dialogData.lestAvVeileder, henvendelseData);
    }

    public static boolean erHenvendelseLestAvBruker(HenvendelseData henvendelseData, DialogData dialogData) {
        return sjekkLestFlagg(dialogData.lestAvBruker, henvendelseData);
    }

    private static boolean sjekkLestFlagg(Date leseTidspunkt, HenvendelseData henvendelseData) {
        return leseTidspunkt != null && henvendelseData.sendt.before(leseTidspunkt);
    }

    private static boolean sjekkLestFlagg(Date leseTidspunkt, DialogData dialogData) {
        return leseTidspunkt != null && dialogData.henvendelser.stream()
                .noneMatch(h -> h.sendt.after(leseTidspunkt));
    }

}
