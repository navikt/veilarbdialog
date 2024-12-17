package no.nav.fo.veilarbdialog.minsidevarsler

import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.KvitteringMetrikk
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
open class VarselUtenKvitteringMetrikkCron(
    val minsideVarselDao: MinsideVarselDao,
    val brukernotifikasjonRepository: BrukernotifikasjonRepository,
    val kvitteringMetrikk: KvitteringMetrikk
) {

    @Scheduled(
        initialDelay = 60000,
        fixedDelay = 30000
    )
    open fun countForsinkedeVarslerSisteDognet() {
        val antallGamle = brukernotifikasjonRepository.hentAntallUkvitterteVarslerForsoktSendt(20)
        val antallNye = minsideVarselDao.hentAntallUkvitterteVarslerForsoktSendt(20)
        kvitteringMetrikk.countForsinkedeVarslerSisteDognet(antallGamle + antallNye)
    }
}