package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.jdbc.VarselRepository;
import no.nav.fo.veilarbdialog.domain.AvsenderType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VarselDAO {

    private final VarselRepository varselRepository;

    public List<String> hentAktorerMedUlesteMeldingerEtterSisteVarsel(long graceMillis) {
        LocalDateTime grense = LocalDateTime.ofInstant(
                new Date(System.currentTimeMillis() - graceMillis).toInstant(), ZoneId.systemDefault());
        return varselRepository.findAktorerMedUlesteMeldingerEtterSisteVarsel(
                AvsenderType.VEILEDER.name(), grense);
    }

    public void oppdaterSisteVarselForBruker(String aktorId) {
        var rowsUpdated = varselRepository.oppdaterSisteVarsel(aktorId);
        if (rowsUpdated == 0) {
            opprettVarselForBruker(aktorId);
        }
    }

    private void opprettVarselForBruker(String aktorId) {
        varselRepository.insert(aktorId);
    }
}
