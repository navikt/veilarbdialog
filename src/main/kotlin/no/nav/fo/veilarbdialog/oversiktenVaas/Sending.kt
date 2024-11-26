package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.fo.veilarbdialog.domain.Person.Fnr
import java.time.ZonedDateTime

data class Sending(
    val fnr: Fnr,
    val opprettet: ZonedDateTime = ZonedDateTime.now(),
    val tidspunktSendt: ZonedDateTime,
    val utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES,
    val melding: OversiktenUtboksService.OversiktenUtboksMelding
)

enum class UtsendingStatus {
    SKAL_SENDES,
    SENDT,
    SKAL_IKKE_SENDES
}
