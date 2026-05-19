package no.nav.fo.veilarbdialog.rest;

import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.job.JobRunner;
import no.nav.fo.veilarbdialog.service.KafkaRepubliseringService;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    public static final String POAO_ADMIN = "poao-admin";
    private final AuthContextHolder authContextHolder;
    private final KafkaRepubliseringService kafkaRepubliseringService;
    private final IAuthService authService;

    @PostMapping("/republiser/endring-paa-dialog")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void republiserDialogerPaaKafka() {
        sjekkTilgangTilAdmin();
        JobRunner.runAsync(
                "republiser-endring-paa-dialog",
                kafkaRepubliseringService::republiserEndringPaaDialogMeldingerForBrukereMedAktivDialog
        );
    }

    @PostMapping("/republiser/endring-paa-dialog/bruker")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void republiserDialogerPaaKafkaForAktor(@RequestBody RepubliserForBrukerRequest request) {
        sjekkTilgangTilAdmin();
        JobRunner.runAsync(
                "republiser-endring-paa-dialog-en-bruker",
                () -> kafkaRepubliseringService.republiserEndringPaaDialogMeldingerForBruker(request.aktorId())
        );
    }

    public record RepubliserForBrukerRequest(String aktorId) {}

    private void sjekkTilgangTilAdmin() {
        if (!authContextHolder.erInternBruker()) throw new ForbiddenException("Må være internbruker");
        // Poao-admin krever at man er medlem i en begrenset entra-gruppe
        authService.sjekkAtApplikasjonErIAllowList(List.of(POAO_ADMIN));
    }
}
