package no.nav.fo.veilarbdialog.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BrukernotifikasjonRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    void opprettBrukernotifikasjon(
            UUID brukernotifikasjonId,
            long dialogId,
            Fnr foedselsnummer,
            String melding,
            UUID oppfolgingsperiode,
            VarselType type,
            VarselStatus status,
            String epostTitel,
            String epostBody,
            String smsTekst
    ) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("brukernotifikasjon_id", brukernotifikasjonId.toString())
                .addValue("dialog_id", dialogId)
                .addValue("foedselsnummer", foedselsnummer.get())
                .addValue("oppfolgingsperiode", oppfolgingsperiode.toString())
                .addValue("type", type.name())
                .addValue("status", status.name())
                .addValue("varsel_kvittering_status", VarselKvitteringStatus.IKKE_SATT.name())
                .addValue("epostTittel", epostTitel)
                .addValue("epostBody", epostBody)
                .addValue("smsTekst", smsTekst)
                .addValue("melding", melding);



        jdbcTemplate.update("" +
                        " INSERT INTO brukernotifikasjon " +
                        "        ( brukernotifikasjon_id,  DIALOG_ID,  foedselsnummer,  oppfolgingsperiode,  type,  status,  varsel_kvittering_status, opprettet,          melding,  smsTekst,  epostTittel,  epostBody) " +
                        " VALUES (:brukernotifikasjon_id, :aktivitet_id, :opprettet_paa_aktivitet_version, :foedselsnummer, :oppfolgingsperiode, :type, :status, :varsel_kvittering_status, CURRENT_TIMESTAMP, :melding, :smsTekst, :epostTittel, :epostBody) ",
                params);
    }
}
