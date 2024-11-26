package no.nav.fo.veilarbdialog.oversiktenVaas

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OversiktenMeldingTest {

    @Test
    fun `Melding for utgått varsel skal ha riktige verdier`() {
        val fnr = "1234567891234"
        val melding = OversiktenMelding.forUtgattVarsel(fnr = fnr, erProd = false)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UTGATT_VARSEL)
        assertThat(melding.avsender).isEqualTo("veilarbdialog")
        assertThat(melding.personID).isEqualTo(fnr)
        assertThat(melding.operasjon).isEqualTo(OversiktenMelding.Operasjon.START)
        assertThat(melding.hendelse.beskrivelse).isEqualTo("Bruker har et utgått varsel")
        assertThat(melding.hendelse.lenke).isEqualTo("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan")
        assertThat(melding.hendelse.dato).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.MILLIS))
    }

    @Test
    fun `Melding for utgått varsel skal ha riktig URL for prod`() {
        val fnr = "1234567891234"
        val melding = OversiktenMelding.forUtgattVarsel(fnr = fnr, erProd = true)
        assertThat(melding.hendelse.lenke).isEqualTo("https://veilarbpersonflate.intern.nav.no/aktivitetsplan")
    }
}
