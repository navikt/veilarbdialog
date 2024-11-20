package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.brukernotifikasjon.BrukernotifikasjonRepository
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselHendelseConsumer
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternStatusOppdatertEventName
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselHendelseDTO
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselKanal
import no.nav.fo.veilarbdialog.minsidevarsler.dto.EksternVarselStatus
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinsideVarselDao
import no.nav.tms.varsel.action.Varseltype
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.function.Executable
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.IllegalArgumentException
import java.util.UUID

/**
 * Tester noen caser som er vrien å teste med integrasjonstester (typisk async feilhåndtering)
 */
@ExtendWith(MockitoExtension::class)
open class EksternVarslingKvitteringConsumerTest(
    @Mock
    private val minsideVarselService: MinsideVarselService,
    @Mock
    private val kvitteringMetrikk: KvitteringMetrikk,
) {
    var eksternVarslingKvitteringConsumer: MinsideVarselHendelseConsumer? = null

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupAll() {
            // To make jackson json annotations work ("@eventName" instead of "eventName")
            JsonUtils.getMapper().registerKotlinModule()
        }
        private const val APP_NAME = "veilarbdialog"
    }

    @BeforeEach
    fun setup() {
        eksternVarslingKvitteringConsumer = MinsideVarselHendelseConsumer(APP_NAME, minsideVarselService, kvitteringMetrikk)
    }

    @Test
    fun skalIgnorereMeldingerMedAnnenBestillerid() {
        val feilApp = "annen-app"
        val varselHendelse = EksternVarselHendelseDTO(
             EksternStatusOppdatertEventName,
            "dab",
            feilApp,
            Varseltype.Beskjed,
            UUID.randomUUID(),
            EksternVarselStatus.sendt,
            false,
            null,
            EksternVarselKanal.SMS
        )

        val consumerRecord = createConsumerRecord(varselHendelse)
        eksternVarslingKvitteringConsumer!!.consume(consumerRecord)
        Mockito.verifyNoInteractions(minsideVarselService, kvitteringMetrikk)
    }

    //    @Test
    //    void skalFeileVedUkjentStatus() {
    //        var varselId = new MinSideVarselId(UUID.randomUUID());
    //        EksternVarselHendelseDTO doknotifikasjonStatus = createDoknotifikasjonStatus(varselId, "RUBBISH");
    //
    //        when(brukernotifikasjonRepository.finnesBrukernotifikasjon(varselId)).thenReturn(true);
    //
    //        var consumerRecord = createConsumerRecord(doknotifikasjonStatus);
    //        Assertions.assertThrows(IllegalArgumentException.class,
    //                () -> eksternVarslingKvitteringConsumer.consume(consumerRecord));
    //
    //        verify(kvitteringDAO).lagreKvittering(varselId, doknotifikasjonStatus);
    //
    //        verify(brukernotifikasjonRepository).finnesBrukernotifikasjon(varselId);
    //        verifyNoMoreInteractions(brukernotifikasjonRepository);
    //    }

    private fun createConsumerRecord(varselHendelse: EksternVarselHendelseDTO): ConsumerRecord<String, String> {
        return ConsumerRecord<String, String>(
            "kvitteringsTopic",
            0,
            1,
            varselHendelse.varselId.toString(),
            JsonUtils.toJson(varselHendelse)
        )
    }

    private fun createDoknotifikasjonStatus(
        varselId: MinSideVarselId,
        status: EksternVarselStatus
    ): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            EksternStatusOppdatertEventName,
            "dab",
            APP_NAME,
            Varseltype.Beskjed,
            varselId.value,
            status,
            false,
            null,
            EksternVarselKanal.SMS
        )
    }

}