package no.nav.fo.veilarbdialog.db;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.db.dao.AktivitetDAO;
import no.nav.fo.veilarbdialog.domain.AktivitetData;
import no.nav.fo.veilarbdialog.domain.AktivitetStatus;
import no.nav.fo.veilarbdialog.domain.EgenAktivitetData;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.veilarbdialog.AktivitetDataBuilder.nyAktivitet;
import static no.nav.fo.veilarbdialog.AktivitetDataBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbdialog.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbdialog.domain.AktivitetTypeData.JOBBSOEKING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AktivitetDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprette_og_hente_egenaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void slett_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val antallSlettet = aktivitetDAO.slettAktivitet(aktivitet.getId());

        assertThat(antallSlettet, equalTo(1));
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID), empty());
    }

    @Test
    public void endre_aktivitet_status() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val raderEndret = aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.GJENNOMFORT);
        assertThat(raderEndret, equalTo(1));
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getStatus(), equalTo(AktivitetStatus.GJENNOMFORT));

        val raderEndret2 = aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.AVBRUTT);
        assertThat(raderEndret2, equalTo(1));
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getStatus(), equalTo(AktivitetStatus.AVBRUTT));
    }

    @Test
    public void hent_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(aktivitet, equalTo(hentetAktivitet));
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSOEKING);
        val stillingsok = nyttStillingssøk();

        aktivitet.setStillingsSoekAktivitetData(stillingsok);
        aktivitetDAO.opprettAktivitet(aktivitet);

        return aktivitet;
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = nyAktivitet(AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData()
                        .setHensikt("nada"));

        aktivitetDAO.opprettAktivitet(aktivitet);

        return aktivitet;
    }
}