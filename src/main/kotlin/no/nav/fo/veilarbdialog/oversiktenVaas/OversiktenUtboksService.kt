package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class OversiktenUtboksService(
) {

    fun sendMeldingTilOversikten(gjeldendeEskaleringsvarsler: List<EskaleringsvarselEntity> ) {
        gjeldendeEskaleringsvarsler.forEach {
            val melding = it.tilMelding(Operasjon.START)
        }

    }

    private fun EskaleringsvarselEntity.tilMelding(operasjon: Operasjon) =
        OversiktenUtboksMelding(
            personID = this.aktorId, // TODO: map til fnr
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = operasjon,
            hendelse = Hendelse(
                beskrivelse = "Bruker har et utgått varsel",
                dato = LocalDateTime.now(),
                lenke = "url", // TODO: fiks ordentlig url
            )
        )

    data class OversiktenUtboksMelding(
        val personID: String,
        val avsender: String = "veilarbdialog",
        val kategori: Kategori,
        val operasjon: Operasjon,
        val hendelse: Hendelse
    )

    data class Hendelse (
        val beskrivelse: String,
        val dato: LocalDateTime,
        val lenke: String,
        val detaljer: String? = null,
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