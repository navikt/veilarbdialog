package no.nav.fo.veilarbdialog.oversiktenVaas

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OversiktenUtboksMeldingTest {

    @Test
    fun `Melding for utgått varsel skal ha riktige verdier`() {
        val fnr = "1234567891234"
        val melding = OversiktenUtboksMelding.forUtgattVarsel(fnr)
        assertThat(melding.kategori).isEqualTo(Kategori.UTGATT_VARSEL)
        assertThat(melding.avsender).isEqualTo("veilarbdialog")
        assertThat(melding.personID).isEqualTo(fnr)
        assertThat(melding.operasjon).isEqualTo(Operasjon.START)
        assertThat(melding.hendelse.beskrivelse).isEqualTo("Bruker har et utgått varsel")
        assertThat(melding.hendelse.lenke).isEqualTo("LENKE")
        assertThat(melding.hendelse.dato).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.MILLIS))
    }
}