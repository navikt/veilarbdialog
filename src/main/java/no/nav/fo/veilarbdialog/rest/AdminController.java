package no.nav.fo.veilarbdialog.rest;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.job.JobRunner;
import no.nav.fo.veilarbdialog.service.KafkaRepubliseringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    public final static String PTO_ADMIN_SERVICE_USER = "srvpto-admin";

    private final AuthContextHolder authContextHolder;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    @PostMapping("/republiser/endring-paa-dialog")
    public void republiserDialogerPaaKafka() {
        sjekkTilgangTilAdmin();
        JobRunner.runAsync(
                "republiser-endring-paa-dialog",
                kafkaRepubliseringService::republiserEndringPaaDialogMeldingerForBrukereMedAktivDialog
        );
    }

    private void sjekkTilgangTilAdmin() {
        String subject = authContextHolder.getSubject()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!PTO_ADMIN_SERVICE_USER.equals(subject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
