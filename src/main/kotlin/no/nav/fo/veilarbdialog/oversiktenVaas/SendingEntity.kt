package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import java.time.ZonedDateTime
import java.util.*

data class SendingEntity(
    val uuid: UUID,
    val fnr: Fnr,
    val opprettet: ZonedDateTime = ZonedDateTime.now(),
    val tidspunktSendt: ZonedDateTime? = null,
    val utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES,
    val meldingSomJson: String,
    val kategori: OversiktenMelding.Kategori,
)

enum class UtsendingStatus {
    SKAL_SENDES,
    SENDT,
    SKAL_IKKE_SENDES
}
