package no.nav.fo.veilarbdialog;

import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
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

import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@EmbeddedKafka
@ActiveProfiles("test")
@Sql("/db/testdata/dialog.sql")
@Slf4j
public class TestApplication extends Application {

    @ClassRule
    public static GenericContainer<?> ibmMQ = new GenericContainer<>(DockerImageName.parse("ibmcom/mq"))
            .withEnv("LICENSE", "accept")
            .withEnv("MQ_QMGR_NAME", "QM1")
            .withExposedPorts(1414);

    @Test
    public void main()
        throws InterruptedException {

        System.setProperty("VEILARB_KASSERING_IDENTER", "NAVIDENT"); // See KasserRessurs.
        TestApplication.main(new String[] {});
        log.info("Application ready");
        Thread.sleep(Long.MAX_VALUE);

    }

    public static Optional<String> getIbmMQPort() {
        return Optional
                .ofNullable(ibmMQ.getFirstMappedPort())
                .map(String::valueOf);
    }

}
