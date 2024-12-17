package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonTekst
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonsType
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import java.net.URL
import java.util.UUID

sealed class DialogVarsel(
    val varselId: MinSideVarselId ,
    val foedselsnummer: Fnr ,
    val melding: String ,
    val oppfolgingsperiodeId: UUID ,
    val type: BrukernotifikasjonsType,
    val lenke: URL,
    val skalBatches: Boolean
) {
    class VarselOmMuligStans(
        varsel: MinSideVarselId,
        fnr: Fnr,
        melding: String,
        oppfolgingsperiodeId: UUID,
        type: BrukernotifikasjonsType,
        link: URL,
        skalBatches: Boolean
    ): DialogVarsel(varsel, fnr, melding, oppfolgingsperiodeId, type, link, skalBatches)
    class VarselOmNyMelding(
        val dialogId: Long,
        varsel: MinSideVarselId,
        fnr: Fnr,
        melding: String,
        oppfolgingsperiodeId: UUID,
        type: BrukernotifikasjonsType,
        link: URL,
        skalBatches: Boolean
    ): DialogVarsel(varsel, fnr, melding, oppfolgingsperiodeId, type, link, skalBatches)

    companion object {
        fun varselOmMuligStans(
            fnr: Fnr,
            oppfolgingsperiode: UUID,
            link: URL
        ): DialogVarsel {
            return VarselOmMuligStans(
                MinSideVarselId(UUID.randomUUID()),
                fnr,
                BrukernotifikasjonTekst.NAV_VURDERER_Å_STANSE_PENGENE_DINE_TEKST,
                oppfolgingsperiode,
                BrukernotifikasjonsType.OPPGAVE,
                link,
                false
            )
        }

        fun varselOmNyMelding(
            dialogId: Long,
            fnr: Fnr,
            oppfolgingsperiode: UUID,
            link: URL
        ): DialogVarsel {
            return VarselOmNyMelding(
                dialogId,
                MinSideVarselId(UUID.randomUUID()),
                fnr,
                BrukernotifikasjonTekst.NY_MELDING_TEKST,
                oppfolgingsperiode,
                BrukernotifikasjonsType.BESKJED,
                link,
                true
            )
        }
    }
}