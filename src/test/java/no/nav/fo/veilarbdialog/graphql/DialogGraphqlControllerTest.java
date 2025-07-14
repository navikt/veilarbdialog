package no.nav.fo.veilarbdialog.graphql;

import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.SpringBootTestBase;
import no.nav.fo.veilarbdialog.domain.AktivitetId;
import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.KladdDTO;
import no.nav.fo.veilarbdialog.domain.NyMeldingDTO;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StartEskaleringDto;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.dto.StopEskaleringDto;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import no.nav.fo.veilarbdialog.mock_nav_modell.RestassuredUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogGraphqlControllerTest extends SpringBootTestBase {

    private MockBruker bruker;
    private MockVeileder veileder;

    @BeforeEach
    void setupl() { // Må bruke et annet navn en "setup" fordi det brukes i super-klassen
        bruker = MockNavService.createHappyBruker();
        veileder = MockNavService.createVeileder(bruker);
        Mockito.when(unleash.isEnabled("veilarbdialog.dialogvarsling")).thenReturn(true);
    }

    private GraphqlResult graphqlRequest(RestassuredUser user, String fnr, String query) {
        return graphqlRequest(user, fnr, query, false);
    }

    private GraphqlResult graphqlRequest(RestassuredUser user, String fnr,  String query, Boolean bareMedAktiviteter) {
        return user.createRequest()
            .body("{ \"query\": \""+ query  +"\", \"variables\": { \"fnr\": \"" + fnr + "\", \"bareMedAktiviteter\": " + bareMedAktiviteter + "} }")
            .post("/veilarbdialog/graphql")
            .then()
            .statusCode(200)
            .extract()
            .as(GraphqlResult.class);
    }

    private DialogDTO nyTraad(RestassuredUser user) {
        return nyTraad(user, null);
    }
    private DialogDTO nyTraad(RestassuredUser user, AktivitetId aktivitetId) {
        return user.createRequest()
                .body(new NyMeldingDTO().setTekst("tekst").setOverskrift("overskrift").setAktivitetId(aktivitetId != null ? aktivitetId.getId() : null))
                .queryParam("aktorId", bruker.getAktorId())
                .post("/veilarbdialog/api/dialog")
                .then()
                .statusCode(200)
                .extract().as(DialogDTO.class);
    }

    private void startVarsel(MockVeileder veileder, MockBruker bruker) {
        this.dialogTestService.startEskalering(veileder, new StartEskaleringDto(
                Fnr.of(bruker.getFnr()),
                "Fordi",
                "VARSEL",
                "tekst",
                null
        ));
    }

    private void stansVarsel(MockVeileder veileder, MockBruker bruker) {
        var stopp = new StopEskaleringDto(Fnr.of(bruker.getFnr()), "Fordi", false);
        this.dialogTestService.stoppEskalering(veileder, stopp);
    }

    @Test
    void veileder_skal_kun_hente_dialoger_for_bruker() {
        nyTraad(veileder);
        var result = graphqlRequest(veileder, bruker.getFnr(), allDialogFields);
        assertThat(result.data.dialoger).hasSize(1);
        assertThat(result.errors).isNull();
    }

    @Test
    void veileder_maa_oppgi_fnr() {
        var result = graphqlRequest(veileder, "", allDialogFields);
        assertThat(result.data.dialoger).isNull();
        assertThat(result.errors.get(0).message).isEqualTo("Ikke tilgang");
    }

    @Test
    void bruker_skal_kun_hente_dialoger_for_seg_selv() {
        nyTraad(bruker);
        var result = graphqlRequest(bruker, bruker.getFnr(), allDialogFields);
        assertThat(result.data.dialoger).hasSize(1);
        assertThat(result.errors).isNull();
    }

    @Test
    void skal_kunne_be_om_bare_dialoger_med_aktivitet_id() {
        nyTraad(bruker, new AktivitetId("123123"));
        nyTraad(bruker);
        var result = graphqlRequest(bruker, bruker.getFnr(), allDialogFields, true);
        assertThat(result.data.dialoger).hasSize(1);
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_bare_kunne_hente_dialoger_for_seg_selv_uansett_fnr_param() {
        nyTraad(bruker);
        var brukerUtenDialoger = MockNavService.createHappyBruker();
        var result = graphqlRequest(brukerUtenDialoger, bruker.getFnr(), allDialogFields);
        assertThat(result.data.dialoger).hasSize(0);
        assertThat(result.errors).isNull();
    }

    @Test
    void veileder_uten_tilgang_til_bruker_skal_ikke_kunne_hente_dialoger() {
        var veilederUtenTilgang = MockNavService.createVeileder();
        var result = graphqlRequest(veilederUtenTilgang, bruker.getFnr(), allDialogFields);
        assertThat(result).isNotNull();
        assertThat(result.errors).hasSize(1);
        assertThat(result.errors.get(0).message).isEqualTo("Ikke tilgang");
    }

    @Test
    void veileder_skal_kunne_hente_varsel_om_stans() {
        startVarsel(veileder, bruker);
        var result = graphqlRequest(veileder, bruker.getFnr(), varselOmStans);
        assertThat(result.getData().stansVarsel).isNotNull();
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_kunne_hente_varsel_om_stans() {
        startVarsel(veileder, bruker);
        var result = graphqlRequest(bruker, "", varselOmStans);
        assertThat(result.getData().stansVarsel).isNotNull();
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_kunne_hente_kladder() {
        var traad = nyTraad(bruker);
        var kladd = KladdDTO.builder()
                .dialogId(traad.getId())
                .fnr(bruker.getFnr())
                .aktivitetId(traad.getId()) // Workaround
                .tekst("noe").build();
        dialogTestService.nyKladd(bruker, kladd);
        var result = graphqlRequest(bruker, "", kladderQuery);
        assertThat(result.getData().kladder).hasSize(1);
        assertThat(result.getData().kladder.get(0)).isEqualTo(kladd.setFnr(null));
        assertThat(result.errors).isNull();
    }

    @Test
    void veileder_skal_kunne_hente_varsel_historikk() {
        startVarsel(veileder, bruker);
        stansVarsel(veileder, bruker);
        var result = graphqlRequest(veileder, bruker.getFnr(), varselOmStansHistorikk);
        assertThat(result.getData().stansVarselHistorikk).isNotNull();
        assertThat(result.errors).isNull();
    }

    @Test
    void veileder_skal_kunne_hente_kladder() {
        var traad = nyTraad(veileder);
        var kladd = KladdDTO.builder()
                .dialogId(traad.getId())
                .fnr(bruker.getFnr())
                .aktivitetId(traad.getId()) // Workaround
                .tekst("noe").build();
        dialogTestService.nyKladd(veileder, kladd);
        var result = graphqlRequest(veileder, bruker.getFnr(), kladderQuery);
        assertThat(result.getData().kladder).hasSize(1);
        assertThat(result.getData().kladder.get(0)).isEqualTo(kladd.setFnr(null));
        assertThat(result.errors).isNull();
    }

    @Test
    void bruker_skal_ikke_kunne_hente_varsel_historikk() {
        startVarsel(veileder, bruker);
        stansVarsel(veileder, bruker);
        var result = graphqlRequest(bruker, "", varselOmStansHistorikk);
        assertThat(result.getData().stansVarselHistorikk).isNull();
        assertThat(result.errors).hasSize(1);
        assertThat(result.errors.get(0).message).isEqualTo("Ikke tilgang");
    }

    static String varselOmStans = """
        query($fnr: String!) {
            stansVarsel(fnr: $fnr) {
                id,
                tilhorendeDialogId,
                opprettetDato,
                opprettetAv,
                opprettetBegrunnelse
            }
        }
    """.trim().replace("\n", "");

    static String kladderQuery = """
        query($fnr: String!) {
            kladder(fnr: $fnr) {
                overskrift,
                tekst,
                dialogId,
                aktivitetId
            }
        }
    """.trim().replace("\n", "");

    static String varselOmStansHistorikk = """
        query($fnr: String!) {
            stansVarselHistorikk(fnr: $fnr) {
                id,
                tilhorendeDialogId,
                opprettetDato,
                opprettetAv,
                opprettetBegrunnelse,
                avsluttetDato,
                avsluttetAv,
                avsluttetBegrunnelse
            }
        }
    """.trim().replace("\n", "");

    static String allDialogFields = """
            query($fnr: String!, $bareMedAktiviteter: Boolean) {
                dialoger(fnr: $fnr, bareMedAktiviteter: $bareMedAktiviteter) {
                    aktivitetId,
                    oppfolgingsperiode,
                    opprettetDato,
                    egenskaper,
                    erLestAvBruker,
                    ferdigBehandlet,
                    historisk,
                    lest,
                    lestAvBrukerTidspunkt,
                    sisteTekst,
                    sisteDato,
                    venterPaSvar,
                    henvendelser {
                        id,
                        lest,
                        avsender,
                        avsenderId,
                        dialogId,
                        sendt,
                        viktig,
                        tekst
                    }
                }
            }   
        """.trim().replace("\n", "");
}
