package no.nav.fo.veilarbdialog.eventsLogger

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class AntallUtgattDAO(
    val template: JdbcTemplate
) {
    open fun hentAntallUtgåtteVarsler(): Int {
        val sql = """
            select count(*) as antallUtgåtte from eskaleringsvarsel 
            where gjeldende is not null 
            and opprettet_dato > now() - INTERVAL '10 days';
        """.trimIndent()
        return template.queryForObject(sql, Int::class.java ) ?: 0
    }
}