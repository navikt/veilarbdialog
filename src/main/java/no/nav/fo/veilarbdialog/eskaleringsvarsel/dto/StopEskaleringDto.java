package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;


import no.nav.common.types.identer.Fnr;

public record StopEskaleringDto(
        Fnr fnr,
        String begrunnelse
) {
}
