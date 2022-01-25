package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.util.KafkaTestService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class SisteOppfolgingsperiodeConsumerTest {

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${application.topic.inn.sisteOppfolgingsperiode}")
    String oppfolgingSistePeriodeTopic;

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

    @Autowired
    private KafkaTestService kafkaTestService;

    @Test
    public void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        UUID oppfolgingsId = UUID.randomUUID();
        String aktorId = new Random().nextInt(100000) + "";


        SisteOppfolgingsperiodeV1 startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
                .uuid(oppfolgingsId)
                .aktorId(aktorId)
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, aktorId, JsonUtils.toJson(startOppfolgiong)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset(),  sendResult.getRecordMetadata().partition(),10);


        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow();
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(oppfolgingsId);
        Assertions.assertThat(oppfolgingsperiode.aktorid()).isEqualTo(aktorId);
        Assertions.assertThat(oppfolgingsperiode.startTid()).isEqualTo(startOppfolgiong.getStartDato());
        Assertions.assertThat(oppfolgingsperiode.sluttTid()).isNull();


        SisteOppfolgingsperiodeV1 avsluttetOppfolgingsperide = startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(MILLIS));
        SendResult<String, String> avsluttetSendResult = producer.send(oppfolgingSistePeriodeTopic, aktorId, JsonUtils.toJson(avsluttetOppfolgingsperide)).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, avsluttetSendResult.getRecordMetadata().offset(), sendResult.getRecordMetadata().partition(),10);


        Oppfolgingsperiode oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(aktorId).orElseThrow();
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode()).isEqualTo(oppfolgingsId);
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.aktorid()).isEqualTo(aktorId);
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.startTid()).isEqualTo(avsluttetOppfolgingsperide.getStartDato());
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.sluttTid()).isEqualTo(avsluttetOppfolgingsperide.getSluttDato());
    }
}