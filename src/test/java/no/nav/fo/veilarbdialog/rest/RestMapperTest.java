// TODO: 07/12/2020 fiks denne

/*
package no.nav.fo.veilarbdialog.rest;

import no.nav.fo.veilarbdialog.domain.DialogDTO;
import no.nav.fo.veilarbdialog.domain.DialogData;
import no.nav.fo.veilarbdialog.domain.HenvendelseData;
import no.nav.fo.veilarbdialog.kvp.KontorsperreFilter;
import no.nav.fo.veilarbdialog.auth.AuthService;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RestMapperTest {

    private RestMapper restMapper;
    private KontorsperreFilter filter;
    private AuthService auth;

    @Before
    public void setUp() {
        filter = mock(KontorsperreFilter.class);
        auth = mock(AuthService.class);
        restMapper = new RestMapper(filter, auth);
    }

    @Test
    public void skal_inneholde_alle_henvendelser_dersom_ikke_kontorsperret() {
        when(filter.filterKontorsperre(null, null)).thenReturn(true);
        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null)));

        assertThat(dialogDto.henvendelser.size()).isEqualTo(1);
    }

    private DialogData nyDialog(HenvendelseData... henvendelser) {
        return DialogData.builder().id(1).henvendelser(asList(henvendelser)).build();
    }

    private HenvendelseData nyHenvendelse(int id, String kontorsperreEnhetId) {
        return HenvendelseData.builder().id(id).sendt(new Date()).kontorsperreEnhetId(kontorsperreEnhetId).build();
    }

    @Test
    public void skal_inneholde_kontorsperrede_henvendelser_dersom_tilgang() {
        String kontorsperretEnhet = "123";
        when(filter.filterKontorsperre(any(), any())).thenReturn(true);
        when(filter.filterKontorsperre(any(), eq(kontorsperretEnhet))).thenReturn(true);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null), nyHenvendelse(2, kontorsperretEnhet), nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size()).isEqualTo(3);
        assertThat(dialogDto.henvendelser.stream().noneMatch(h -> ("2".equals(h.id)))).isFalse();
    }

    @Test
    public void skal_fjerne_kontorsperrede_henvendelser_dersom_ikke_tilgang() {
        String kontorsperretEnhet = "123";
        when(filter.filterKontorsperre(any(), any())).thenReturn(true);
        when(filter.filterKontorsperre(any(), eq(kontorsperretEnhet))).thenReturn(false);

        DialogDTO dialogDto = restMapper.somDialogDTO(nyDialog(nyHenvendelse(1, null), nyHenvendelse(2, kontorsperretEnhet), nyHenvendelse(3, "")));
        assertThat(dialogDto.henvendelser.size()).isEqualTo(2);
        assertThat(dialogDto.henvendelser.stream().noneMatch(h -> ("2".equals(h.id)))).isTrue();
    }
}
*/
