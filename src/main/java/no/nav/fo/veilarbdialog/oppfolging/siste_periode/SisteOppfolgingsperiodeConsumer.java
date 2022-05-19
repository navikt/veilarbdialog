package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class SisteOppfolgingsperiodeConsumer {
    private final SistePeriodeDAO sistePeriodeDAO;
    private final EskaleringsvarselService eskaleringsvarselService;
    private final BrukernotifikasjonService brukernotifikasjonService;

    @KafkaListener(topics = "${application.topic.inn.sisteOppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory")
    void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), SisteOppfolgingsperiodeV1.class);

        if (sisteOppfolgingsperiodeV1.sluttDato != null) {
            eskaleringsvarselService.stop(sisteOppfolgingsperiodeV1.uuid);
            brukernotifikasjonService.bestillDoneForOppfolgingsperiode(sisteOppfolgingsperiodeV1.uuid);
        }

        log.info("Siste oppfølgingsperiode: {}", sisteOppfolgingsperiodeV1);
        sistePeriodeDAO.uppsertOppfolingsperide(
                new Oppfolgingsperiode(
                        sisteOppfolgingsperiodeV1.aktorId,
                        sisteOppfolgingsperiodeV1.uuid,
                        sisteOppfolgingsperiodeV1.startDato,
                        sisteOppfolgingsperiodeV1.sluttDato));
    }
}