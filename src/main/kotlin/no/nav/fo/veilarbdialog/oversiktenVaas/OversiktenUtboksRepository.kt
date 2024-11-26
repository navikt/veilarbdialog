package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.fo.veilarbdialog.util.DatabaseUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
open class OversiktenUtboksRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagreSending(sending: Sending) {
        val sql =  """ 
            INSERT INTO oversikten_vaas_utboks (
                    fnr, opprettet, tidspunkt_sendt, utsending_status, melding)
            VALUES ( :fnr, :opprettet, :tidspunkt_sendt, :utsending_status, :melding)
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("fnr", sending.fnr)
            addValue("opprettet", sending.opprettet)
            addValue("tidspunkt_sendt", sending.tidspunktSendt)
            addValue("utsending_status", sending.utsendingStatus)
            addValue("melding", sending.melding)
        }

        jdbc.update(sql, params)
    }

    open fun hentAlleSomSkalSendes(): List<Sending> {
        val sql =  """
            SELECT * FROM oversikten_vaas_utboks WHERE utsending_status = SKAL_SENDES
        """.trimIndent()

        return emptyList()
    }
}