package no.nav.fo.veilarbdialog.eskaleringsvarsel;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.fo.veilarbdialog.db.jdbc.EskaleringsvarselJdbcRepository;
import no.nav.fo.veilarbdialog.db.jdbc.EskaleringsvarselRow;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.entity.EskaleringsvarselEntity;
import no.nav.fo.veilarbdialog.eskaleringsvarsel.exceptions.AktivEskaleringException;
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toLocalDateTime;
import static no.nav.fo.veilarbdialog.db.jdbc.JdbcConverters.toZonedDateTime;

@Repository
@RequiredArgsConstructor
public class EskaleringsvarselRepository {

    private final EskaleringsvarselJdbcRepository repository;

    private static EskaleringsvarselEntity mapRow(EskaleringsvarselRow row) {
        return new EskaleringsvarselEntity(
                row.getId(),
                row.getTilhorendeDialogId(),
                Optional.ofNullable(row.getTilhorendeMinsideVarsel()).map(MinSideVarselId::new).orElse(null),
                row.getAktorId(),
                row.getOpprettetAv(),
                toZonedDateTime(row.getOpprettetDato()),
                row.getOpprettetBegrunnelse(),
                toZonedDateTime(row.getAvsluttetDato()),
                row.getAvsluttetAv(),
                row.getAvsluttetBegrunnelse(),
                row.getOversiktenMeldingKey());
    }

    public EskaleringsvarselEntity opprett(long tilhorendeDialogId, MinSideVarselId varselId, String aktorId, String opprettetAv, String opprettetBegrunnelse) {
        ZonedDateTime opprettetDato = ZonedDateTime.now();

        Long key;
        try {
            key = repository.insertAndReturnId(
                    aktorId,
                    opprettetAv,
                    toLocalDateTime(opprettetDato),
                    tilhorendeDialogId,
                    varselId.getValue(),
                    opprettetBegrunnelse);
        } catch (DuplicateKeyException dke) {
            throw new AktivEskaleringException("Pågående start-eksalering.");
        }

        return new EskaleringsvarselEntity(
                key,
                tilhorendeDialogId,
                varselId,
                aktorId,
                opprettetAv,
                opprettetDato,
                opprettetBegrunnelse,
                null,
                null,
                null,
                null
                );
    }

    public void stop(long varselId, String begrunnelse, NavIdent avsluttetAv) {
        int update = repository.stop(varselId, toLocalDateTime(ZonedDateTime.now()), avsluttetAv.get(), begrunnelse);
        assert update == 1;
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(AktorId aktorId) {
        return repository.findGjeldende(aktorId.get()).map(EskaleringsvarselRepository::mapRow);
    }

    public Optional<EskaleringsvarselEntity> hentGjeldende(UUID oppfolgingsperiodeUuid) {
        return repository.findGjeldendeForPeriode(oppfolgingsperiodeUuid.toString())
                .map(EskaleringsvarselRepository::mapRow);
    }

    public boolean stopPeriode(UUID oppfolgingsperiodeUuid) {
        int rowsUpdated = repository.stopPeriode(oppfolgingsperiodeUuid.toString());
        return rowsUpdated != 0;
    }

    public Optional<EskaleringsvarselEntity> hentVarsel(long varselId) {
        return repository.findById(varselId).map(EskaleringsvarselRepository::mapRow);
    }

    public List<EskaleringsvarselEntity> hentUsendteGjeldendeVarslerEldreEnn(LocalDateTime tidspunkt) {
        return repository.findUsendteGjeldendeVarslerEldreEnn(tidspunkt)
                .stream()
                .map(EskaleringsvarselRepository::mapRow)
                .toList();
    }

    public List<EskaleringsvarselEntity> hentHistorikk(AktorId aktorId) {
        return repository.findHistorikk(aktorId.get())
                .stream()
                .map(EskaleringsvarselRepository::mapRow)
                .toList();
    }

    public void knyttVarselTilOversiktenMelding(long varselId, UUID oversiktenSendingMeldingKey) {
        repository.knyttTilOversiktenMelding(varselId, oversiktenSendingMeldingKey);
    }
}
