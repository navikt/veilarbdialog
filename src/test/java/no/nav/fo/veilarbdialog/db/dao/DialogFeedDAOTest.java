package no.nav.fo.veilarbdialog.db.dao;

import lombok.val;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.service.DialogStatusService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static no.nav.fo.IntegationTest.uniktTidspunkt;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyDialog;
import static no.nav.fo.veilarbdialog.TestDataBuilder.nyHenvendelse;
import static no.nav.fo.veilarbdialog.domain.DialogStatus.builder;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@Sql("/db/testdata/slett_alle_dialoger.sql") // Workaround for some other test that isn't properly transactional.
public class DialogFeedDAOTest {
    private static final String AKTOR_ID = "1234";

    @Autowired
    private DialogDAO dialogDAO;

    @Autowired
    private DialogStatusService dialogStatusService;

    @Autowired
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
    public void hentAktorerMedEndringerEtter_statusPaaFeedskalTaHensynTilAlleAktorensDialoger() {
        Date ettSekundSiden = new Date(System.currentTimeMillis() - 1000L);

        DialogData nyDialog = nyDialog(AKTOR_ID);
        DialogData dialogData = dialogDAO.opprettDialog(nyDialog);
        long dialogId = dialogData.getId();
        DialogData opdatert1 = dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, new DialogStatus(dialogId, true, false));
        dialogStatusService.oppdaterVenterPaNavSiden(opdatert1, new DialogStatus(dialogId, false, false));
        updateDialogAktorFor(AKTOR_ID);

        List<DialogAktor> endringerForEttSekundSiden = dialogFeedDAO.hentAktorerMedEndringerFOM(ettSekundSiden, 500);
        assertThat(endringerForEttSekundSiden).hasSize(1);
        Date tidspunktEldsteVentende = endringerForEttSekundSiden.get(0).getTidspunktEldsteVentende();
        Date ubehandletTidspunkt = endringerForEttSekundSiden.get(0).getTidspunktEldsteUbehandlede();
        assertThat(tidspunktEldsteVentende).isNotNull();
        assertThat(ubehandletTidspunkt).isNotNull();

        Date forrigeLeseTidspunkt = uniktTidspunkt();
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
        DialogData dialogData = opprettNyDialog(AKTOR_ID);
        long dialogId = dialogData.getId();

        HenvendelseData henvendelseData = nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.BRUKER);

        dialogDAO.opprettHenvendelse(henvendelseData);
        dialogStatusService.nyHenvendelse(dialogData, henvendelseData);

        Date forForsteStatusOppdatering = uniktTidspunkt();
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(forForsteStatusOppdatering, 500)).isEmpty();

        DialogStatus.DialogStatusBuilder dialogStatusBuilder = builder().dialogId(dialogId);

        dialogStatusService.oppdaterVenterPaSvarFraBrukerSiden(dialogData, dialogStatusBuilder
                .venterPaSvar(true)
                .build());

        updateDialogAktorFor(AKTOR_ID);

        Date etterForsteStatusOppdatering = uniktTidspunkt();
        DialogAktor etterForsteOppdatering = hentAktorMedEndringerEtter(forForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.sisteEndring).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterForsteOppdatering.tidspunktEldsteUbehandlede).isBefore(forForsteStatusOppdatering);

        Date forAndreStatusOppdatering = uniktTidspunkt();
        assertThat(dialogFeedDAO.hentAktorerMedEndringerFOM(forAndreStatusOppdatering, 500)).isEmpty();

        DialogData oppdatert2 = dialogDAO.hentDialog(dialogId);
        dialogStatusService.oppdaterVenterPaNavSiden(oppdatert2, dialogStatusBuilder
                .ferdigbehandlet(true)
                .build());

        updateDialogAktorFor(AKTOR_ID);

        uniktTidspunkt(); // TODO: Check this.

        DialogAktor etterAndreOppdatering = hentAktorMedEndringerEtter(forAndreStatusOppdatering);
        assertThat(etterAndreOppdatering.sisteEndring).isBetween(forAndreStatusOppdatering, uniktTidspunkt());
        assertThat(etterAndreOppdatering.tidspunktEldsteVentende).isBetween(forForsteStatusOppdatering, etterForsteStatusOppdatering);
        assertThat(etterAndreOppdatering.tidspunktEldsteUbehandlede).isNull();

        Date forNyHenvendelse = uniktTidspunkt();
        HenvendelseData nyHenvendelse = nyHenvendelse(dialogId, AKTOR_ID, AvsenderType.BRUKER);
        HenvendelseData nyOpprettetHenvendelse = dialogDAO.opprettHenvendelse(nyHenvendelse);
        dialogStatusService.nyHenvendelse(dialogDAO.hentDialog(dialogId), nyOpprettetHenvendelse);
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

    private DialogData opprettNyDialog(String aktorId) {
        return dialogDAO.opprettDialog(nyDialog(aktorId));
    }

    public void updateDialogAktorFor(String aktorId) {
        val dialoger = dialogDAO.hentDialogerForAktorId(aktorId);
        dialogFeedDAO.updateDialogAktorFor(aktorId, dialoger);
    }
}
