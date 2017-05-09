package no.nav.fo.veilarbdialog;

import no.nav.apiapp.ApiApplication;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbdialog")
public class ApplicationContext implements ApiApplication{

    @Override
    public Sone getSone() {
        return Sone.FSS;
    }

}
