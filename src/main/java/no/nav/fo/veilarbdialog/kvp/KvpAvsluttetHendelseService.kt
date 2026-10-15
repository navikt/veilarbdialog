package no.nav.fo.veilarbdialog.kvp

import no.nav.fo.veilarbdialog.service.DialogDataService
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.Date

@Service
class KvpAvsluttetHendelseService(
    private val dialogDataService: DialogDataService,
) {
    fun behandleAvsluttet(aktorId: String, avsluttetDato: ZonedDateTime) {
        dialogDataService.settKontorsperredeDialogerTilHistoriske(aktorId, Date.from(avsluttetDato.toInstant()))
    }
}
