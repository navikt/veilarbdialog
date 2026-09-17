package no.nav.fo.veilarbdialog.db.dao;

import lombok.RequiredArgsConstructor;
import no.nav.fo.veilarbdialog.db.jdbc.KladdRepository;
import no.nav.fo.veilarbdialog.domain.Kladd;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class KladdDAO {

    private final KladdRepository kladdRepository;

    @Transactional
    public void upsertKladd(Kladd kladd) {
        Long id = Optional.ofNullable(kladd.getDialogId()).map(Long::parseLong).orElse(null);
        long kladdSeq = kladdRepository.nextKladdSeq();

        kladdRepository.insert(
                kladd.getAktorId(),
                id,
                kladd.getAktivitetId(),
                kladd.getOverskrift(),
                kladd.getTekst(),
                kladd.getLagtInnAv(),
                LocalDateTime.now(),
                kladdSeq
        );

        //henvendelser på eksisterende tråer har ikke aktivitetId
        kladdRepository.deleteEldreEnnUniqueSeq(
                kladdSeq,
                kladd.getLagtInnAv(),
                kladd.getAktorId(),
                id,
                kladd.getAktivitetId()
        );
    }

    public List<Kladd> getKladder(String aktorId, String lagtInnAv) {
        return kladdRepository.findByAktorIdAndLagtInnAv(aktorId, lagtInnAv)
                .stream()
                .map(kladd -> Kladd.builder()
                        .aktorId(kladd.getAktorId())
                        .dialogId(kladd.getDialogId() == null ? null : kladd.getDialogId().toString())
                        .aktivitetId(kladd.getAktivitetId())
                        .overskrift(kladd.getOverskrift())
                        .tekst(kladd.getTekst())
                        .lagtInnAv(kladd.getLagtInnAv())
                        .build())
                .toList();
    }

    public void slettKladderGamlereEnnTimer(long timer) {
        LocalDateTime olderThanThis = LocalDateTime.now().minusHours(timer);
        kladdRepository.deleteGamlereEnn(olderThanThis);
    }

    public void slettKladd(Kladd kladd) {
        Long dialogId = Optional.ofNullable(kladd.getDialogId()).map(Long::parseLong).orElse(null);
        //henvendelser på eksisterende tråer har ikke aktivitetId
        kladdRepository.deleteKladd(
                kladd.getAktorId(),
                kladd.getLagtInnAv(),
                dialogId,
                kladd.getAktivitetId()
        );
    }
}
