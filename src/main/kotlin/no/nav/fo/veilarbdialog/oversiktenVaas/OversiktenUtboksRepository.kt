package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenUtboksRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagreSending(sendingEntity: SendingEntity) {
        val sql = """ 
            INSERT INTO oversikten_utboks (
                    fnr, opprettet, tidspunkt_sendt, utsending_status, melding, kategori, uuid)
            VALUES ( :fnr, :opprettet, :tidspunkt_sendt, :utsending_status, :melding::json, :kategori, :uuid)
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("fnr", sendingEntity.fnr.get())
            addValue("opprettet", sendingEntity.opprettet)
            addValue("tidspunkt_sendt", sendingEntity.tidspunktSendt)
            addValue("utsending_status", sendingEntity.utsendingStatus.name)
            addValue("melding", sendingEntity.meldingSomJson)
            addValue("kategori", sendingEntity.kategori.name)
            addValue("uuid", sendingEntity.uuid)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<SendingEntity> {
        val sql = """
            SELECT * FROM oversikten_utboks WHERE utsending_status = 'SKAL_SENDES'
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun hentSendinger(fnr: Fnr, kategori: OversiktenMelding.Kategori, operasjon: OversiktenMelding.Operasjon): List<SendingEntity> {
        val sql = """
            SELECT * FROM oversikten_utboks
            WHERE fnr = :fnr
            AND kategori = :kategori
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("fnr", fnr.get())
            addValue("kategori", kategori.name)
//            addValue("operasjon", operasjon.name)
        }

        return jdbc.queryForList(sql, params, SendingEntity::class.java)
    }

    open fun markerSomSendt(uuid: UUID) {
        val sql = """
           UPDATE oversikten_utboks
           SET utsending_status = 'SENDT',
           tidspunkt_sendt = now()
           WHERE uuid = :uuid
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("uuid", uuid)
        }

        jdbc.update(sql, params)
    }

    private val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        SendingEntity(
            fnr = Fnr.of(rs.getString("fnr")),
            opprettet = DatabaseUtils.hentZonedDateTime(rs, "opprettet"),
            tidspunktSendt = DatabaseUtils.hentZonedDateTime(rs, "tidspunkt_sendt"),
            utsendingStatus = UtsendingStatus.valueOf(rs.getString("utsending_status")),
            meldingSomJson = rs.getString("melding"),
            kategori = OversiktenMelding.Kategori.valueOf(rs.getString("kategori")),
            uuid = UUID.fromString(rs.getString("uuid"))
        )
    }
}