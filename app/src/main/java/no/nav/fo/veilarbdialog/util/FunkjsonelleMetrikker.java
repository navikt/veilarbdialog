package no.nav.fo.veilarbdialog.util;

import no.nav.fo.veilarbdialog.domain.AvsenderType;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static no.nav.fo.veilarbdialog.util.DateUtils.msSiden;
import static no.nav.fo.veilarbdialog.util.StringUtils.notNullOrEmpty;

public class FunkjsonelleMetrikker {

    public static void oppdaterFerdigbehandletTidspunktMetrikk(DialogData dialog, DialogStatus dialogStatus) {
        if(dialog.erFerdigbehandlet() == dialogStatus.ferdigbehandlet)
            return;

        Event event = MetricsFactory
                .createEvent("dialog.veileder.oppdater.ferdigbehandlet")
                .addFieldToReport("ferdigbehandlet", dialogStatus.ferdigbehandlet);

        Date ubehandletTidspunkt = dialog.getUbehandletTidspunkt();
        if (dialogStatus.ferdigbehandlet && ubehandletTidspunkt != null) {
            event.addFieldToReport("behandlingsTid", msSiden(ubehandletTidspunkt));
        }
        event.report();
    }

    public static void merkDialogSomLestAvBrukerMetrikk(DialogData dialogData) {
        Optional<Long> readTime = tidSidenSiteMeldingSom(dialogData, erIkkeLestAvBruker());
        readTime.ifPresent(FunkjsonelleMetrikker::sendMarkerSomLestAvBrukerMetrikk);
    }

    public static void markerSomLestAvVeilederMetrikk(DialogData dialogData) {
        Optional<Long> readTime = tidSidenSiteMeldingSom(dialogData, erIkkeLestAvVeileder());
        readTime.ifPresent(FunkjsonelleMetrikker::sendMarkerSomLestAvVeilederMetrikk);
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

    public static void oppdaterVenterSvarMetrikk(DialogStatus nyStatus, DialogData eksisterendeData) {
        if(nyStatus.venterPaSvar == eksisterendeData.venterPaSvar())
            return;

        MetricsFactory
                .createEvent("dialog.veileder.oppdater.VenterSvarFraBruker")
                .addFieldToReport("venter", nyStatus.venterPaSvar)
                .setSuccess()
                .report();
    }

    public static DialogData nyHenvendelseBrukerMetrikk(DialogData dialogData) {
        Event event = MetricsFactory
                .createEvent("henvendelse.bruker.ny")
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialogData.getAktivitetId()));

        boolean svartPaa = isSvartpaa(dialogData);
        event.addFieldToReport("erSvar", svartPaa);
        if (svartPaa) {
            event.addFieldToReport("svartid", msSiden(dialogData.getVenterPaSvarTidspunkt()));
        }

        event.report();
        return dialogData;
    }

    private static void reportMetrikMedPaaAktivitet(String eventName, DialogData dialog){
        MetricsFactory.createEvent(eventName)
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialog.getAktivitetId()))
                .setSuccess()
                .report();
    }

    private static boolean isSvartpaa(DialogData dialogData) {
        Date venterPaSvarTidspunkt = dialogData.getVenterPaSvarTidspunkt();
        if (venterPaSvarTidspunkt == null)
            return false;

        List collect = dialogData.getHenvendelser().stream()
                .filter(h -> h.getAvsenderType() == AvsenderType.BRUKER)
                .filter(h -> venterPaSvarTidspunkt.before(h.getSendt()))
                .collect(Collectors.toList());

        return collect.size() == 1;
    }

    private static Optional<Long> tidSidenSiteMeldingSom(DialogData dialogData, Predicate<HenvendelseData> ikkeLestAv) {
        return dialogData.getHenvendelser().stream()
                .filter(ikkeLestAv)
                .map(HenvendelseData::getSendt)
                .min(naturalOrder())
                .map(DateUtils::msSiden);
    }

    private static void sendMarkerSomLestAvBrukerMetrikk(Long time) {
        sendMarkerSomLesMetrikk(time, "bruker");
    }

    private static void sendMarkerSomLestAvVeilederMetrikk(Long time) {
        sendMarkerSomLesMetrikk(time, "veileder");
    }

    private static Predicate<HenvendelseData> erIkkeLestAvBruker() {
        return h -> h.fraVeileder() && !h.lestAvBruker;
    }

    private static Predicate<HenvendelseData> erIkkeLestAvVeileder() {
        return h -> h.fraBruker() && !h.lestAvVeileder;
    }

    private static void sendMarkerSomLesMetrikk(Long time, String lestAv){
        MetricsFactory
                .createEvent("dialog."+ lestAv + ".lest")
                .addFieldToReport("ReadTime", time)
                .report();
    }

    private FunkjsonelleMetrikker() {
    }
}
