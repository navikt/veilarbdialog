package no.nav.fo.veilarbdialog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@EmbeddedKafka
@ActiveProfiles("test")
public class TestApplication extends Application {

    @Test
    public void main() {
        TestApplication.main(new String[] {});
    }

}
