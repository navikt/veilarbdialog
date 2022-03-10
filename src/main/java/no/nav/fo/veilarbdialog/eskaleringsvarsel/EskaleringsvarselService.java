package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.fo.veilarbdialog.auth.AuthService;
import no.nav.fo.veilarbdialog.domain.*;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.service.DialogDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EskaleringsvarselService {

    private final EskaleringsvarselRepository eskaleringsvarselRepository;
    private final DialogDataService dialogDataService;
    private final AuthService authService;

    @Transactional
    public EskaleringsvarselEntity start(Fnr fnr, String begrunnelse, String overskrift, String tekst) {
        authService.skalVereInternBruker();
        DialogData dialogData;
        NyHenvendelseDTO nyHenvendelseDTO = new NyHenvendelseDTO()
                .setTekst(tekst)
                .setOverskrift(overskrift)
                .setEgenskaper(List.of(Egenskap.ESKALERINGSVARSEL));

        dialogData = dialogDataService.opprettHenvendelse(nyHenvendelseDTO, Person.fnr(fnr.get()));
        // TODO filtrer kontorsperre før retur

        var dialogStatus = DialogStatus.builder()
                .dialogId(dialogData.getId())
                .venterPaSvar(true)
                .build();

        dialogData = dialogDataService.oppdaterVentePaSvarTidspunkt(dialogStatus);


        dialogDataService.oppdaterFerdigbehandletTidspunkt(dialogData.getId(), true);
        dialogDataService.sendPaaKafka(dialogData.getAktorId());

        dialogDataService.markerDialogSomLest(dialogData.getId());

        EskaleringsvarselEntity eskaleringsvarselEntity =  eskaleringsvarselRepository.opprett(dialogData.getId(), dialogData.getAktorId(), authService.getIdent().orElseThrow(), begrunnelse);


        /*
        opprett henvendelse                                 v
        sett ferdigbehandlet og venter på svar fra bruker   v
        lagre eskaleringsvarselet                           v
        bestille brukernotifikasjon
         */
        return eskaleringsvarselEntity;
    }

    public void stop(String fnr, String begrunnelse, String tekst) {

    }

    public EskaleringsvarselEntity hentGjeldende(Fnr fnr) {
        return null;
    }

    public List<EskaleringsvarselEntity> historikk(Fnr fnr) {
        return null;
    }

}
