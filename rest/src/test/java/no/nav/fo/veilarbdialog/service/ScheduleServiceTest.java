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
        String melding = ScheduleService.marshall(new ObjectFactory().createStoppReVarsel(stoppReVarsel), ScheduleService.STOPP_VARSEL_CONTEXT);
        assertThat(melding, containsString("UUID-123"));
    }
    
    @Test
    public void skalMarshalleVarselMeldingUtenException() {
        String melding = ScheduleService.marshall(ScheduleService.lagNyttVarsel("aktoer-123", "meldingsid-123"), ScheduleService.VARSEL_CONTEXT);
        assertThat(melding, containsString("aktoer-123"));
        assertThat(melding, containsString("meldingsid-123"));
    }
}
