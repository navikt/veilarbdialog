package no.nav.fo.veilarbdialog;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.security.PepClient;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbdialog")
@Import(AbacContext.class)
public class ApplicationContext implements ApiApplication{

    @Override
    public Sone getSone() {
        return Sone.FSS;
    }

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClient(pep, "veilarb", ResourceType.VeilArbPerson);
    }

}
