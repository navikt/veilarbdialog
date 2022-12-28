package no.nav.fo.veilarbdialog;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@Disabled("Comment out @Ignore to run the application locally.")
@SpringBootTest
@ActiveProfiles("local")
@EnableAutoConfiguration
@EmbeddedKafka
@Slf4j
class TestApplication {

    @Sql("/db/testdata/dialog.sql")
    @Test
    @java.lang.SuppressWarnings("squid:S2925")
    void main()
            throws InterruptedException {

        System.setProperty("VEILARB_KASSERING_IDENTER", "NAVIDENT"); // See KasserRessurs.
        System.setProperty("spring.profiles.active", "local"); // See MessageQueueConfig (application doesn't inherit profile from test when running).
        Assertions.assertThatCode(Application::main).doesNotThrowAnyException();
        log.info("Application ready");
        Thread.sleep(Long.MAX_VALUE);

    }

}
