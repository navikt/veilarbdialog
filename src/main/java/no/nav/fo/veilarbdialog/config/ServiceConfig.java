package no.nav.fo.veilarbdialog.config;

import no.nav.fo.veilarbdialog.rest.KasserRessurs;
import no.nav.fo.veilarbdialog.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        AppService.class,
        AutorisasjonService.class,
        DialogStatusService.class,
        OppgaveService.class,
        ServiceMeldingService.class,
        StopRevarslingService.class,
        VarselMedHandlingService.class,
        KasserRessurs.class,
})
public class ServiceConfig {
}
