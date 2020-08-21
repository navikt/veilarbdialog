package no.nav.fo.veilarbdialog.util;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;

import java.util.Date;

import static no.nav.fo.veilarbdialog.util.DateUtils.nullSafeMsSiden;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class FunksjonelleMetrikker {

    private static final MetricsClient client = new InfluxClient();

    public static void oppdaterFerdigbehandletTidspunkt(DialogData dialog, DialogStatus dialogStatus) {
        client.report(
                new Event("dialog.veileder.oppdater.ferdigbehandlet")
                        .addFieldToReport("ferdigbehandlet", dialogStatus.ferdigbehandlet)
                        .addFieldToReport("behandlingsTid", nullSafeMsSiden(dialog.getVenterPaNavSiden()))
        );
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
        client.report(
                new Event("dialog.veileder.oppdater.VenterSvarFraBruker")
                        .addFieldToReport("venter", nyStatus.venterPaSvar)
        );
    }

    public static void nyHenvendelseBruker(DialogData dialogData) {
        Event event = new Event("henvendelse.bruker.ny")
                .addFieldToReport("erSvar", dialogData.venterPaSvar());
        if (dialogData.getVenterPaSvarFraBrukerSiden() != null) {
            event.addFieldToReport("svartid", nullSafeMsSiden(dialogData.getVenterPaSvarFraBrukerSiden()));
        }
        event = addDialogMetadata(event, dialogData);
        client.report(event);
    }

    public static void nyeVarsler(int antall, long paragraf8Varsler) {
        client.report(
                new Event("dialog.varsel")
                        .addFieldToReport("antall", antall)
                        .addFieldToReport("antallParagraf8", paragraf8Varsler)
        );
    }

    public static void stoppetRevarsling(int antall) {
        client.report(
                new Event("dialog.revarsel.stoppet")
                        .addFieldToReport("antall", antall)
        );
    }

    private static void reportDialogMedMetadata(String eventName, DialogData dialog) {
        client.report(addDialogMetadata(new Event(eventName), dialog));
    }

    private static Event addDialogMetadata(Event event, DialogData dialog) {
        return event
                .addFieldToReport("paaAktivitet", isNotEmpty(dialog.getAktivitetId()))
                .addFieldToReport("kontorsperre", isNotEmpty(dialog.getKontorsperreEnhetId()));

    }

    private static void sendMarkerSomLestMetrikk(Date eldsteUlesteTidspunkt, String lestAv) {
        client.report(
                new Event("dialog." + lestAv + ".lest")
                        .addFieldToReport("ReadTime", nullSafeMsSiden(eldsteUlesteTidspunkt))

        );
    }

}
