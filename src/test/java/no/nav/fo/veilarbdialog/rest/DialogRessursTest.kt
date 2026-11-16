package no.nav.fo.veilarbdialog.rest

import com.github.tomakehurst.wiremock.client.WireMock
import io.restassured.response.Response
import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.SpringBootTestBase
import no.nav.fo.veilarbdialog.clients.dialogvarsler.DialogVarslerClient
import no.nav.fo.veilarbdialog.domain.AntallUlesteDTO
import no.nav.fo.veilarbdialog.domain.DialogDTO
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO
import no.nav.fo.veilarbdialog.domain.SistOppdatertDTO
import no.nav.fo.veilarbdialog.mock_nav_modell.*
import no.nav.fo.veilarbdialog.sisteDialogHendelse.SisteDialogHendelse
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZoneId
import kotlin.String

internal class DialogRessursTest: SpringBootTestBase() {

    private lateinit var bruker: MockBruker
    private lateinit var veileder: MockVeileder

    @BeforeEach
    fun setupl() {
        bruker = MockNavService.createHappyBruker()
        veileder = MockNavService.createVeileder(bruker)
        Mockito.`when`<Boolean>(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true)
    }

    private fun veilederSenderMelding() {
        nyMelding(veileder, bruker)
    }

    private fun nyMelding(
        avsender: RestassuredUser?,
        bruker: MockBruker?,
        henvendelseDTO: NyMeldingDTO? = NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift dialog")
    ): DialogDTO {
        val erVeileder = avsender is MockVeileder
        val postfix = if (erVeileder) "?fnr={fnr}" else null
        val request = avsender!!.createRequest()
            .body(henvendelseDTO)
        var requestWithBody: Response? = null
        if (erVeileder) {
            requestWithBody = request.post("/veilarbdialog/api/dialog" + postfix, bruker!!.getFnr())
        } else {
            requestWithBody = request.post("/veilarbdialog/api/dialog")
        }
        return requestWithBody
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)
    }

    private fun nyHenvendelseUtenFnrIUrl(avsender: RestassuredUser, henvendelseDTO: NyMeldingDTO): DialogDTO? {
        return avsender!!.createRequest()
            .body(henvendelseDTO)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO?>(DialogDTO::class.java)
    }

    private fun hentDialoger(avsender: RestassuredUser, mockBruker: MockBruker): MutableList<DialogDTO?>? {
        val erVeileder = avsender is MockVeileder
        val postfix = if (erVeileder) "?fnr={fnr}" else null
        val r = avsender.createRequest()
        var request: Response? = null
        if (erVeileder) {
            request = r.get("/veilarbdialog/api/dialog" + postfix, mockBruker.fnr)
        } else {
            request = r.get("/veilarbdialog/api/dialog")
        }
        return request
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList<DialogDTO?>(".", DialogDTO::class.java)
    }

    private fun hentDialog(subject: RestassuredUser, dialogId: String): DialogDTO {
        return subject.createRequest()
            .get("/veilarbdialog/api/dialog/{dialogId}", dialogId)
            .then()
            .statusCode(200)
            .extract()
            .`as`<DialogDTO>(DialogDTO::class.java)
    }

    private fun hentAntallUleste(user: RestassuredUser?, fnr: String?): AntallUlesteDTO {
        val request = user!!.createRequest()
        if (fnr != null) {
            request.body(FnrDto(fnr))
        }
        return request
            .post("/veilarbdialog/api/dialog/antallUleste")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getObject<AntallUlesteDTO>(".", AntallUlesteDTO::class.java)
    }

    private fun hentSistOppdatert(user: RestassuredUser?, fnr: String?): SistOppdatertDTO? {
        val request = user!!.createRequest()
        if (fnr != null) {
            request.body(FnrDto(fnr))
        }
        return request
            .post("/veilarbdialog/api/dialog/sistOppdatert")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getObject<SistOppdatertDTO?>(".", SistOppdatertDTO::class.java)
    }

    @Test
    fun hentDialoger_bruker() {
        veilederSenderMelding()
        val dialoger = hentDialoger(bruker!!, bruker!!)
        Assertions.assertThat<DialogDTO?>(dialoger).hasSize(1)
    }

    @Test
    fun bruker_skal_kunne_hente_antall_uleste() {
        veilederSenderMelding()
        val uleste = hentAntallUleste(bruker, null)
        Assertions.assertThat(uleste.antallUleste).isEqualTo(1)
    }

    @Test
    fun veileder_skal_kunne_hente_antall_uleste() {
        veilederSenderMelding()
        val uleste = hentAntallUleste(veileder, bruker!!.getFnr())
        Assertions.assertThat(uleste.antallUleste).isZero()
    }

    @Test
    fun bruker_skal_kunne_hente_sist_oppdatert() {
        veilederSenderMelding()
        hentSistOppdatert(bruker, null)
    }

    @Test
    fun veileder_skal_kunne_hente_sist_oppdatert() {
        veilederSenderMelding()
        hentSistOppdatert(veileder, bruker!!.getFnr())
    }

    @Test
    fun skal_kunne_filtrere_vekk_dialoger_med_kontorsperre_på_veileders_egen_enhet_når_bruker_er_i_kvp() {
        val oppfølgingsenhet = "enhetTilKvpBruker"
        val brukerOptions =
            BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true)
                .oppfolgingsEnhet(oppfølgingsenhet).build()
        val kvpBruker = MockNavService.createBruker(brukerOptions)
        val veileder = MockNavService.createVeileder(kvpBruker)
        veileder.setNasjonalTilgang(true)
        dialogTestService.opprettDialogSomVeileder(
            veileder,
            kvpBruker,
            NyMeldingDTO().setTekst("hei").setOverskrift("overskrift dialog 3")
        )
        dialogTestService.opprettDialogSomBruker(
            kvpBruker,
            NyMeldingDTO().setTekst("hallo").setOverskrift("overskrift dialog 2")
        )

        val dialoger = veileder.createRequest()
            .get("/veilarbdialog/api/dialog?fnr={fnr}&ekskluderDialogerMedKontorsperre=true", kvpBruker.getFnr())
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList<DialogDTO?>(".", DialogDTO::class.java)

        Assertions.assertThat<DialogDTO?>(dialoger).isEmpty()
    }

    @Test
    fun skal_kunne_inkludere_dialoger_med_kontorsperre_på_veileders_egen_enhet_selv_om_bruker_er_i_kvp() {
        val oppfølgingsenhet = "enhetTilKvpBruker"
        val brukerOptions =
            BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true)
                .oppfolgingsEnhet(oppfølgingsenhet).build()
        val kvpBruker = MockNavService.createBruker(brukerOptions)
        val veileder = MockNavService.createVeileder(kvpBruker)
        dialogTestService.opprettDialogSomVeileder(
            veileder,
            kvpBruker,
            NyMeldingDTO().setTekst("hei").setOverskrift("overskrift")
        )
        dialogTestService.opprettDialogSomBruker(
            kvpBruker,
            NyMeldingDTO().setTekst("hallo").setOverskrift("overskrift")
        )

        val dialoger = hentDialoger(veileder, kvpBruker)

        Assertions.assertThat<DialogDTO?>(dialoger).hasSize(2)
    }

    @Test
    fun nyHenvendelse_fraBruker_venterPaaNav() {
        val kafkaConsumer = kafkaTestService.createStringStringConsumer(sisteHendelseTopic)
        val dialog = nyMelding(bruker, bruker)

        // Konsumerer meldinger fra sisteHendelseTopic
        val records = kafkaTestService.consumeNewRecords(sisteHendelseTopic, kafkaConsumer)
        val sisteHendelse: SisteDialogHendelse =
            JsonUtils.fromJson(records.last().value(), SisteDialogHendelse::class.java)

        //Bruker skal ikke vite om nav har ferdig behandlet dialogen
        Assertions.assertThat(dialog.isVenterPaSvar).isFalse()
        Assertions.assertThat(dialog.isFerdigBehandlet).isTrue()

        val veiledersDialog = hentDialog(veileder!!, dialog.id)

        Assertions.assertThat(veiledersDialog.isVenterPaSvar).isFalse()
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isFalse()

        wireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "eventType",
                        WireMock.equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_BRUKER_TIL_NAV.name)
                    )
                )
        )

        // Verifiser at sisteHendelseTopic har sendt siste melding
        Assertions.assertThat(records.last().key()).isEqualTo(bruker!!.aktorId)
        Assertions.assertThat(sisteHendelse.sisteMeldingBruker).isNotNull()
        Assertions.assertThat(sisteHendelse.sisteMeldingBruker!!.dialogId).isEqualTo(dialog.id.toLong())
        Assertions.assertThat(sisteHendelse.sisteMeldingBruker.tidspunkt)
            .isEqualTo(dialog.sisteDato.toInstant().atZone(ZoneId.of("Europe/Oslo")))
        Assertions.assertThat(sisteHendelse.sisteMeldingVeileder).isNull()
    }

    @Test
    fun nyHenvendelse_fraVeileder_venterIkkePaaNoen() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        val dialog = nyMelding(veileder, bruker, NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift"))

        Assertions.assertThat(dialog.isVenterPaSvar()).isFalse()
        Assertions.assertThat(dialog.isFerdigBehandlet()).isTrue()
        wireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/please/notify-subscribers"))
                .withRequestBody(
                    WireMock.matchingJsonPath(
                        "eventType",
                        WireMock.equalTo(DialogVarslerClient.EventType.NY_DIALOGMELDING_FRA_NAV_TIL_BRUKER.name)
                    )
                )
        )
    }

    @Test
    fun nyHenvendelse_fraVeileder_venter_på_svar_fra_bruker() {
        val kafkaConsumer = kafkaTestService.createStringStringConsumer(sisteHendelseTopic)
        val dialog = NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift").setVenterPaaSvarFraBruker(true)
            .setFnr(bruker!!.fnr)

        val dialogIRespons = veileder!!.createRequest()
            .body(dialog)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(200)
            .extract()
            .`as`(DialogDTO::class.java)

        Assertions.assertThat(dialogIRespons.isVenterPaSvar()).isTrue()

        // Konsumerer meldinger fra sisteHendelseTopic
        val records = kafkaTestService.consumeNewRecords(sisteHendelseTopic, kafkaConsumer)
        val sisteHendelse: SisteDialogHendelse =
            JsonUtils.fromJson(records.last().value(), SisteDialogHendelse::class.java)

        // Verifiser at sisteHendelseTopic har sendt siste melding
        Assertions.assertThat(records.last().key()).isEqualTo(bruker!!.aktorId)
        Assertions.assertThat(sisteHendelse.sisteMeldingVeileder).isNotNull()
        Assertions.assertThat(sisteHendelse.sisteMeldingVeileder!!.dialogId).isEqualTo(dialogIRespons.id.toLong())
        Assertions.assertThat(sisteHendelse.sisteMeldingVeileder.tidspunkt)
            .isEqualTo(dialogIRespons.sisteDato.toInstant().atZone(ZoneId.of("Europe/Oslo")))
        Assertions.assertThat(sisteHendelse.sisteMeldingBruker).isNull()
    }

    @Test
    fun nyHenvendelse_veilederSvarerPaaBrukersHenvendelse_venterIkkePaaNav() {
        val brukersDialog = nyMelding(bruker, bruker, NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift"))
        val veiledersHenvendelse = NyMeldingDTO().setTekst("tekst").setDialogId(brukersDialog.id)
        val veiledersDialog = nyMelding(veileder, bruker, veiledersHenvendelse)
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isTrue()
    }

    @Test
    fun nyHenvendelse_brukerSvarerPaaVeiledersHenvendelse_venterPaNav() {
        val veiledersHenvendelse = NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift")
        var veiledersDialog = nyMelding(veileder, bruker, veiledersHenvendelse)

        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isTrue()
        val brukersHenvendelse = NyMeldingDTO().setTekst("tekst").setDialogId(veiledersDialog.id)
        nyMelding(bruker, bruker, brukersHenvendelse)
        veiledersDialog = hentDialog(veileder!!, veiledersDialog.id)
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isFalse()
    }

    @Test
    fun nyHenvendelse_egenvurdering_venterIkkePaaSvarFraNav() {
        val egenVurdering = NyMeldingDTO()
            .setTekst("Jeg skal klare meg selv")
            .setOverskrift("Egenvurdering")
            .setVenterPaaSvarFraNav(false)
        val brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering)
        val veiledersDialog = hentDialog(veileder, brukersEgenvurdering.id)
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isTrue()
    }

    @Test
    fun nyHenvendelse_egenvurdering_venterPaaSvarFraNav() {
        val egenVurdering = NyMeldingDTO()
            .setTekst("Jeg trenger hjelp fra Nav")
            .setOverskrift("Egenvurdering")
            .setVenterPaaSvarFraNav(true)
        val brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering)
        val veiledersDialog = hentDialog(veileder, brukersEgenvurdering.id)
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isFalse()
    }

    @Test
    fun nyHenvendelse_egenvurdering_venterPaaSvarFraNav_default() {
        val egenVurdering = NyMeldingDTO()
            .setOverskrift("Egenvurdering")
            .setTekst("Jeg trenger hjelp fra Nav")
        val brukersEgenvurdering = nyMelding(bruker, bruker, egenVurdering)
        val veiledersDialog = hentDialog(veileder, brukersEgenvurdering.id)
        Assertions.assertThat(veiledersDialog.isFerdigBehandlet).isFalse()
    }

    @Test
    fun nyHenvendelse_fraVeileder_kanVentePaaBeggeParter() {
        //Veileder kan sende en beskjed som bruker ikke trenger å svare på, veileder må eksplisitt markere at dialogen venter på brukeren
        val dialog = nyMelding(
            veileder, bruker, NyMeldingDTO()
                .setTekst("tekst")
                .setOverskrift("overskrift")
                .setVenterPaaSvarFraBruker(true)
                .setVenterPaaSvarFraNav(true)
        )

        Assertions.assertThat(dialog.isVenterPaSvar).isTrue()
        Assertions.assertThat(dialog.isFerdigBehandlet).isFalse()
    }

    @Test
    fun bruker_skal_kunne_sende_henvedelse_uten_fnr_i_url() {
        val melding = NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift")
        nyHenvendelseUtenFnrIUrl(bruker, melding)
    }

    @Test
    fun dialog_uten_overskrift_skal_feile_med_bad_request_400() {
        val melding = NyMeldingDTO().setTekst("tekst")
        bruker!!.createRequest()
            .body(melding)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(400)
    }

    @Test
    fun dialog_med_for_lang_tekst_skal_feile_med_bad_request_400() {
        val melding = NyMeldingDTO().setTekst("tekst".repeat(1001)).setOverskrift("overskrift")
        bruker!!.createRequest()
            .body(melding)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(400)
            .body(Matchers.equalTo<String?>("Ny melding kan ikke være mer enn 5000 tegn"))
    }

    @Test
    fun veileder_skal_kunne_sende_henvedelse_uten_fnr_i_url() {
        val melding = NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift").setFnr(bruker!!.getFnr())
        nyHenvendelseUtenFnrIUrl(veileder, melding)
    }

    @Test
    fun veileder_skal_ikke_kunne_sende_henvedelse_til_kvpbruker_uten_tilgang_til_enhet() {
        val oppfølgingsenhet = "enhetTilKvpBruker"
        val brukerOptions =
            BrukerOptions.builder().erUnderKvp(true).underOppfolging(true).erManuell(false).kanVarsles(true)
                .oppfolgingsEnhet(oppfølgingsenhet).build()
        val kvpBruker = MockNavService.createBruker(brukerOptions)
        val melding = NyMeldingDTO().setFnr(kvpBruker.getFnr()).setTekst("LOL")
        veileder!!.createRequest()
            .body(melding)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(403)
    }

    @Test
    fun bruker_kan_ikke_sende_henvendelse_på_historisk_dialog() {
        val henvendelseFørHistorisk = NyMeldingDTO()
            .setTekst("tekst")
            .setOverskrift("overskrift")
        val dialog = dialogTestService.opprettDialogSomBruker(bruker, henvendelseFørHistorisk)
        dialogDataService.settDialogerTilHistoriske(bruker.aktorId, dialog.oppfolgingsperiode)

        val melding = NyMeldingDTO().setFnr(bruker.fnr).setTekst("LOL").setDialogId(dialog.id)
        bruker.createRequest()
            .body(melding)
            .post("/veilarbdialog/api/dialog")
            .then()
            .statusCode(409)
    }
}
