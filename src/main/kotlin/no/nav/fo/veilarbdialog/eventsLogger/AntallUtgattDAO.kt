package no.nav.fo.veilarbdialog.eventsLogger

import no.nav.fo.veilarbdialog.db.jdbc.EskaleringsvarselJdbcRepository
import org.springframework.stereotype.Repository

@Repository
open class AntallUtgattDAO(
    private val repository: EskaleringsvarselJdbcRepository
) {
    open fun hentAntallUtgåtteVarsler(): Int {
        return repository.tellUtgaatteVarsler()
    }
}
