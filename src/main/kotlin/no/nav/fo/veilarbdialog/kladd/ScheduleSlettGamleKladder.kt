package no.nav.fo.veilarbdialog.kladd

import no.nav.fo.veilarbdialog.service.KladdService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduleSlettGamleKladder(
    private val kladdService: KladdService
) {

    // Ti min
    @Scheduled(fixedDelay = 600000)
    open fun slettGamleKladder() {
        kladdService.slettGamleKladder()
    }
}