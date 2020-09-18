package no.nav.fo.veilarbdialog.config;

import lombok.val;
import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbdialog.domain.AvsluttetOppfolgingFeedDTO;
import no.nav.fo.veilarbdialog.domain.DialogAktor;
import no.nav.fo.veilarbdialog.domain.DialogAktorDTO;
import no.nav.fo.veilarbdialog.domain.KvpDTO;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import no.nav.fo.veilarbdialog.util.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
public class FeedProducerConfig {

    @Bean
    public FeedController feedController(
            FeedProducer<DialogAktorDTO> dialogaktorFeed,
            FeedConsumer<AvsluttetOppfolgingFeedDTO> avsluttetOppfolgingFeedDTOFeedConsumer,
            FeedConsumer<KvpDTO> kvpDTOFeedConsumer) {
        FeedController feedController = new FeedController();

        feedController.addFeed("dialogaktor", dialogaktorFeed);
        feedController.addFeed(AvsluttetOppfolgingFeedDTO.FEED_NAME, avsluttetOppfolgingFeedDTOFeedConsumer);
        feedController.addFeed(KvpDTO.FEED_NAME, kvpDTOFeedConsumer);

        return feedController;
    }

    @Bean
    public FeedProducer<DialogAktorDTO> dialogAktorDTOFeedProducer(DialogDataService dialogDataService) {
        return FeedProducer.<DialogAktorDTO>builder()
                .provider((tidspunkt, pageSize) -> getFeedElementStream(tidspunkt, pageSize, dialogDataService))
                .authorizationModule(new OidcFeedAuthorizationModule())
                .maxPageSize(1000)
                .build();
    }

    private Stream<FeedElement<DialogAktorDTO>> getFeedElementStream(String tidspunkt, int pageSize, DialogDataService dialogDataService) {
        Date date = Optional.ofNullable(tidspunkt)
                .map(DateUtils::toDate)
                .orElse(new Date(0));

        return dialogDataService.hentAktorerMedEndringerFOM(date, pageSize)
                .stream()
                .map(this::somFeedElement);
    }

    private FeedElement<DialogAktorDTO> somFeedElement(DialogAktor dialogAktor) {
        String id = DateUtils.ISO8601FromDate(dialogAktor.getOpprettetTidspunkt(), ZoneId.systemDefault());
        val dto = somDTO(id, dialogAktor);

        return new FeedElement<DialogAktorDTO>()
                .setId(id)
                .setElement(dto);
    }

    private DialogAktorDTO somDTO(String id, DialogAktor dialogAktor) {
        return new DialogAktorDTO()
                .setId(id)
                .setAktorId(dialogAktor.aktorId)
                .setTidspunktEldsteUbehandlede(dialogAktor.tidspunktEldsteUbehandlede)
                .setTidspunktEldsteVentende(dialogAktor.tidspunktEldsteVentende)
                .setSisteEndring(dialogAktor.sisteEndring)
                ;
    }
}
