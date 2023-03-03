package no.nav.fo.veilarbdialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockBruker;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockNavService;
import no.nav.fo.veilarbdialog.mock_nav_modell.MockVeileder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@Disabled("Comment out @Ignore to run the application locally.")
@SpringBootTest
@ActiveProfiles("local")
@EnableAutoConfiguration
@EmbeddedKafka
@AutoConfigureWireMock
@Slf4j
class TestApplication {

    @Test
    @Sql("/db/testdata/slett_alle_dialoger.sql")
    @java.lang.SuppressWarnings("squid:S2925")
    void main()
            throws InterruptedException {

        System.setProperty("VEILARB_KASSERING_IDENTER", "NAVIDENT"); // See KasserRessurs.
        System.setProperty("spring.profiles.active", "local");
        Assertions.assertThatCode(Application::main).doesNotThrowAnyException();
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        log.info("Wiremock og mock tokens satt opp for bruker {} og veileder {}", happyBruker, veileder);
        log.info("Application ready");
        Thread.sleep(Long.MAX_VALUE);

    }

}
