package no.nav.fo.veilarbdialog;

import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Ignore("Comment out @Ignore to run the application locally.")
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("local")
@EnableAutoConfiguration
@EmbeddedKafka
@Slf4j
public class TestApplication {

    @ClassRule
    public static final GenericContainer<?> IBM_MQ = new GenericContainer<>(DockerImageName.parse("ibmcom/mq"))
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withExposedPorts(1414);

    @Test
    @Sql("/db/testdata/dialog.sql")
    public void main()
        throws InterruptedException {

        System.setProperty("VEILARB_KASSERING_IDENTER", "NAVIDENT"); // See KasserRessurs.
        System.setProperty("spring.profiles.active", "local"); // See MessageQueueConfig (application doesn't inherit profile from test when running).
        Application.main();
        log.info("Application ready");
        Thread.sleep(Long.MAX_VALUE);

    }

}
