package no.nav.fo.veilarbdialog.minsidevarsler.dto

import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.PENDING
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselType
import no.nav.fo.veilarbdialog.minsidevarsel.VarselKvitteringStatus.IKKE_SATT
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters
import no.nav.fo.veilarbdialog.db.jdbc.MinSideVarselJdbcRepository
import no.nav.fo.veilarbdialog.db.jdbc.MinSideVarselRow
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.AVSLUTTET
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.SENDT
import no.nav.fo.veilarbdialog.minsidevarsel.MinSideVarselBehandlingStatus.SKAL_AVSLUTTES
import no.nav.fo.veilarbdialog.minsidevarsel.VarselKvitteringStatus
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel.VarselOmNyMelding
import no.nav.fo.veilarbdialog.minsidevarsler.PendingVarsel
import no.nav.fo.veilarbdialog.util.EnumUtils
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
open class MinsideVarselDao(
    private val repository: MinSideVarselJdbcRepository
) {
    private fun MinSideVarselRow.toVarselId() = MinSideVarselId(this.varselId)
    private fun MinSideVarselRow.toStatus() = EnumUtils.valueOf(MinSideVarselBehandlingStatus::class.java, this.status)
    private fun MinSideVarselRow.toKvitteringStatus() = EnumUtils.valueOf(VarselKvitteringStatus::class.java, this.varselKvitteringStatus)

    private fun MinSideVarselRow.toPendingVarsel() = PendingVarsel(
        varselId = toVarselId(),
        lenke = JdbcConverters.parseUrlOrNull(lenke),
        skalBatches = skalBatches ?: false,
        type = EnumUtils.valueOf(MinSideVarselType::class.java, type),
        melding = melding,
        fnr = Fnr.of(foedselsnummer),
    )

    private fun MinSideVarselRow.toDialogVarselEntity() = DialogVarselEntity(
        toVarselId(),
        toStatus(),
        opprettet,
        toKvitteringStatus()
    )

    open fun hentPendingVarsler(): List<PendingVarsel> {
        return repository.findByStatus(PENDING.name).map { it.toPendingVarsel() }
    }

    open fun hentVarslerSomSkalAvsluttes(): List<MinSideVarselId> {
        return repository.findByStatus(SKAL_AVSLUTTES.name).map { it.toVarselId() }
    }

    open fun updateStatus(varselId: MinSideVarselId, status: MinSideVarselBehandlingStatus): Int {
        return repository.updateStatus(varselId.value, status.name)
    }

    open fun opprettVarselIPendingStatus(pendingMinsideVarsel: DialogVarsel) {
        repository.insert(
            pendingMinsideVarsel.varselId.value,
            pendingMinsideVarsel.foedselsnummer.get(),
            pendingMinsideVarsel.oppfolgingsperiodeId,
            pendingMinsideVarsel.type.name,
            PENDING.name,
            pendingMinsideVarsel.skalBatches,
            pendingMinsideVarsel.melding,
            IKKE_SATT.name,
            pendingMinsideVarsel.lenke.toExternalForm()
        )

        if (pendingMinsideVarsel is VarselOmNyMelding) {
            kobleTilDialog(pendingMinsideVarsel)
        }
    }

    open fun setDialogVarslerTilSkalAvsluttes(dialogId: Long) {
        val varselFor = getVarslerForDialog(dialogId)
        varselFor.forEach { updateStatus(it.varselId, SKAL_AVSLUTTES) }
    }

    open fun finnesVarsel(varselId: MinSideVarselId): Boolean {
        return repository.countByVarselId(varselId.value) > 0
    }

    open fun setEksternVarselFeilet(varselId: MinSideVarselId) {
        repository.updateKvitteringStatus(varselId.value, VarselKvitteringStatus.FEILET.toString())
    }

    open fun setEksternVarselKvitteringStatusOk(varlselId: MinSideVarselId) {
        repository.updateKvitteringStatus(varlselId.value, VarselKvitteringStatus.OK.name)
    }

    /* Only used in tests */
    open fun getMinsideVarselForForhåndsvarsel(forhåndsVarselId: Long): DialogVarselEntity {
        val rows = repository.findForForhandsvarsel()
        if (rows.isEmpty()) throw EmptyResultDataAccessException(1)
        if (rows.size > 1) throw IncorrectResultSizeDataAccessException(1, rows.size)
        return rows.first().toDialogVarselEntity()
    }

    open fun getVarslerForDialog(dialogId: Long): List<DialogVarselEntity> {
        return repository.findForDialog(dialogId).map { it.toDialogVarselEntity() }
    }

    open fun setSkalAvsluttesForVarslerIPeriode(oppfolgingsperiodeUuid: UUID) {
        repository.updateStatusForPeriode(oppfolgingsperiodeUuid, PENDING.name, AVSLUTTET.name)
        repository.updateStatusForPeriode(oppfolgingsperiodeUuid, SENDT.name, SKAL_AVSLUTTES.name)
    }

    private fun kobleTilDialog(varselOmNyMelding: VarselOmNyMelding) {
        repository.insertMapping(varselOmNyMelding.varselId.value, varselOmNyMelding.dialogId)
    }

    open fun hentVarselEntity(varselId: MinSideVarselId): DialogVarselEntity? {
        return repository.findVarsel(varselId.value).orElse(null)?.toDialogVarselEntity()
    }

    open fun hentAntallUkvitterteVarslerForsoktSendt(timerForsinkelse: Long): Int {
        return repository.countUkvitterteForsoktSendt(LocalDateTime.now().minusHours(timerForsinkelse))
    }
}

class DialogVarselEntity(
    val varselId: MinSideVarselId,
    val status: MinSideVarselBehandlingStatus,
    val opprettet: LocalDateTime,
    val kvitteringStatus: VarselKvitteringStatus
)
