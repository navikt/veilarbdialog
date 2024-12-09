package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbDialogSqlParameterSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Types
import java.util.*

@Repository
open class OversiktenMeldingMedMetadataRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagre(oversiktenMeldingMedMetadata: OversiktenMeldingMedMetadata) {
        val sql = """ 
            INSERT INTO oversikten_melding_med_metadata (
                    fnr, opprettet, utsending_status, melding, kategori, melding_key)
            VALUES ( :fnr, :opprettet, :utsending_status::OVERSIKTEN_UTSENDING_STATUS, :melding::json, :kategori, :melding_key)
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("fnr", oversiktenMeldingMedMetadata.fnr.get())
            addValue("opprettet", oversiktenMeldingMedMetadata.opprettet)
            addValue("utsending_status", oversiktenMeldingMedMetadata.utsendingStatus.name)
            addValue("melding", oversiktenMeldingMedMetadata.meldingSomJson)
            addValue("kategori", oversiktenMeldingMedMetadata.kategori.name)
            addValue("melding_key", oversiktenMeldingMedMetadata.meldingKey)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<LagretOversiktenMeldingMedMetadata> {
        val sql = """
            SELECT * FROM oversikten_melding_med_metadata WHERE utsending_status IN ('SKAL_STARTES', 'SKAL_STOPPES')
        """.trimIndent()

        return jdbc.query(sql, rowMapper)
    }

    open fun hent(meldingKey: MeldingKey, operasjon: OversiktenMelding.Operasjon): List<LagretOversiktenMeldingMedMetadata> {
        val sql = """
            select * from oversikten_melding_med_metadata
            where melding_key = :melding_key
            and melding->>'operasjon' = :operasjon
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("melding_key", meldingKey)
            addValue("operasjon", operasjon.name)
        }

        return jdbc.query(sql, params, rowMapper)
    }

    open fun markerSomSendt(id: Long) {
        val sql = """
           UPDATE oversikten_melding_med_metadata
           SET utsending_status = 'SENDT',
           tidspunkt_sendt = now()
           WHERE id = :id
        """.trimIndent()

        val params = VeilarbDialogSqlParameterSource().apply {
            addValue("id", id)
        }

        jdbc.update(sql, params)
    }

    open val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        LagretOversiktenMeldingMedMetadata(
            id = rs.getLong("id"),
            fnr = Fnr.of(rs.getString("fnr")),
            opprettet = DatabaseUtils.hentZonedDateTime(rs, "opprettet"),
            utsendingStatus = UtsendingStatus.valueOf(rs.getString("utsending_status")),
            meldingSomJson = rs.getString("melding"),
            kategori = OversiktenMelding.Kategori.valueOf(rs.getString("kategori")),
            meldingKey = UUID.fromString(rs.getString("melding_key")),
            tidspunktSendt = DatabaseUtils.hentZonedDateTime(rs, "tidspunkt_sendt")
        )
    }
}