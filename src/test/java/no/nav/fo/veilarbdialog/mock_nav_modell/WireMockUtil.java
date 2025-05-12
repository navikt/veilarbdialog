package no.nav.fo.veilarbdialog.mock_nav_modell;

import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.oppfolging.v2.OppfolgingPeriodeMinimalDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.fo.veilarbdialog.SpringBootTestBase.wireMock;

public class WireMockUtil {

    static void stubBruker(MockBruker mockBruker) {
        String fnr = mockBruker.getFnr();
        String aktorId = mockBruker.getAktorId();
        boolean erManuell = mockBruker.getBrukerOptions().isErManuell();
        boolean erReservertKrr = mockBruker.getBrukerOptions().isErReservertKrr();
        boolean erUnderKvp = mockBruker.getBrukerOptions().isErUnderKvp();
        boolean kanVarsles = mockBruker.getBrukerOptions().isKanVarsles();
        boolean underOppfolging = mockBruker.getBrukerOptions().isUnderOppfolging();
        boolean oppfolgingFeiler = mockBruker.getBrukerOptions().isOppfolgingFeiler();

        oppfolging(fnr, underOppfolging, oppfolgingFeiler, mockBruker.getOppfolgingsperiode());
        manuell(fnr, erManuell, erReservertKrr, kanVarsles);
        kvp(aktorId, erUnderKvp, mockBruker.getBrukerOptions().getOppfolgingsEnhet());
        aktor(fnr, aktorId);
        dialogvarsler();
    }

    private static void oppfolging(String fnr, boolean underOppfolging, boolean oppfolgingFeiler, UUID periode) {
        if (oppfolgingFeiler) {
            wireMock.stubFor(post("/veilarboppfolging/api/v3/hent-oppfolging")
                    .willReturn(aResponse().withStatus(500)));
            wireMock.stubFor(post("/veilarboppfolging/api/v3/oppfolging/periode/hent-gjeldende-periode")
                    .willReturn(aResponse().withStatus(500)));
            wireMock.stubFor(post("/veilarboppfolging/api/v3/oppfolging/hent-perioder")
                    .willReturn(aResponse().withStatus(500)));
            return;
        }
        wireMock.stubFor(post("/veilarboppfolging/api/v3/hent-oppfolging")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderOppfolging\":" + underOppfolging + "}")));

        if (underOppfolging) {
            OppfolgingPeriodeMinimalDTO oppfolgingsperiode = OppfolgingPeriodeMinimalDTO.builder()
                    .startDato(ZonedDateTime.now().minusDays(5))
                    .uuid(periode)
                    .build();
            OppfolgingPeriodeMinimalDTO gammelPeriode = OppfolgingPeriodeMinimalDTO.builder()
                    .startDato(ZonedDateTime.now().minusDays(100))
                    .sluttDato(ZonedDateTime.now().minusDays(50))
                    .uuid(UUID.randomUUID())
                    .build();

            String gjeldendePeriode = JsonUtils.toJson(oppfolgingsperiode);

            String oppfolgingsperioder = JsonUtils.toJson(List.of(oppfolgingsperiode, gammelPeriode));
            wireMock.stubFor(post("/veilarboppfolging/api/v3/oppfolging/periode/hent-gjeldende-periode")
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(gjeldendePeriode)));
            wireMock.stubFor(post("/veilarboppfolging/api/v3/oppfolging/hent-perioder")
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(oppfolgingsperioder)));

        } else {
            wireMock.stubFor(post("/veilarboppfolging/api/v3/oppfolging/periode/hent-gjeldende-periode")
                    .willReturn(aResponse().withStatus(204)));
        }
    }

    private static void manuell(String fnr, boolean erManuell, boolean erReservertKrr, boolean kanVarsles) {
        wireMock.stubFor(post("/veilarboppfolging/api/v3/manuell/hent-status")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderManuellOppfolging\":" + erManuell + ",\"krrStatus\":{\"kanVarsles\":" + kanVarsles + ",\"erReservert\":" + erReservertKrr + "}}")));
    }

    private static void kvp(String aktorId, boolean erUnderKvp, String enhet) {
        if (erUnderKvp) {
            wireMock.stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody("{\"enhet\":\"" + enhet + "\"}")));
        } else {
            wireMock.stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(aResponse().withStatus(204)));
        }
    }

    private static void dialogvarsler() {
        wireMock.stubFor(post("/please/notify-subscribers")
                .willReturn(aResponse().withStatus(204)));
    }

    public static void aktorUtenGjeldendeIdent(String fnr, String aktorId) {
        wireMock.stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId)))
                .willReturn(aResponse()
                        .withBody("""
                                 {
                                   "data": {
                                     "hentIdenter": {
                                       "identer": []
                                     }
                                   }
                                 }
                                 """)));

        wireMock.stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*AKTORID.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fnr)))
                .willReturn(aResponse()
                        .withBody("""
                                 {
                                   "data": {
                                     "hentIdenter": {
                                       "identer": []
                                     }
                                   }
                                 }
                                 """)));
    }

    private static void aktor(String fnr, String aktorId) {
        wireMock.stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId)))
                .willReturn(aResponse()
                        .withBody("""
                                 {
                                   "data": {
                                     "hentIdenter": {
                                       "identer": [{
                                          "ident": "%s",
                                          "historisk": false,
                                          "gruppe": "FOLKEREGISTERIDENT"
                                       }
                                       ]
                                     }
                                   }
                                 }
                                 """.formatted(fnr))));

        wireMock.stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*AKTORID.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fnr)))
                .willReturn(aResponse()
                        .withBody("""
                                 {
                                   "data": {
                                     "hentIdenter": {
                                       "identer": [{
                                          "ident": "%s",
                                          "historisk": false,
                                          "gruppe": "AKTORID"

                                       }]
                                     }
                                   }
                                 }
                                 """.formatted(aktorId))));
    }
}
