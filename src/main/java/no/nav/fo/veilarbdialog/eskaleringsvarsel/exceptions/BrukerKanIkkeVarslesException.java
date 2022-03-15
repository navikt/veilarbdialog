package no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions;

public class BrukerKanIkkeVarslesException  extends RuntimeException {
    public BrukerKanIkkeVarslesException() {
    }

    public BrukerKanIkkeVarslesException(String message) {
        super(message);
    }
}
