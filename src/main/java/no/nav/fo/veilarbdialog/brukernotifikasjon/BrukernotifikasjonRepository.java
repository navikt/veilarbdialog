package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.brukernotifikasjon.entity.BrukernotifikasjonEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BrukernotifikasjonRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    BrukernotifikasjonEntity opprettBrukernotifikasjon(BrukernotifikasjonInsert insert) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", insert.brukernotifikasjonId().toString())
                .addValue("dialog_id", insert.dialogId())
                .addValue("foedselsnummer", insert.foedselsnummer().get())
                .addValue("oppfolgingsperiode", insert.oppfolgingsperiodeId().toString())
                .addValue("type", insert.type().name())
                .addValue("status", insert.status().name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", insert.epostTitel())
                .addValue("epostBody", insert.epostBody())
                .addValue("smsTekst", insert.smsTekst())
                .addValue("melding", insert.melding());

        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        ( brukernotifikasjon_id,  DIALOG_ID,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          melding,  smsTekst,  epostTittel,  epostBody) " +
                        " VALUES (:brukernotifikasjon_id, :aktivitet_id, :opprettet_paa_aktivitet_version, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params);

        return null;
    }

}
