package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class OversiktenUtboksService(
) {

    fun sendMeldingTilOversikten(eskaleringsvarsler: List<EskaleringsvarselEntity> ) {



    }
    data class OversiktenUtboksMelding(
        val personID: String,
        val avsender: String,
        val kategori: Kategori,
        val operasjon: Operasjon,
        val hendelse: Hendelse
    )

    data class Hendelse (
        val navn: String,
        val dato: LocalDateTime,
        val lenke: String,
        val detaljer: String?,
    )

    enum class Kategori {
        UTGATT_VARSEL
    }

    enum class Operasjon {
        START,
        OPPDATER,
        STOPP
    }
}