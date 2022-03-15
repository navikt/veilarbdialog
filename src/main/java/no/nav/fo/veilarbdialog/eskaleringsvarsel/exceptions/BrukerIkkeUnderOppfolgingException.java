package no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions;

public class BrukerIkkeUnderOppfolgingException extends RuntimeException {
    public BrukerIkkeUnderOppfolgingException() {
    }

    public BrukerIkkeUnderOppfolgingException(String message) {
        super(message);
    }
}
