package no.nav.domain

import no.nav.fo.veilarbdialog.domain.DialogData
import no.nav.fo.veilarbdialog.domain.Egenskap
import no.nav.fo.veilarbdialog.domain.HenvendelseData
import org.slf4j.LoggerFactory
import java.util.*

val logger = LoggerFactory.getLogger(DialogDtoGraphql::class.java)

data class DialogDtoGraphql(
    val id: String?,
    val aktivitetId: String?,
    val overskrift: String?,
    val sisteTekst: String?,
    val sisteDato: Date?,
    val opprettetDato: Date?,
    val historisk: Boolean?,
    val lest: Boolean?,
    val venterPaSvar: Boolean?,
    val ferdigBehandlet: Boolean?,
    val lestAvBrukerTidspunkt: Date?,
    val erLestAvBruker: Boolean?,
    val oppfolgingsperiode: UUID?,
    val henvendelser: List<HenvendelseDtoGraphQl>?,
    val egenskaper: List<Egenskap>?,
    val kontorsperreEnhetId: String?
) {
    companion object {
        fun mapTilDialogDtoGraphql(dialogData: DialogData?, erEksternBruker: Boolean): DialogDtoGraphql? = runCatching {
            if (dialogData == null) return null
            val sisteHenvendelse = dialogData.henvendelser?.maxByOrNull { it.sendt ?: Date.from(java.time.Instant.MIN) }

            return DialogDtoGraphql(
                id = dialogData.id.toString(),
                aktivitetId = dialogData.aktivitetId?.id,
                overskrift = dialogData.overskrift,
                sisteTekst = sisteHenvendelse?.tekst,
                sisteDato = sisteHenvendelse?.sendt ?: dialogData.opprettetDato,
                opprettetDato = dialogData.opprettetDato,
                historisk = dialogData.isHistorisk,
                lest = dialogData.erLestAvBruker(),
                venterPaSvar = dialogData.venterPaSvarFraBruker(),
                ferdigBehandlet = if (erEksternBruker) true else dialogData.erFerdigbehandlet(),
                erLestAvBruker = if (erEksternBruker) false else dialogData.erLestAvBruker(),
                lestAvBrukerTidspunkt = if (erEksternBruker) null else dialogData.lestAvBrukerTidspunkt,
                oppfolgingsperiode = dialogData.oppfolgingsperiode,
                henvendelser = dialogData.henvendelser?.map { HenvendelseDtoGraphQl.mapTilHenvendelseDtoGraphql(it, erEksternBruker) } ?: emptyList(),
                egenskaper = dialogData.egenskaper?.map { Egenskap.valueOf(it.name) } ?: emptyList(),
                kontorsperreEnhetId = dialogData.kontorsperreEnhetId
            )
        }.onFailure {
            logger.error("Klarte ikke mappe felter i graphql dto", it)
        }.getOrThrow()
    }
}

data class HenvendelseDtoGraphQl(
    val id: String?,
    val dialogId: String?,
    val avsender: String?,
    val avsenderId: String?,
    val sendt: Date?,
    val lest: Boolean?,
    val viktig: Boolean?,
    val tekst: String?,
) {
    companion object {
        fun mapTilHenvendelseDtoGraphql(henvendelseData: HenvendelseData, eksternBruker: Boolean): HenvendelseDtoGraphQl {
            return HenvendelseDtoGraphQl(
                id = henvendelseData.id.toString(),
                dialogId = henvendelseData.dialogId?.toString(),
                avsender = henvendelseData.avsenderType.name,
                avsenderId = if (eksternBruker) null else henvendelseData.avsenderId,
                sendt = henvendelseData.sendt,
                lest = if (eksternBruker) henvendelseData.lestAvBruker else henvendelseData.lestAvVeileder,
                viktig = henvendelseData.viktig,
                tekst = henvendelseData.tekst
            )
        }
    }
}
