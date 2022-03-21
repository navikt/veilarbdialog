package no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions;

public class AktivEskaleringException extends RuntimeException {
    public AktivEskaleringException() {
        super("Brukeren har allerede et aktivt eskaleringsvarsel.");
    }

    public AktivEskaleringException(String message) {
        super(message);
    }
}
