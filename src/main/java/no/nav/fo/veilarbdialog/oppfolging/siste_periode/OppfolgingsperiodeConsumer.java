package no.nav.fo.veilarbdialog.oppfolging.siste_periode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
class OppfolgingsperiodeConsumer {
    private final SistePeriodeDAO sistePeriodeDAO;
    private final EskaleringsvarselService eskaleringsvarselService;
    private final BrukernotifikasjonService brukernotifikasjonService;
    private final DialogDataService dialogDataService;

    @KafkaListener(topics = "${application.topic.inn.oppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory")
    @Timed
    void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        OppfolgingsperiodeV1 oppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), OppfolgingsperiodeV1.class);

        if (oppfolgingsperiodeV1.sluttDato != null) {
            avsluttEskaleringsverslerOgAktiveBrukernotifikasjoner(oppfolgingsperiodeV1);
            settDialogerTilHistorisk(oppfolgingsperiodeV1);
        }

        sistePeriodeDAO.upsertOppfolgingsperiode(
                new Oppfolgingsperiode(
                        oppfolgingsperiodeV1.aktorId,
                        oppfolgingsperiodeV1.uuid,
                        oppfolgingsperiodeV1.startDato,
                        oppfolgingsperiodeV1.sluttDato));
    }

    private void avsluttEskaleringsverslerOgAktiveBrukernotifikasjoner(OppfolgingsperiodeV1 oppfolgingsperiodeV1) {
        eskaleringsvarselService.stop(oppfolgingsperiodeV1.uuid);
        brukernotifikasjonService.bestillDoneForOppfolgingsperiode(oppfolgingsperiodeV1.uuid);
    }

    private void settDialogerTilHistorisk(OppfolgingsperiodeV1 oppfolgingAvsluttetDto) {
        Date sluttDato = new Date(oppfolgingAvsluttetDto.getSluttDato().toInstant().toEpochMilli());
        dialogDataService.settDialogerTilHistoriske(oppfolgingAvsluttetDto.getAktorId(), sluttDato);
    }
}