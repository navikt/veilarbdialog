package no.nav.fo.veilarbdialog.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;

public class ScheduleServiceTest {

    @Test
    public void skalMarshalleStoppVarselMeldingUtenException() {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId("UUID-123");
        String melding = Utils.marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), ScheduleService.STOPP_VARSEL_CONTEXT);
        assertThat(melding, containsString("UUID-123"));
    }

    @Test
    public void skalMarshalleVarselMeldingUtenException() {
        String melding = Utils.marshall(ServiceMeldingService.lagNyttVarsel("aktoer-123"), ScheduleService.VARSEL_CONTEXT);
        assertThat(melding, containsString("aktoer-123"));
        assertThat(melding, containsString("DittNAV_000007"));
    }
}
