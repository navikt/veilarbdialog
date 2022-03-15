package no.nav.fo.veilarbdialog.brukernotifikasjon;

public enum VarselType {
    ESKALERINGSVARSEL;

    public BrukernotifikasjonsType getNotifikasjonsType() {
        return switch (this) {
            case ESKALERINGSVARSEL -> BrukernotifikasjonsType.OPPGAVE;
        };
    }
}
