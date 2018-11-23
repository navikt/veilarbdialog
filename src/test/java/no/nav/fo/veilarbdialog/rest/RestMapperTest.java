package no.nav.fo.veilarbdialog.rest;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;

public class RestMapperTest {
    
    private RestMapper restMapper;
    private KontorsperreFilter filter;

    @Before
    public void setUp() {
        restMapper = new RestMapper();
        filter = mock(KontorsperreFilter.class);
        restMapper.kontorsperreFilter = filter;
    }
    
    @Test
    public void skal_inneholde_alle_henvendelser_dersom_ikke_kontorsperret() {
        when(filter.harTilgang(null)).thenReturn(true);
        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(1, nyHenvendelse(1, null)));

        assertThat(dialogDto.henvendelser.size(), is(1));
    }
    
    private DialogData nyDialog(int id, HenvendelseData... henvendelser) {
        return DialogData.builder().id(id).henvendelser(asList(henvendelser)).build();
    }

    private HenvendelseData nyHenvendelse(int id, String kontorsperreEnhetId) {
        return HenvendelseData.builder().id(id).sendt(new Date()).kontorsperreEnhetId(kontorsperreEnhetId).build();
    }

    @Test
    public void skal_inneholde_kontorsperrede_henvendelser_dersom_tilgang() {
        String kontorsperretEnhet = "123";
        when(filter.harTilgang(any())).thenReturn(true);
        when(filter.harTilgang(kontorsperretEnhet)).thenReturn(true);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(1, nyHenvendelse(1, null),nyHenvendelse(2, kontorsperretEnhet),nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size(), is(3));
        assertThat(dialogDto.henvendelser.stream().filter(h -> ("2".equals(h.id))).collect(Collectors.toList()).isEmpty(), is(false));
    }
    
    @Test
    public void skal_fjerne_kontorsperrede_henvendelser_dersom_ikke_tilgang() {
        String kontorsperretEnhet = "123";
        when(filter.harTilgang(any())).thenReturn(true);
        when(filter.harTilgang(kontorsperretEnhet)).thenReturn(false);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(1, nyHenvendelse(1, null),nyHenvendelse(2, kontorsperretEnhet),nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size(), is(2));
        assertThat(dialogDto.henvendelser.stream().filter(h -> ("2".equals(h.id))).collect(Collectors.toList()).isEmpty(), is(true));
    }
}
