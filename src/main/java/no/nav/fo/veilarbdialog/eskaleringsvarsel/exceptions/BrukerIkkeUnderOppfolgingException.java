package no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions;

public class BrukerIkkeUnderOppfolgingException extends RuntimeException {
    public BrukerIkkeUnderOppfolgingException() {
        super("Bruker er ikke under arbeidsrettet oppfølging.");
    }

    public BrukerIkkeUnderOppfolgingException(String message) {
        super(message);
    }
}
