package no.nav.fo.veilarbdialog.service;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.db.dao.DataVarehusDAO;
import no.nav.fo.veilarbdialog.db.dao.DialogDAO;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.kvp.KvpService;
import org.junit.*;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class DialogDataServiceTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String IDENT = "ident";
    private static final String AKTIVITET_ID = "aktivitetId";
    private static final String KONTORSPERRE_ENHET_ID = "1337";
    private static final NyHenvendelseDTO HENVENDELSE_DTO = new NyHenvendelseDTO();


    @Autowired
    DialogDAO dialogDAO;

    @MockBean
    AuthService authService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DialogStatusService dialogStatusService;

    @MockBean
    DataVarehusDAO dataVarehusDAO;

    //TODO hva burde vi gjøre her
    @MockBean
    KafkaProducerService kafkaProducerService;

    @MockBean
    KvpService kvpService;

    @Autowired
    AktorOppslagClient aktorOppslagClient;

    @Autowired
    private DialogDataService dialogDataService;

    @Before
    public void setup() {
        when(aktorOppslagClient.hentFnr(AktorId.of(AKTOR_ID))).thenReturn(Fnr.of(IDENT));
        when(aktorOppslagClient.hentAktorId(Fnr.of(IDENT))).thenReturn(AktorId.of(AKTOR_ID));
        when(authService.harTilgangTilPerson(AKTOR_ID)).thenReturn(true);
    }

    @After
    public void cleanUp() {
        jdbcTemplate.update("delete from HENVENDELSE");
        jdbcTemplate.update("delete from DIALOG");
        jdbcTemplate.update("delete from DIALOG_AKTOR");
    }

    @Test
    public void opprettDialog_kontorsperrePaBruker_returnererKontorsperretDialog() {
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(KONTORSPERRE_ENHET_ID);
        DialogData dialogData = dialogDataService.opprettDialog(HENVENDELSE_DTO, AKTOR_ID);
        Assert.assertEquals(KONTORSPERRE_ENHET_ID, dialogData.getKontorsperreEnhetId());
    }

    @Test
    public void opprettHenvendelse_kontorsperrePaBruker_returnererKontorsperretDialog() {
        when(authService.getIdent()).thenReturn(Optional.of(AKTOR_ID));
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(KONTORSPERRE_ENHET_ID);

        DialogData dialogData = dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("test"), Person.aktorId(AKTOR_ID));
        Assert.assertEquals(KONTORSPERRE_ENHET_ID, dialogData.getKontorsperreEnhetId());
    }

    @Test
    public void opprettHenvendelse_IkkeKontorsperrePaBruker_returnererNull() {
        when(authService.getIdent()).thenReturn(Optional.of(AKTOR_ID));
        when(kvpService.kontorsperreEnhetId(AKTOR_ID)).thenReturn(null);

        DialogData dialogData = dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("test"), Person.aktorId(AKTOR_ID));
        Assert.assertNull(dialogData.getKontorsperreEnhetId());
    }

    @Test(expected = ResponseStatusException.class)
    public void opprettHenvendelse_brukerManglerTilgangTilPerson_kasterException() {

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(authService).harTilgangTilPersonEllerKastIngenTilgang((AKTOR_ID));
        dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("test"), Person.aktorId(AKTOR_ID));
    }

    @Test(expected = ResponseStatusException.class)
    public void opprettHenvendelse_brukerManglerTilgangTilDialog_kasterException() {

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(authService).harTilgangTilPerson((AKTOR_ID));
        when(authService.getIdent()).thenReturn(Optional.of(IDENT));
        dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("test"), Person.aktorId(AKTOR_ID));
    }

    @Test
    public void publicMetoder_sjekkerOmBrukerHarTilgang() {
        when(authService.getIdent()).thenReturn(Optional.of(IDENT));

        DialogData dialogData = dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("tekst").setAktivitetId(AKTIVITET_ID), Person.aktorId(AKTOR_ID));

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(authService).harTilgangTilPersonEllerKastIngenTilgang((any()));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(authService).harTilgangTilPerson((any()));

        kasterException(
                ResponseStatusException.class,
                () -> dialogDataService.hentDialogMedTilgangskontroll(dialogData.getId()),
                () -> dialogDataService.hentDialogMedTilgangskontroll(Long.toString(dialogData.getId()), dialogData.getAktivitetId()),
                () -> dialogDataService.hentDialogForAktivitetId(dialogData.getAktivitetId()),
                () -> dialogDataService.hentDialogerForBruker(Person.aktorId(AKTOR_ID)),
                () -> dialogDataService.markerDialogSomLest(dialogData.getId()),
                () -> dialogDataService.opprettHenvendelse(HENVENDELSE_DTO.setTekst("tekst").setAktivitetId(AKTIVITET_ID), Person.aktorId(AKTOR_ID))
        );
    }

    private void kasterException(Class<? extends Exception> exceptionClass, Runnable... runnable) {
        Arrays.asList(runnable)
                .forEach(r -> Assertions.assertThrows(exceptionClass, r::run));
    }
}
