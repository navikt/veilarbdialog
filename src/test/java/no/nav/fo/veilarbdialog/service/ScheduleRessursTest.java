package no.nav.fo.veilarbdialog.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import no.nav.fo.veilarbdialog.util.MessageQueueUtils;
import org.junit.Test;

import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.ObjectFactory;
import no.nav.melding.virksomhet.stopprevarsel.v1.stopprevarsel.StoppReVarsel;

public class ScheduleRessursTest {

    @Test
    public void skalMarshalleStoppVarselMeldingUtenException() {
        StoppReVarsel stoppReVarsel = new StoppReVarsel();
        stoppReVarsel.setVarselbestillingId("UUID-123");
        String melding = MessageQueueUtils.marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), ScheduleRessurs.STOPP_VARSEL_CONTEXT);
        assertThat(melding, containsString("UUID-123"));
    }

    @Test
    public void skalMarshalleVarselMeldingUtenException() {
        String melding = MessageQueueUtils.marshall(ServiceMeldingService.lagNyttVarsel("aktoer-123"), ScheduleRessurs.VARSEL_CONTEXT);
        assertThat(melding, containsString("aktoer-123"));
        assertThat(melding, containsString("DittNAV_000007"));
    }
}
