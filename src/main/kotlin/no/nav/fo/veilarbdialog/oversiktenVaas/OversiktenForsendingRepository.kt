package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenForsendingRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagreSending(oversiktenForsendingEntity: OversiktenForsendingEntity) {
        val sql = """ 
            INSERT INTO oversikten_forsending (
                    fnr, opprettet, tidspunkt_sendt, utsending_status, melding, kategori, melding_key)
            VALUES ( :fnr, :opprettet, :tidspunkt_sendt, :utsending_status, :melding::json, :kategori, :melding_key)
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("fnr", oversiktenForsendingEntity.fnr.get())
            addValue("opprettet", oversiktenForsendingEntity.opprettet)
            addValue("tidspunkt_sendt", oversiktenForsendingEntity.tidspunktSendt)
            addValue("utsending_status", oversiktenForsendingEntity.utsendingStatus.name)
            addValue("melding", oversiktenForsendingEntity.meldingSomJson)
            addValue("kategori", oversiktenForsendingEntity.kategori.name)
            addValue("melding_key", oversiktenForsendingEntity.meldingKey)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<OversiktenForsendingEntity> {
        val sql = """
            SELECT * FROM oversikten_forsending WHERE utsending_status = 'SKAL_SENDES'
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun hentForsendinger(meldingKey: MeldingKey): List<OversiktenForsendingEntity> {
        val sql = """
            select * from oversikten_forsending
            where melding_key = :melding_key
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("melding_key", meldingKey)
        }

        return jdbc.queryForList(sql, params, OversiktenForsendingEntity::class.java)
    }

    open fun markerSomSendt(meldingKey: MeldingKey) {
        val sql = """
           UPDATE oversikten_forsending
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
        OversiktenForsendingEntity(
            fnr = Fnr.of(rs.getString("fnr")),
            opprettet = DatabaseUtils.hentZonedDateTime(rs, "opprettet"),
            tidspunktSendt = DatabaseUtils.hentZonedDateTime(rs, "tidspunkt_sendt"),
            utsendingStatus = UtsendingStatus.valueOf(rs.getString("utsending_status")),
            meldingSomJson = rs.getString("melding"),
            kategori = OversiktenMelding.Kategori.valueOf(rs.getString("kategori")),
            meldingKey = UUID.fromString(rs.getString("melding_key"))
        )
    }
}