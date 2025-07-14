package no.nav.fo.veilarbdialog.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerIkkeUnderOppfolgingException;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.BrukerKanIkkeVarslesException;
import no.nav.fo.veilarbdialog.service.exceptions.NyHenvendelsePåHistoriskDialogException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class HttpExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({BrukerKanIkkeVarslesException.class, BrukerIkkeUnderOppfolgingException.class, AktivEskaleringException.class, NyHenvendelsePåHistoriskDialogException.class})
    public ResponseEntity<String> handleExceptions(Exception e) {
        String feilmelding = String.format("Funksjonell feil under behandling: %s - %s ", e.getClass().getSimpleName(), e.getMessage());
        log.warn(feilmelding);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(feilmelding);
    }

    @ExceptionHandler({ResponseStatusException.class})
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode().value()).body(e.getReason());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
