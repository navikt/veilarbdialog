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

public class FunksjonelleMetrikker {

    public static void oppdaterFerdigbehandletTidspunkt(DialogData dialog, DialogStatus dialogStatus) {
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

    public static void markerDialogSomLestAvBruker(DialogData dialogData) {
        Optional<Long> readTime = tidSidenSisteMeldingSom(dialogData, erIkkeLestAvBruker());
        readTime.ifPresent(FunksjonelleMetrikker::sendMarkerSomLestAvBruker);
    }

    public static void markerDialogSomLestAvVeileder(DialogData dialogData) {
        Optional<Long> readTime = tidSidenSisteMeldingSom(dialogData, erIkkeLestAvVeileder());
        readTime.ifPresent(FunksjonelleMetrikker::sendMarkerSomLestAvVeileder);
    }

    public static DialogData nyDialogBruker(DialogData dialogData) {
        reportDialogMedMetadata("dialog.bruker.ny", dialogData);
        return dialogData;
    }

    public static void nyHenvedelseVeileder(DialogData dialogMedNyHenvendelse) {
        reportDialogMedMetadata("henvendelse.veileder.ny", dialogMedNyHenvendelse);
    }

    public static void nyDialogVeileder(DialogData nyDialog) {
        reportDialogMedMetadata("dialog.veileder.ny", nyDialog);
    }

    public static void oppdaterVenterSvar(DialogStatus nyStatus, DialogData eksisterendeData) {
        if(nyStatus.venterPaSvar == eksisterendeData.venterPaSvar())
            return;

        MetricsFactory
                .createEvent("dialog.veileder.oppdater.VenterSvarFraBruker")
                .addFieldToReport("venter", nyStatus.venterPaSvar)
                .setSuccess()
                .report();
    }

    public static DialogData nyHenvendelseBruker(DialogData dialogData) {
        Event event = MetricsFactory.createEvent("henvendelse.bruker.ny");
        event = addDialogMetadata(event, dialogData);

        boolean svartPaa = isSvartPaa(dialogData);
        event.addFieldToReport("erSvar", svartPaa);
        if (svartPaa) {
            event.addFieldToReport("svartid", msSiden(dialogData.getVenterPaSvarTidspunkt()));
        }

        event.report();
        return dialogData;
    }

    private static Event addDialogMetadata(Event event, DialogData dialog) {
        return event
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialog.getAktivitetId()))
                .addFieldToReport("kontorsperret", notNullOrEmpty(dialog.getKontorsperreEnhetId()));

    }

    private static void reportDialogMedMetadata(String eventName, DialogData dialog) {
        Event event = MetricsFactory.createEvent(eventName);
        event = addDialogMetadata(event, dialog);
        event.setSuccess();
        event.report();
    }

    private static boolean isSvartPaa(DialogData dialogData) {
        Date venterPaSvarTidspunkt = dialogData.getVenterPaSvarTidspunkt();
        if (venterPaSvarTidspunkt == null)
            return false;

        List collect = dialogData.getHenvendelser().stream()
                .filter(h -> h.getAvsenderType() == AvsenderType.BRUKER)
                .filter(h -> venterPaSvarTidspunkt.before(h.getSendt()))
                .collect(Collectors.toList());

        return collect.size() == 1;
    }

    private static Optional<Long> tidSidenSisteMeldingSom(DialogData dialogData, Predicate<HenvendelseData> ikkeLestAv) {
        return dialogData.getHenvendelser().stream()
                .filter(ikkeLestAv)
                .map(HenvendelseData::getSendt)
                .min(naturalOrder())
                .map(DateUtils::msSiden);
    }

    private static void sendMarkerSomLestAvBruker(Long time) {
        sendMarkerSomLestMetrikk(time, "bruker");
    }

    private static void sendMarkerSomLestAvVeileder(Long time) {
        sendMarkerSomLestMetrikk(time, "veileder");
    }

    private static Predicate<HenvendelseData> erIkkeLestAvBruker() {
        return h -> h.fraVeileder() && !h.lestAvBruker;
    }

    private static Predicate<HenvendelseData> erIkkeLestAvVeileder() {
        return h -> h.fraBruker() && !h.lestAvVeileder;
    }

    private static void sendMarkerSomLestMetrikk(Long time, String lestAv){
        MetricsFactory
                .createEvent("dialog."+ lestAv + ".lest")
                .addFieldToReport("ReadTime", time)
                .report();
    }

    private FunksjonelleMetrikker() {
    }
}
