package no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions;

public class BrukerKanIkkeVarslesException  extends RuntimeException {
    public BrukerKanIkkeVarslesException() {
        super("Bruker kan ikke varsles digitalt.");
    }

    public BrukerKanIkkeVarslesException(String message) {
        super(message);
    }
}
