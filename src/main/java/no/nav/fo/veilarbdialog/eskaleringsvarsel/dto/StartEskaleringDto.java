package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;

import no.nav.common.types.identer.Fnr;

public record StartEskaleringDto(
        Fnr fnr,
        String begrunnelse,
        String overskrift,
        String tekst
) {
}
