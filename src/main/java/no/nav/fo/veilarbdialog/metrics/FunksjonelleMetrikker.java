package no.nav.fo.veilarbdialog.metrics;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.DialogStatus;

import java.util.Date;

import static no.nav.fo.veilarbdialog.util.DateUtils.nullSafeMsSiden;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@RequiredArgsConstructor
public class FunksjonelleMetrikker {

    private final MetricsClient client;

    public void oppdaterFerdigbehandletTidspunkt(DialogData dialog, boolean ferdigBehandlet) {
        client.report(
                new Event("dialog.veileder.oppdater.ferdigbehandlet")
                        .addFieldToReport("ferdigbehandlet", ferdigBehandlet)
                        .addFieldToReport("behandlingsTid", nullSafeMsSiden(dialog.getVenterPaNavSiden()))
        );
    }

    public void markerDialogSomLestAvBruker(DialogData dialogData) {
        sendMarkerSomLestMetrikk(dialogData.getEldsteUlesteTidspunktForBruker(), "bruker");
    }

    public void markerDialogSomLestAvVeileder(DialogData dialogData) {
        sendMarkerSomLestMetrikk(dialogData.getSisteUlestAvVeilederTidspunkt(), "veileder");
    }

    public DialogData nyDialogBruker(DialogData dialogData) {
        reportDialogMedMetadata("dialog.bruker.ny", dialogData);
        return dialogData;
    }

    public void nyHenvendelseVeileder(DialogData dialog) {
        reportDialogMedMetadata("henvendelse.veileder.ny", dialog);
    }

    public void nyDialogVeileder(DialogData nyDialog) {
        reportDialogMedMetadata("dialog.veileder.ny", nyDialog);
    }

    public void oppdaterVenterSvar(DialogStatus nyStatus) {
        client.report(
                new Event("dialog.veileder.oppdater.VenterSvarFraBruker")
                        .addFieldToReport("venter", nyStatus.venterPaSvar)
        );
    }

    public void nyHenvendelseBruker(DialogData dialogData) {
        Event event = new Event("henvendelse.bruker.ny")
                .addFieldToReport("erSvar", dialogData.venterPaSvar());
        if (dialogData.getVenterPaSvarFraBrukerSiden() != null) {
            event.addFieldToReport("svartid", nullSafeMsSiden(dialogData.getVenterPaSvarFraBrukerSiden()));
        }
        event = addDialogMetadata(event, dialogData);
        client.report(event);
    }

    public void nyeVarsler(int antall, long paragraf8Varsler) {
        client.report(
                new Event("dialog.varsel")
                        .addFieldToReport("antall", antall)
                        .addFieldToReport("antallParagraf8", paragraf8Varsler)
        );
    }

    public void stoppetRevarsling(int antall) {
        client.report(
                new Event("dialog.revarsel.stoppet")
                        .addFieldToReport("antall", antall)
        );
    }

    private void reportDialogMedMetadata(String eventName, DialogData dialog) {
        client.report(addDialogMetadata(new Event(eventName), dialog));
    }

    private Event addDialogMetadata(Event event, DialogData dialog) {
        return event
                .addFieldToReport("paaAktivitet", isNotEmpty(dialog.getAktivitetId()))
                .addFieldToReport("kontorsperre", isNotEmpty(dialog.getKontorsperreEnhetId()));

    }

    private void sendMarkerSomLestMetrikk(Date eldsteUlesteTidspunkt, String lestAv) {
        client.report(
                new Event("dialog." + lestAv + ".lest")
                        .addFieldToReport("ReadTime", nullSafeMsSiden(eldsteUlesteTidspunkt))

        );
    }

}
