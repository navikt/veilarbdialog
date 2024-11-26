package no.nav.fo.veilarbdialog.oversiktenVaas

import java.time.LocalDateTime


data class OversiktenUtboksMelding(
    val personID: String,
    val avsender: String = "veilarbdialog",
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: Hendelse
) {
    companion object {
        fun forUtgattVarsel(fnr: String) = OversiktenUtboksMelding(
            personID = fnr,
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = Hendelse(
                beskrivelse = "Bruker har et utgått varsel",
                dato = LocalDateTime.now(),
                lenke = "aktivitetsplan",
            )
        )
    }
}

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