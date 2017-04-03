package no.nav.fo.veilarbdialog.db;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbdialog.db.dao.AktivitetDAO;
import no.nav.fo.veilarbdialog.db.dao.EndringsLoggDAO;
import no.nav.fo.veilarbdialog.domain.AktivitetData;
import no.nav.fo.veilarbdialog.domain.AktivitetStatus;
import no.nav.fo.veilarbdialog.domain.AktivitetTypeData;
import no.nav.fo.veilarbdialog.domain.InnsenderData;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EndringsloggDAOTest extends IntegrasjonsTest {

    private static final String endretAv = "BATMAN!!";
    private static final String endringsBeskrivelse = "one does not simply change anything";

    @Inject
    private EndringsLoggDAO endringsLoggDao;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprett_og_hent_endringslogg() {
        val aktivitetId = opprett_aktivitet();
        endringsLoggDao.opprettEndringsLogg(aktivitetId, endretAv, endringsBeskrivelse);

        val endringsLoggs = endringsLoggDao.hentEndringdsloggForAktivitetId(aktivitetId);

        assertThat(endringsLoggs, hasSize(1));
        assertThat(endretAv, equalTo(endringsLoggs.get(0).getEndretAv()));
        assertThat(endringsBeskrivelse, equalTo(endringsLoggs.get(0).getEndringsBeskrivelse()));
        assertTrue("Dates are close enough",
                (new Date().getTime() - endringsLoggs.get(0).getEndretDato().getTime()) < 1000);
    }

    @Test
    public void slett_endringslogg() {
        val aktivitetId = opprett_aktivitet();
        endringsLoggDao.opprettEndringsLogg(aktivitetId, endretAv, endringsBeskrivelse);
        endringsLoggDao.opprettEndringsLogg(aktivitetId, endretAv, endringsBeskrivelse);
        endringsLoggDao.opprettEndringsLogg(aktivitetId, endretAv, endringsBeskrivelse);

        assertThat(endringsLoggDao.slettEndringslogg(aktivitetId), equalTo(3));
        val endringsLoggs = endringsLoggDao.hentEndringdsloggForAktivitetId(aktivitetId);
        assertThat(endringsLoggs, hasSize(0));
    }


    private long opprett_aktivitet() {
        val aktorId = "123";

        aktivitetDAO.opprettAktivitet(nyAktivitet(aktorId));

        val aktiviter = aktivitetDAO.hentAktiviteterForAktorId(aktorId);
        return aktiviter.get(0).getId();
    }

    private AktivitetData nyAktivitet(String aktorId) {
        return new AktivitetData()
                .setAktorId(aktorId)
                .setTittel("tittel")
                .setBeskrivelse("beskrivelse")
                .setAktivitetType(AktivitetTypeData.EGENAKTIVITET)
                .setStatus(AktivitetStatus.values()[0])
                .setAvsluttetKommentar("avsluttetKommentar")
                .setLagtInnAv(InnsenderData.values()[0])
                .setLenke("lenke")
                ;
    }

}