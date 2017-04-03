package no.nav.fo.veilarbdialog.ws.provider;

import lombok.val;
import no.nav.fo.veilarbdialog.service.AppService;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitetsplan;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;
import java.util.Optional;

import static java.util.Optional.of;
import static no.nav.fo.veilarbdialog.domain.AktivitetStatus.aktivitetStatus;
import static no.nav.fo.veilarbdialog.ws.provider.SoapServiceMapper.mapTilAktivitet;
import static no.nav.fo.veilarbdialog.ws.provider.SoapServiceMapper.mapTilAktivitetData;

@WebService
@Service
public class SoapService implements BehandleAktivitetsplanV1 {

    @Inject
    private AppService appService;

    @Override
    public OpprettNyAktivitetResponse opprettNyAktivitet(OpprettNyAktivitetRequest opprettNyAktivitetRequest) {

        return Optional.of(opprettNyAktivitetRequest)
                .map(OpprettNyAktivitetRequest::getAktivitet)
                .map((aktivitet) -> {
                    val aktivitetData = mapTilAktivitetData(aktivitet);
                    val lagretAktivtet = appService.opprettNyAktivtet(aktivitet.getPersonIdent(), aktivitetData);
                    return mapTilAktivitet(aktivitet.getPersonIdent(), lagretAktivtet);
                })
                .map(SoapServiceMapper::mapTilOpprettNyAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        val wsHentAktiviteterResponse = new HentAktivitetsplanResponse();
        val aktivitetsplan = new Aktivitetsplan();
        wsHentAktiviteterResponse.setAktivitetsplan(aktivitetsplan);

        appService.hentAktiviteterForIdent(hentAktivitetsplanRequest.getPersonident())
                .stream()
                .map(aktivitet -> mapTilAktivitet(hentAktivitetsplanRequest.getPersonident(), aktivitet))
                .forEach(aktivitetsplan.getAktivitetListe()::add);

        return wsHentAktiviteterResponse;
    }

    @Override
    public HentEndringsLoggForAktivitetResponse hentEndringsLoggForAktivitet(HentEndringsLoggForAktivitetRequest hentEndringsLoggForAktivitetRequest) {
        val endringsloggResponse = new HentEndringsLoggForAktivitetResponse();
        val endringer = endringsloggResponse.getEndringslogg();

        of(hentEndringsLoggForAktivitetRequest)
                .map(HentEndringsLoggForAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(aktivietId -> appService.hentEndringsloggForAktivitetId(aktivietId))
                .ifPresent((endringslist) -> endringslist.stream()
                        .map(SoapServiceMapper::somEndringsLoggResponse)
                        .forEach(endringer::add)
                );
        return endringsloggResponse;
    }

    @Override
    public EndreAktivitetStatusResponse endreAktivitetStatus(EndreAktivitetStatusRequest endreAktivitetStatusRequest) {
        return of(endreAktivitetStatusRequest)
                .map((req) -> appService.oppdaterStatus(
                        Long.parseLong(req.getAktivitetId()),
                        aktivitetStatus(req.getStatus()))
                )
                .map((aktivtet) -> mapTilAktivitet("", aktivtet)) //TODO: fnr don't know it here
                .map(SoapServiceMapper::mapTilEndreAktivitetStatusResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public SlettAktivitetResponse slettAktivitet(SlettAktivitetRequest slettAktivitetRequest) {
        of(slettAktivitetRequest)
                .map(SlettAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .ifPresent(aktivitetId -> appService.slettAktivitet(aktivitetId));
        return new SlettAktivitetResponse();
    }

    @Override
    public void ping() {}
}

