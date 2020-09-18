package no.nav.fo.veilarbdialog;

import no.nav.common.utils.Credentials;
import no.nav.common.utils.NaisUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContextRefreshedEventListener {

/*    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {

        // TODO: Fix this the NAIS way, see https://github.com/navikt/vault-iac/blob/master/doc/nais/integration.md#mounting-managed-secrets.
        Credentials c = NaisUtils.getCredentials("srvveilarbdialog");
        System.setProperty("srvveilarbdialog", c.username);
        System.setProperty("srvveilarbdialog", c.password);

    }*/

}
