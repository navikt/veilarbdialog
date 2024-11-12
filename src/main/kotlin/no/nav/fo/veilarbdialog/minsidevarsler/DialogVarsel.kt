package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import java.net.URL
import java.util.UUID

class DialogVarsel(
    val varselId: MinSideVarselId ,
    val dialogId: Long ,
    val foedselsnummer: Fnr ,
    val melding: String ,
    val oppfolgingsperiodeId: UUID ,
    val type: BrukernotifikasjonsType,
    val link: URL
) {

    companion object {
        fun varselOmMuligStans(
            dialogId: Long,
            fnr: Fnr,
            oppfolgingsperiode: UUID,
            link: URL
        ): DialogVarsel {
            return DialogVarsel(
                MinSideVarselId(UUID.randomUUID()),
                dialogId,
                fnr,
                BrukernotifikasjonTekst.NAV_VURDERER_Å_STANSE_PENGENE_DINE_TEKST,
                oppfolgingsperiode,
                BrukernotifikasjonsType.OPPGAVE,
                link
            )
        }

        fun varselOmNyMelding(
            dialogId: Long,
            fnr: Fnr,
            oppfolgingsperiode: UUID,
            link: URL
        ): DialogVarsel {
            return DialogVarsel(
                MinSideVarselId(UUID.randomUUID()),
                dialogId,
                fnr,
                BrukernotifikasjonTekst.NY_MELDING_TEKST,
                oppfolgingsperiode,
                BrukernotifikasjonsType.BESKJED,
                link,
            )
        }
    }

}