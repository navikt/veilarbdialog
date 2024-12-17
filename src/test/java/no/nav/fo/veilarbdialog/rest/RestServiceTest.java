package no.nav.fo.veilarbdialog.rest;

import io.restassured.RestAssured;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.Avsender;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.HenvendelseDTO;
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;


class RestServiceTest extends SpringBootTestBase {

    MockVeileder veileder;
    MockBruker bruker;


    @BeforeEach
    public void before() {
        RestAssured.port = port;
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
    }

    @Test
    void nyHenvendelse_dialogFinnesIkke_bruker() {
        String tekst = "tekst", overskrift = "overskrift";
        final NyMeldingDTO nyHenvendelse = new NyMeldingDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift);

        final DialogDTO expected = new DialogDTO()
                .setOppfolgingsperiode(bruker.getOppfolgingsperiode())
                .setOverskrift(overskrift)
                .setSisteTekst(tekst)
                .setLest(true)
                .setFerdigBehandlet(true);

        final HenvendelseDTO henvendelseExpected = new HenvendelseDTO()
                .setTekst(tekst)
                .setLest(true)
                .setAvsender(Avsender.BRUKER);


        DialogDTO resultatDialog = bruker.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.getHenvendelser().get(0);

        assertThat(resultatDialog)
                .usingRecursiveComparison()
                .ignoringFields("opprettetDato", "sisteDato", "henvendelser", "id")
                .isEqualTo(expected);
        assertThat(resultatHenvendelse)
                .usingRecursiveComparison()
                .ignoringFields("sendt", "id", "dialogId")
                .isEqualTo(henvendelseExpected);
        assertThat(resultatDialog.getId()).isEqualTo(resultatHenvendelse.getDialogId());

    }

    @Test
    void nyHenvendelse_dialogFinnesIkke_veileder() {
        String tekst = "tekst", overskrift = "overskrift";
        final NyMeldingDTO nyHenvendelse = new NyMeldingDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift);

        final DialogDTO expected = new DialogDTO()
                .setOppfolgingsperiode(bruker.getOppfolgingsperiode())
                .setOverskrift(overskrift)
                .setSisteTekst(tekst)
                .setLest(true)
                .setFerdigBehandlet(true);

        final HenvendelseDTO henvendelseExpected = new HenvendelseDTO()
                .setTekst(tekst)
                .setAvsender(Avsender.VEILEDER)
                .setAvsenderId(veileder.getNavIdent())
                .setLest(true);

        DialogDTO resultatDialog = veileder.createRequest()
                .body(nyHenvendelse)
                .post("/veilarbdialog/api/dialog?fnr={fnr}", bruker.getFnr())
                .then()
                .statusCode(200)
                .extract()
                .as(DialogDTO.class);
        HenvendelseDTO resultatHenvendelse = resultatDialog.getHenvendelser().get(0);

        assertThat(resultatDialog)
                .usingRecursiveComparison()
                .ignoringFields("opprettetDato", "sisteDato", "henvendelser", "id")
                .isEqualTo(expected);
        assertThat(resultatHenvendelse)
                .usingRecursiveComparison()
                .ignoringFields("sendt", "id", "dialogId")
                .isEqualTo(henvendelseExpected);

        assertThat(resultatDialog.getId()).isEqualTo(resultatHenvendelse.getDialogId());
    }

    @Test
    void sistOppdatert_brukerInnlogget_kunBrukerHarLest_returnererNull() {
        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", bruker.getAktorId(), bruker.getAktorId());

        bruker.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(null));
    }

    @Test
    void sistOppdatert_veilederInnlogget_kunVeilederHarLest_returnererNull() {
        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, CURRENT_TIMESTAMP)", bruker.getAktorId(), veileder.getNavIdent());

        veileder.createRequest()
                .param("fnr", bruker.getFnr())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(null));
    }

    @Test
    void sistOppdatert_brukerInnlogget_veilederEndretSist_returnererTidspunkt() {
        Timestamp brukerLest = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
        Timestamp veilederLest = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), bruker.getAktorId(), brukerLest);
        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), veileder.getNavIdent(), veilederLest);

        bruker.createRequest()
                .param("aktorId", bruker.getAktorId())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(veilederLest.getTime()));
    }

    @Test
    void sistOppdatert_veilederInnlogget_brukerEndretSist_returnererTidspunkt() {
        Timestamp veilederLest = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
        Timestamp brukerLest = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (0, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), veileder.getNavIdent(), veilederLest);
        jdbcTemplate.update("insert into EVENT (EVENT_ID, DIALOGID, EVENT, AKTOR_ID, LAGT_INN_AV, TIDSPUNKT) values (1, 0, 'DIALOG_OPPRETTET', ?, ?, ?)", bruker.getAktorId(), bruker.getAktorId(), brukerLest);

        veileder.createRequest()
                .param("fnr", bruker.getFnr())
                .get("/veilarbdialog/api/dialog/sistOppdatert")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(equalTo(MediaType.APPLICATION_JSON_VALUE))
                .body("sistOppdatert", equalTo(brukerLest.getTime()));
    }
}
