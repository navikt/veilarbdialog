package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.feed.common.FeedElement;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarbdialog.domain.DialogAktorDTO;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.fo.veilarbdialog.util.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

@Configuration
public class FeedConfig {
    @Bean
    public FeedController feedController(FeedProducer<DialogAktorDTO> dialogaktorFeed) {
        FeedController feedController = new FeedController();

        feedController.addFeed("dialogaktor", dialogaktorFeed);

        return feedController;
    }

    @Bean
    public FeedProducer<DialogAktorDTO> dialogAktorDTOFeedProducer(AppService appService, RestMapper restMapper) {
        return FeedProducer.<DialogAktorDTO>builder()
                .provider((tidspunkt, pageSize) -> getFeedElementStream(tidspunkt, appService, restMapper))
                .maxPageSize(1000)
                .build();
    }

    private Stream<FeedElement<DialogAktorDTO>> getFeedElementStream(String tidspunkt, AppService appService, RestMapper restMapper) {
        return appService.hentAktorerMedEndringerEtter(DateUtils.toDate(tidspunkt))
                .stream()
                .map(restMapper::somDTO)
                .map((dto) -> new FeedElement<DialogAktorDTO>()
                        .setElement(dto)
                        .setId(dto.getSisteEndring().toString())
                );
    }
}
