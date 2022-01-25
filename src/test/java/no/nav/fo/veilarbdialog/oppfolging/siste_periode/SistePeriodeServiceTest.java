package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import no.nav.common.types.identer.AktorId;
import no.nav.fo.veilarbdialog.mock_nav_modell.BrukerOptions;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class SistePeriodeServiceTest {
    @Autowired
    SistePeriodeDAO sistePeriodeDAO;
    @Autowired
    SistePeriodeService sistePeriodeService;

    @Test
    public void skalHenteOgBrukeSistePeriodeFraDao() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode oppfolgingsperiode = new Oppfolgingsperiode(mockBruker.getAktorId(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), null);
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode);
        UUID fraBasen = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraBasen).isEqualTo(oppfolgingsperiodeId);
    }

    @Test
    public void fallBackHvisPeriodeAvsluttet() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        Oppfolgingsperiode avsluttet = new Oppfolgingsperiode(mockBruker.getAktorId(), oppfolgingsperiodeId, ZonedDateTime.now().minusDays(5), ZonedDateTime.now());
        sistePeriodeDAO.uppsertOppfolingsperide(avsluttet);
        UUID fraVeilarbOppfolging = sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(mockBruker.getAktorIdAsAktorId());
        assertThat(fraVeilarbOppfolging).isEqualTo(mockBruker.getOppfolgingsperiode());

    }

    @Test
    public void oppfolgingFeiler() {
        BrukerOptions oppfolgingFeiler = BrukerOptions.happyBrukerBuilder().oppfolgingFeiler(true).build();
        MockBruker bruker = MockNavService.createBruker(oppfolgingFeiler);
        AktorId aktorId = bruker.getAktorIdAsAktorId();
        Assert.assertThrows(ResponseStatusException.class, () -> sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId));
    }

    @Test
    public void ikkeUnderOppfolging() {
        BrukerOptions ikkeUnderOppfolging = BrukerOptions.happyBrukerBuilder().underOppfolging(false).build();
        MockBruker bruker = MockNavService.createBruker(ikkeUnderOppfolging);
        AktorId aktorId = bruker.getAktorIdAsAktorId();
        Assert.assertThrows(IngenGjeldendePeriodeException.class, () -> sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorId));
    }


}