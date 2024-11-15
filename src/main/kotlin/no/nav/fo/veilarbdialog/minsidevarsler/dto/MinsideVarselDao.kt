package no.nav.fo.veilarbdialog.minsidevarsler.dto

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.PENDING
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.fo.veilarbdialog.brukernotifikasjon.VarselKvitteringStatus.IKKE_SATT
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonBehandlingStatus.SKAL_AVSLUTTES
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel
import no.nav.fo.veilarbdialog.minsidevarsler.DialogVarsel.VarselOmNyMelding
import no.nav.fo.veilarbdialog.minsidevarsler.PendingVarsel
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import no.nav.fo.veilarbdialog.util.EnumUtils
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.net.URL
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
open class MinsideVarselDao(
    val template: NamedParameterJdbcTemplate
) {
    private fun ResultSet.getVarselId() = MinSideVarselId(DatabaseUtils.hentMaybeUUID(this, "varsel_id"))
    private fun ResultSet.getStatus() = EnumUtils.valueOf(BrukernotifikasjonBehandlingStatus::class.java, this.getString("status"))

    open fun hentPendingVarsler(): List<PendingVarsel> {
        val params = mapOf("pending" to PENDING.name)
        val sql = """
            SELECT * FROM min_side_varsel WHERE STATUS = :pending
        """.trimIndent()
        return template.query(sql, params) { rs, _ ->
            PendingVarsel(
                varselId = rs.getVarselId(),
                lenke = DatabaseUtils.hentMaybeURL(rs, "lenke"),
                skalBatches = rs.getBoolean("skal_batches"),
                type = EnumUtils.valueOf(BrukernotifikasjonsType::class.java, rs.getString("type")),
                melding = rs.getString("melding"),
                fnr = Fnr.of(rs.getString("foedselsnummer")),
            )
        }
    }

    open fun hentVarslerSomSkalAvsluttes(): List<MinSideVarselId> {
        val params = mapOf("skalAvsluttes" to SKAL_AVSLUTTES.name)
        val sql = """SELECT * FROM min_side_varsel WHERE STATUS = :skalAvsluttes"""
        return template.query(sql, params) { rs, _ -> rs.getVarselId() }
    }

    open fun updateStatus(varselId: MinSideVarselId, status: BrukernotifikasjonBehandlingStatus): Int {
        val params = mapOf("varselId" to varselId.value, "status" to status.name)
        val sql = """UPDATE min_side_varsel SET status = :status WHERE varsel_id = :varselId"""
        return template.update(sql, params)
    }

    open fun opprettVarselIPendingStatus(pendingMinsideVarsel: DialogVarsel) {
        val params = mapOf(
            "status" to PENDING.name,
            "varselKvitteringStatus" to IKKE_SATT.name,
            "oppfolgingsperiodeId" to pendingMinsideVarsel.oppfolgingsperiodeId.toString(),
            "type" to pendingMinsideVarsel.type.name,
            "skalBatches" to pendingMinsideVarsel.skalBatches,
            "melding" to pendingMinsideVarsel.melding,
            "fnr" to pendingMinsideVarsel.foedselsnummer.get(),
            "lenke" to pendingMinsideVarsel.lenke.toExternalForm(),
            "varselId" to pendingMinsideVarsel.varselId.value)
        template.update("""
            INSERT INTO min_side_varsel(varsel_id, foedselsnummer, oppfolgingsperiode_id, type, status, skal_batches, melding, varsel_kvittering_status, lenke, opprettet)
            VALUES (:varselId, :fnr, :oppfolgingsperiodeId, :type, :status, :skalBatches, :melding, :varselKvitteringStatus, :lenke, CURRENT_TIMESTAMP)
        """.trimIndent(), params)

        if (pendingMinsideVarsel is VarselOmNyMelding) {
            kobleTilDialog(pendingMinsideVarsel)
        }
    }

    open fun setDialogVarslerTilSkalAvsluttes(dialogId: Long) {
        val varselFor = getVarslerForDialog(dialogId)
        varselFor.forEach { updateStatus(it.varselId, SKAL_AVSLUTTES) }
    }

    open fun getVarslerForDialog(dialogId: Long): List<DialogVarselStatus> {
        val params = mapOf("dialogId" to dialogId)
        val sql = """SELECT mapping.varsel_id, min_side_varsel.status, min_side_varsel.opprettet 
            |    FROM min_side_varsel_dialog_mapping mapping JOIN min_side_varsel 
            |        ON min_side_varsel.varsel_id = mapping.varsel_id
            |    WHERE dialog_id = :dialogId""".trimMargin()
        return try {
            template.query(sql, params) { rs, _ ->
                DialogVarselStatus(
                    rs.getVarselId(),
                    rs.getStatus(),
                    DatabaseUtils.hentLocalDateTime(rs, "opprettet")
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            emptyList()
        }
    }

    private fun kobleTilDialog(varselOmNyMelding: VarselOmNyMelding) {
        val params = mapOf(
            "varselId" to varselOmNyMelding.varselId.value,
            "dialogId" to varselOmNyMelding.dialogId,
        )
        val sql = """
            INSERT INTO min_side_varsel_dialog_mapping(varsel_id, dialog_id)
            VALUES (:varselId, :dialogId)
        """.trimIndent()
        template.update(sql, params)
    }
}

class DialogVarselStatus(
    val varselId: MinSideVarselId,
    val status: BrukernotifikasjonBehandlingStatus,
    val opprettet: LocalDateTime,
)

data class PendingMinsideVarsel(
    val varselId: MinSideVarselId,
    val lenke: URL,
    val skalBatches: Boolean,
    val type: BrukernotifikasjonsType,
    val melding: String,
    val fnr: Fnr
)