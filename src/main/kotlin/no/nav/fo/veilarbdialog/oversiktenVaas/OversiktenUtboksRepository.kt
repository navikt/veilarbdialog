package no.nav.fo.veilarbdialog.oversiktenVaas

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

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
            addValue("melding", sending.meldingSomJson)
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