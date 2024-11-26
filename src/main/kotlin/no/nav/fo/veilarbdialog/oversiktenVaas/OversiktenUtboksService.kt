package no.nav.fo.veilarbdialog.oversiktenVaas

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class OversiktenUtboksService(
    private val aktorOppslagClient: AktorOppslagClient,
    private val  oversiktenUtboksRepository: OversiktenUtboksRepository
) {

    fun sendMeldingTilOversikten(gjeldendeEskaleringsvarsler: List<EskaleringsvarselEntity> ) {
        gjeldendeEskaleringsvarsler.forEach {
            val fnr =  aktorOppslagClient.hentFnr(AktorId(it.aktorId))
            val melding = it.tilMelding(Operasjon.START, fnr)
            val sendingEntity = SendingEntity(meldingSomJson = JsonUtils.toJson(melding), fnr = fnr)
            oversiktenUtboksRepository.lagreSending(sendingEntity)
        }

    }

    private fun EskaleringsvarselEntity.tilMelding(operasjon: Operasjon, fnr: Fnr) =
        OversiktenUtboksMelding(
            personID = fnr.get(),
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