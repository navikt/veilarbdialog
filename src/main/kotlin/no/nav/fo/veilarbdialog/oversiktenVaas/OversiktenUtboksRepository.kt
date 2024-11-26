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
            INSERT INTO oversikten_vaas_utboks (
                    fnr, opprettet, tidspunkt_sendt, utsending_status, melding, kategori, melding_key)
            VALUES ( :fnr, :opprettet, :tidspunkt_sendt, :utsending_status, :melding::json, :kategori, :melding_key)
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("fnr", sendingEntity.fnr.get())
            addValue("opprettet", sendingEntity.opprettet)
            addValue("tidspunkt_sendt", sendingEntity.tidspunktSendt)
            addValue("utsending_status", sendingEntity.utsendingStatus.name)
            addValue("melding", sendingEntity.meldingSomJson)
            addValue("kategori", sendingEntity.kategori.name)
            addValue("melding_key", sendingEntity.meldingKey)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<SendingEntity> {
        val sql = """
            SELECT * FROM oversikten_vaas_utboks WHERE utsending_status = 'SKAL_SENDES'
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun markerSomSendt(meldingKey: UUID) {
        val sql = """
           UPDATE oversikten_vaas_utboks
           SET utsending_status = 'SENDT',
           tidspunkt_sendt = now()
           WHERE melding_key = :melding_key
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("melding_key", meldingKey)
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
            kategori = OversiktenUtboksService.Kategori.valueOf(rs.getString("kategori")),
            meldingKey = UUID.fromString(rs.getString("melding_key"))
        )
    }
}