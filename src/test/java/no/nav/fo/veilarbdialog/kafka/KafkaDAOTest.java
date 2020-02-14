package no.nav.fo.veilarbdialog.kafka;

import no.nav.fo.IntegationTest;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class KafkaDAOTest extends IntegationTest {

    @Inject
    private KafkaDAO kafkaDAO;

    @BeforeClass
    public static void addSpringBeans() {
        initSpringContext(Arrays.asList(KafkaDAO.class));
    }

    @Test
    public void kan_inserte_feilmelding() {
        KafkaDialogMelding dialog =  KafkaDialogMelding.builder()
                .aktorId("1234")
                .tidspunktOpprettet(LocalDateTime.now())
                .build();

        kafkaDAO.insertFeiletMelding(dialog);


        List<KafkaDialogMelding> alleFeilendeMeldinger = kafkaDAO.hentAlleFeilendeMeldinger();
        KafkaDialogMelding dialogFraDB = alleFeilendeMeldinger.get(0);

        assertThat(dialogFraDB).isEqualTo(dialog);
    }

    @Test
    public void kan_slette_feilmelding() {
        KafkaDialogMelding dialog =  KafkaDialogMelding.builder()
                .aktorId("1234")
                .tidspunktOpprettet(LocalDateTime.now())
                .build();

        kafkaDAO.insertFeiletMelding(dialog);

        kafkaDAO.slettFeiletMelding(dialog);


        List<KafkaDialogMelding> alleFeilendeMeldinger = kafkaDAO.hentAlleFeilendeMeldinger();

        assertThat(alleFeilendeMeldinger.size()).isEqualTo(0);
    }

}
