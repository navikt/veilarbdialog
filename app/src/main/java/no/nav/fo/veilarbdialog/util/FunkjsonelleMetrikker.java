package no.nav.fo.veilarbdialog.util;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.metrics.MetricsFactory;

import static no.nav.fo.veilarbdialog.util.DateUtils.msSiden;
import static no.nav.fo.veilarbdialog.util.DateUtils.nullSafeMsSiden;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;

public class FunkjsonelleMetrikker {
    private FunkjsonelleMetrikker() {
    }

    public static void oppdaterFerdigbehandletTidspunktMetrikk(DialogData dialog, DialogStatus dialogStatus) {
        MetricsFactory
                .createEvent("dialog.veileder.oppdater.ferdigbehandlet")
                .addFieldToReport("ferdigbehandlet", dialogStatus.ferdigbehandlet)
                .addFieldToReport("behandlingsTid", nullSafeMsSiden(dialog.getVenterPaNavSiden()))
                .report();
    }

    public static void merkDialogSomLestAvBrukerMetrikk(DialogData dialogData) {
        long ms = msSiden(dialogData.getEldsteUlesteTidspunktForBruker());
        sendMarkerSomLesMetrikk(ms, "bruker");
    }

    public static void markerSomLestAvVeilederMetrikk(DialogData dialogData) {
        long ms = msSiden(dialogData.getEldsteUlesteTidspunktForVeileder());
        sendMarkerSomLesMetrikk(ms, "veileder");
    }

    public static DialogData nyDialogBrukerMetrikk(DialogData dialogData) {
        reportMetrikMedPaaAktivitet("dialog.bruker.ny", dialogData);
        return dialogData;
    }

    public static void nyHenvedelseVeilederMetrikk(DialogData dialogMedNyHenvendelse) {
        reportMetrikMedPaaAktivitet("henvendelse.veileder.ny", dialogMedNyHenvendelse);
    }

    public static void nyDialogVeilederMetrikk(DialogData nyDialog) {
        reportMetrikMedPaaAktivitet("dialog.veileder.ny", nyDialog);
    }

    public static void oppdaterVenterSvarMetrikk(DialogStatus nyStatus) {
        MetricsFactory
                .createEvent("dialog.veileder.oppdater.VenterSvarFraBruker")
                .addFieldToReport("venter", nyStatus.venterPaSvar)
                .report();
    }

    public static void nyHenvendelseBrukerMetrikk(DialogData dialogData) {
        MetricsFactory
                .createEvent("henvendelse.bruker.ny")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialogData.getAktivitetId()))
                .addFieldToReport("erSvar", dialogData.venterPaSvar())
                .addFieldToReport("svartid", nullSafeMsSiden(dialogData.getVenterPaSvarFraBrukerSiden()))
                .report();
    }

    private static void reportMetrikMedPaaAktivitet(String eventName, DialogData dialog) {
        MetricsFactory.createEvent(eventName)
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialog.getAktivitetId()))
                .report();
    }

    private static void sendMarkerSomLesMetrikk(Long time, String lestAv) {
        MetricsFactory
                .createEvent("dialog." + lestAv + ".lest")
                .addFieldToReport("ReadTime", time)
                .report();
    }
}
