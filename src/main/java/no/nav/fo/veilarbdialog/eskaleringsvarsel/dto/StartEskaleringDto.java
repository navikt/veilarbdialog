package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;

public record StartEskaleringDto(
        String fnr,
        String begrunnelse,
        String overskrift,
        String tekst
) {
}
