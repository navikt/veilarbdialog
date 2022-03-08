package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;


public record StopEskaleringDto(
        String fnr,
        String begrunnelse,
        String tekst
) {
}
