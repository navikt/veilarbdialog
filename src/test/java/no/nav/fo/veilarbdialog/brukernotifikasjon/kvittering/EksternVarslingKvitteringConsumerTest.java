package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering;

import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Tester noen caser som er vrien å teste med integrasjonstester (typisk async feilhåndtering)
 */
@ExtendWith(MockitoExtension.class)
class EksternVarslingKvitteringConsumerTest {

    private static final String APP_NAME = "veilarbdialog";

    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @Mock
    private KvitteringDAO kvitteringDAO;
    @Mock
    private BrukernotifikasjonRepository brukernotifikasjonRepository;

    @Mock
    private KvitteringMetrikk kvitteringMetrikk;

    @BeforeEach
    void setup() {
        eksternVarslingKvitteringConsumer = new EksternVarslingKvitteringConsumer(kvitteringDAO, brukernotifikasjonRepository, kvitteringMetrikk, APP_NAME);
    }

    @Test
    void skalIgnorereMeldingerMedAnnenBestillerid() {
        String feilApp = "annen-app";
        String bestillingsId = UUID.randomUUID().toString();
        DoknotifikasjonStatus doknotifikasjonStatus = DoknotifikasjonStatus
                .newBuilder()
                .setStatus("INFO")
                .setBestillingsId(bestillingsId)
                .setBestillerId(feilApp)
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();

        ConsumerRecord consumerRecord = createConsumerRecord(doknotifikasjonStatus);
        eksternVarslingKvitteringConsumer.consume(consumerRecord);
        verifyNoInteractions(
                kvitteringDAO, brukernotifikasjonRepository, kvitteringMetrikk);
    }

    @Test
    void skalFeileVedUkjentStatus() {
        String bestillingsId = UUID.randomUUID().toString();
        DoknotifikasjonStatus doknotifikasjonStatus = createDoknotifikasjonStatus(bestillingsId, "RUBBISH");

        when(brukernotifikasjonRepository.finnesBrukernotifikasjon(bestillingsId)).thenReturn(true);

        ConsumerRecord consumerRecord = createConsumerRecord(doknotifikasjonStatus);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> eksternVarslingKvitteringConsumer.consume(consumerRecord));

        verify(kvitteringDAO).lagreKvittering(bestillingsId, doknotifikasjonStatus);

        verify(brukernotifikasjonRepository).finnesBrukernotifikasjon(bestillingsId);
        verifyNoMoreInteractions(brukernotifikasjonRepository);
    }

    @Test
    void skalFeileVedUkjentBestillingsId() {
        String bestillingsId = UUID.randomUUID().toString();
        DoknotifikasjonStatus doknotifikasjonStatus = createDoknotifikasjonStatus(bestillingsId, "OVERSENDT");

        when(brukernotifikasjonRepository.finnesBrukernotifikasjon(bestillingsId)).thenReturn(false);


        ConsumerRecord consumerRecord = createConsumerRecord(doknotifikasjonStatus);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> eksternVarslingKvitteringConsumer.consume(consumerRecord));
        verify(brukernotifikasjonRepository).finnesBrukernotifikasjon(bestillingsId);
        verifyNoMoreInteractions(brukernotifikasjonRepository);
    }

    private ConsumerRecord createConsumerRecord(DoknotifikasjonStatus doknotifikasjonStatus) {
        return new ConsumerRecord("kvitteringsTopic", 0, 1, doknotifikasjonStatus.getBestillingsId(), doknotifikasjonStatus);
    }

    private DoknotifikasjonStatus createDoknotifikasjonStatus(String bestillingsId, String status) {
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId(APP_NAME)
                .setMelding("her er en melding")
                .setDistribusjonId(1L)
                .build();
    }
}