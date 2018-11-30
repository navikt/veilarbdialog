package no.nav.fo.veilarbdialog.util;

import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import java.util.Date;

import static no.nav.apiapp.util.StringUtils.notNullOrEmpty;
import static no.nav.fo.veilarbdialog.util.DateUtils.nullSafeMsSiden;

public class FunksjonelleMetrikker {

    public static void oppdaterFerdigbehandletTidspunkt(DialogData dialog, DialogStatus dialogStatus) {
        MetricsFactory
                .createEvent("dialog.veileder.oppdater.ferdigbehandlet")
                .addFieldToReport("ferdigbehandlet", dialogStatus.ferdigbehandlet)
                .addFieldToReport("behandlingsTid", nullSafeMsSiden(dialog.getVenterPaNavSiden()))
                .report();
    }

    public static void markerDialogSomLestAvBruker(DialogData dialogData) {
        sendMarkerSomLestMetrikk(dialogData.getEldsteUlesteTidspunktForBruker(), "bruker");
    }

    public static void markerDialogSomLestAvVeileder(DialogData dialogData) {
        sendMarkerSomLestMetrikk(dialogData.getEldsteUlesteTidspunktForVeileder(), "veileder");
    }

    public static DialogData nyDialogBruker(DialogData dialogData) {
        reportDialogMedMetadata("dialog.bruker.ny", dialogData);
        return dialogData;
    }

    public static void nyHenvendelseVeileder(DialogData dialog) {
        reportDialogMedMetadata("henvendelse.veileder.ny", dialog);
    }

    public static void nyDialogVeileder(DialogData nyDialog) {
        reportDialogMedMetadata("dialog.veileder.ny", nyDialog);
    }

    public static void oppdaterVenterSvar(DialogStatus nyStatus) {
        MetricsFactory
                .createEvent("dialog.veileder.oppdater.VenterSvarFraBruker")
                .addFieldToReport("venter", nyStatus.venterPaSvar)
                .report();
    }

    public static void nyHenvendelseBruker(DialogData dialogData) {
        Event event = MetricsFactory
                .createEvent("henvendelse.bruker.ny")
                .addFieldToReport("erSvar", dialogData.venterPaSvar())
                .addFieldToReport("svartid", nullSafeMsSiden(dialogData.getVenterPaSvarFraBrukerSiden()));
        event = addDialogMetadata(event, dialogData);
        event.report();
    }

    public static void nyeVarsler(int antall, long paragraf8Varsler) {
        MetricsFactory.createEvent("dialog.varsel")
                .addFieldToReport("antall", antall)
                .addFieldToReport("antallParagraf8", paragraf8Varsler)
                .report();
    }

    public static void paragraf8Varsel() {
        MetricsFactory.createEvent("dialog.paragraf8.varsel")
                .report();
    }

    public static void stoppetRevarsling(int antall) {
        MetricsFactory.createEvent("dialog.revarsel.stoppet")
                .addFieldToReport("antall", antall)
                .report();
    }

    private static void reportDialogMedMetadata(String eventName, DialogData dialog) {
        Event event = MetricsFactory.createEvent(eventName);
        event = addDialogMetadata(event, dialog);
        event.report();
    }

    private static Event addDialogMetadata(Event event, DialogData dialog) {
        return event
                .addFieldToReport("paaAktivitet", notNullOrEmpty(dialog.getAktivitetId()))
                .addFieldToReport("kontorsperre", notNullOrEmpty(dialog.getKontorsperreEnhetId()));

    }

    private static void sendMarkerSomLestMetrikk(Date eldsteUlesteTidspunkt, String lestAv) {
        MetricsFactory
                .createEvent("dialog." + lestAv + ".lest")
                .addFieldToReport("ReadTime", nullSafeMsSiden(eldsteUlesteTidspunkt))
                .report();
    }
}
