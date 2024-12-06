package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import java.time.ZonedDateTime
import java.util.*

data class OversiktenMeldingMedMetadata(
    val meldingKey: MeldingKey,
    val fnr: Fnr,
    val opprettet: ZonedDateTime = ZonedDateTime.now(),
    val tidspunktStartet: ZonedDateTime? = null,
    val tidspunktStoppet: ZonedDateTime? = null,
    val utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_STARTES,
    val meldingSomJson: String,
    val kategori: OversiktenMelding.Kategori,
) {
}

typealias MeldingKey = UUID

enum class UtsendingStatus {
    SKAL_STARTES,
    SKAL_STOPPES,
    STARTET,
    STOPPET,
    ABORTERT
}
