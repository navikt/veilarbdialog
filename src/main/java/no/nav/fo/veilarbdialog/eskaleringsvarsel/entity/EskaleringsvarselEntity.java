package no.nav.fo.veilarbdialog.eskaleringsvarsel.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

public record EskaleringsvarselEntity(
        long varselId,
        long tilhorendeDialogId,
        String aktorId,
        String opprettetAv,
        ZonedDateTime opprettetDato,
        String opprettetBegrunnelse,
        ZonedDateTime avsluttetDato,
        String avsluttetAv,
        String avsluttetBegrunnelse,
        UUID brukernotifikasjonsNokkel // Kanskje, kanskje ikke
) {
}
