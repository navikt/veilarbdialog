package no.nav.fo.veilarbdialog.oversiktenVaas

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.fo.veilarbdialog.eskaleringsvarsel.EskaleringsvarselService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
open class OversiktenUtboksService(
    private val eskaleringsvarselService: EskaleringsvarselService
) {

    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "oversikten_utboks_scheduledTask", lockAtMostFor = "PT2M")
    open fun sendMeldingTilOversikten() {

    }


}