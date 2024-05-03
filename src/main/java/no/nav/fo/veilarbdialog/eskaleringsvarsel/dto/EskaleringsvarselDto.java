package no.nav.fo.veilarbdialog.eskaleringsvarsel.dto;

import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;

import java.time.ZonedDateTime;

public record EskaleringsvarselDto(
        long id,
        long tilhorendeDialogId,

        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse,

        ZonedDateTime avsluttetDato,
        String avsluttetAv,
        String avsluttetBegrunnelse
) {
    public static EskaleringsvarselDto fromEntity(EskaleringsvarselEntity entity) {
        return new EskaleringsvarselDto(
                entity.varselId(),
                entity.tilhorendeDialogId(),
                entity.opprettetAv(),
                entity.opprettetDato(),
                entity.opprettetBegrunnelse(),
                entity.avsluttetDato(),
                entity.avsluttetAv(),
                entity.avsluttetBegrunnelse());
    }
}
