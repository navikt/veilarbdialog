package no.nav.fo.veilarbdialog.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonServiceTest {

    static AktorOppslagClient aktorOppslagClient = mock(AktorOppslagClient.class);
    static PersonService personService = new PersonService(aktorOppslagClient);

    @BeforeAll
    static void setup() {
        when(aktorOppslagClient.hentAktorId(any())).thenReturn(AktorId.of("123"));
        when(aktorOppslagClient.hentFnr(any())).thenReturn(Fnr.of("123"));
    }

    @BeforeEach
    public void clear() {
        clearInvocations(aktorOppslagClient);
    }

    @Test
    public void skal_ikke_gjore_oppslag_for_aktorId_hvis_aktorId_kommer_inn() {
        personService.getAktorIdForPersonBruker(AktorId.of("123"));
        verify(aktorOppslagClient, Mockito.times(0)).hentAktorId(any());
    }
    @Test
    public void skal_gjore_oppslag_for_aktorId_hvis_aktorId_kommer_inn() {
        personService.getAktorIdForPersonBruker(Fnr.of("123"));
        verify(aktorOppslagClient, Mockito.times(1)).hentAktorId(any());
    }
    @Test
    public void skal_ikke_gjore_oppslag_for_fnr_hvis_aktorId_kommer_inn() {
        personService.getFnrForAktorId(Fnr.of("123"));
        verify(aktorOppslagClient, Mockito.times(0)).hentFnr(any());
    }
    @Test
    public void skal_gjore_oppslag_for_fnr_hvis_aktorId_kommer_inn() {
        personService.getFnrForAktorId(AktorId.of("123"));
        verify(aktorOppslagClient, Mockito.times(1)).hentFnr(any());
    }
}