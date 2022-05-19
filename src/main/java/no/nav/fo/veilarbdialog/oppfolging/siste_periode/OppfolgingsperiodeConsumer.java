package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class OppfolgingsperiodeConsumer {
    private final SistePeriodeDAO sistePeriodeDAO;
    private final EskaleringsvarselService eskaleringsvarselService;
    private final BrukernotifikasjonService brukernotifikasjonService;

    @KafkaListener(topics = "${application.topic.inn.oppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory")
    void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        OppfolgingsperiodeV1 oppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), OppfolgingsperiodeV1.class);

        if (oppfolgingsperiodeV1.sluttDato != null) {
            eskaleringsvarselService.stop(oppfolgingsperiodeV1.uuid);
            brukernotifikasjonService.bestillDoneForOppfolgingsperiode(oppfolgingsperiodeV1.uuid);
        }

        log.info("Siste oppfølgingsperiode: {}", oppfolgingsperiodeV1);
        sistePeriodeDAO.uppsertOppfolingsperide(
                new Oppfolgingsperiode(
                        oppfolgingsperiodeV1.aktorId,
                        oppfolgingsperiodeV1.uuid,
                        oppfolgingsperiodeV1.startDato,
                        oppfolgingsperiodeV1.sluttDato));
    }
}