package no.nav.fo.veilarbdialog.eventsLogger

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class BigQueryMetrikkCron(
    val bigQueryClient: BigQueryClient,
    private val antallUtgattDAO: AntallUtgattDAO,
) {

    @Scheduled(cron = "0 */10 * * * *") // TODO: sendre til cron = "@midnight" før prod
    @SchedulerLock(name = "utgåtteVarsler_bigquery_metrikker", lockAtMostFor = "PT2M")
    fun hentUtgåtteVarslerCron() {
        val antallUtgåtteVarsler = antallUtgattDAO.hentAntallUtgåtteVarsler()
        bigQueryClient.logAntallUtgåtteVarsler(antallUtgåtteVarsler)
    }

}