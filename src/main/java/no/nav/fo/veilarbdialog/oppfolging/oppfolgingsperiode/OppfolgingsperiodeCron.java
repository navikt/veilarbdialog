package no.nav.fo.veilarbdialog.oppfolging.oppfolgingsperiode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class OppfolgingsperiodeCron {
    private final LeaderElectionClient leaderElectionClient;
    private final OppfolgingsperiodeService oppfolgingsperiodeServiceAdder;

    @Scheduled(
            initialDelay = 60000,
            fixedDelay = 500
    )
    public void addOppfolgingsperioder() {
        if (leaderElectionClient.isLeader()) {
            while (oppfolgingsperiodeServiceAdder.addOppfolgingsperioderForEnBruker());
            log.info("ferdig med aa legge til alle oppfolgingsperioder");
        }
    }
}
