package no.nav.fo.veilarbdialog.db.dao;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.domain.*;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.DialogStatus.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogFeedDAOTest extends IntegrasjonsTest {
    private static final String AKTOR_ID = "1234";

    @Inject
    private DialogDAO dialogDAO;

    @Inject
    private DialogFeedDAO dialogFeedDAO;

    @Test
    public void hentAktorerMedEndringerEtter_nyDialog_aktorEndret() {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);
        Date omEttSekund = new Date(System.currentTimeMillis() + 1000L);

        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500)).isEmpty();
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(omEttSekund, 500)).isEmpty();

        opprettNyDialog(AKTOR_ID);
        opprettNyDialog("5678");

        updateDialogAktorFor(AKTOR_ID);
        updateDialogAktorFor("5678");

        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500)).hasSize(2);
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(omEttSekund, 500)).isEmpty();
    }

    @Test
    public void hentAktorerMedEndringerEtter_statusPaaFeedskalTaHensynTilAlleAktorensDialoger() throws InterruptedException {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);

        DialogData nyDialog = nyDialog(AKTOR_ID);
        long dialogId = dialogDAO.opprettDialog(nyDialog);
        dialogDAO.oppdaterVentePaSvarTidspunkt(new DialogStatus(dialogId, true, false));
        dialogDAO.oppdaterFerdigbehandletTidspunkt(new DialogStatus(dialogId, false, false));
        updateDialogAktorFor(AKTOR_ID);

        List<DialogAktor> endringerForEttSekundSiden = dialogFeedDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500);
        assertThat(endringerForEttSekundSiden).hasSize(1);
        Date tidspunktEldsteVentende = endringerForEttSekundSiden.get(0).getTidspunktEldsteVentende();
        Date ubehandletTidspunkt = endringerForEttSekundSiden.get(0).getTidspunktEldsteUbehandlede();
        assertThat(tidspunktEldsteVentende).isNotNull();
        assertThat(ubehandletTidspunkt).isNotNull();

        Thread.sleep(1);
        Date forrigeLeseTidspunkt = new Date();
        dialogDAO.opprettDialog(nyDialog(AKTOR_ID));
        updateDialogAktorFor(AKTOR_ID);

        List<DialogAktor> endringerEtterForrigeLesetidspunkt = dialogFeedDAO.hentAktorerMedEndringerFOM(forrigeLeseTidspunkt, 500);
        assertThat(endringerEtterForrigeLesetidspunkt).hasSize(1);
        Date nyttTidspunktEldsteVentende = endringerEtterForrigeLesetidspunkt.get(0).getTidspunktEldsteVentende();
        Date nyttUbehandletTidspunkt = endringerEtterForrigeLesetidspunkt.get(0).getTidspunktEldsteUbehandlede();
        assertThat(nyttTidspunktEldsteVentende).isEqualTo(tidspunktEldsteVentende);
        assertThat(nyttUbehandletTidspunkt).isEqualTo(nyttUbehandletTidspunkt);

    }

    @Test
    public void hentAktorerMedEndringerFOM_oppdaterDialogStatusOgNyHenvendelse_riktigStatus() {
        long dialogId = opprettNyDialog(AKTOR_ID);

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.values()[0]);
        dialogDAO.opprettHenvendelse(henvendelseData);

        Date forForsteStatusOppdatering = uniktTidspunkt();
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(forForsteStatusOppdatering, 500)).isEmpty();

        DialogStatus.DialogStatusBuilder dialogStatusBuilder = builder().dialogId(dialogId);

        dialogDAO.oppdaterVentePaSvarTidspunkt(dialogStatusBuilder
                .venterPaSvar(true)
                .build()
        );

        updateDialogAktorFor(AKTOR_ID);

        Date etterForsteStatusOppdatering = uniktTidspunkt();
        DialogAktor etterForsteOppdatering = hentAktorMedEndringerEtter(forForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.sisteEndring).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteUbehandlede).isBefore(forForsteStatusOppdatering);

        Date forAndreStatusOppdatering = uniktTidspunkt();
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(forAndreStatusOppdatering, 500)).isEmpty();
        dialogDAO.oppdaterFerdigbehandletTidspunkt(dialogStatusBuilder
                .ferdigbehandlet(true)
                .build()
        );
        updateDialogAktorFor(AKTOR_ID);

        uniktTidspunkt();

        DialogAktor etterAndreOppdatering = hentAktorMedEndringerEtter(forAndreStatusOppdatering);
        assertThat(etterAndreOppdatering.sisteEndring).isBetween(forAndreStatusOppdatering, uniktTidspunkt());
        assertThat(etterAndreOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterAndreOppdatering.tidspunktEldsteUbehandlede).isNull();

        Date forNyHenvendelse = uniktTidspunkt();
        dialogDAO.opprettHenvendelse(nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.values()[0]));
        updateDialogAktorFor(AKTOR_ID);

        DialogAktor etterNyHenvenselse = hentAktorMedEndringerEtter(forNyHenvendelse);
        assertThat(etterNyHenvenselse.sisteEndring).isBetween(forNyHenvendelse, uniktTidspunkt());
        assertThat(etterNyHenvenselse.tidspunktEldsteVentende).isNull();
        assertThat(etterNyHenvenselse.tidspunktEldsteUbehandlede).isBetween(forNyHenvendelse, uniktTidspunkt());
    }

    private DialogAktor hentAktorMedEndringerEtter(Date tidspunkt) {
        List<DialogAktor> endredeAktorer = dialogFeedDAO.hentAktorerMedEndringerFOM(tidspunkt, 500);
        assertThat(endredeAktorer).hasSize(1);
        return endredeAktorer.get(0);
    }

    @SneakyThrows
    private Date uniktTidspunkt() {
        sleep(1);
        Date tidspunkt = new Date();
        sleep(1);
        return tidspunkt;
    }

    private long opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    public void updateDialogAktorFor(String aktorId){
        val dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        dialogFeedDAO.updateDialogAktorFor(aktorId, dialoger);
    }
}