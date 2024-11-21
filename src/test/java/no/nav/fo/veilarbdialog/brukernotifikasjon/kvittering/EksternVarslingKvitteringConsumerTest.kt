package no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.json.JsonUtils
import no.nav.fo.veilarbdialog.brukernotifikasjon.kvittering.VarselHendelseUtil.eksternVarselHendelseSendt
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselHendelseConsumer
import no.nav.fo.veilarbdialog.minsidevarsler.MinsideVarselService
import no.nav.fo.veilarbdialog.minsidevarsler.dto.MinSideVarselId
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.TextNode
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
        val minsideVarselHendelseTopic = "topic"
        private const val APP_NAME = "veilarbdialog"
    }

    @BeforeEach
    fun setup() {
        eksternVarslingKvitteringConsumer = MinsideVarselHendelseConsumer(APP_NAME, minsideVarselService, kvitteringMetrikk)
    }

    @Test
    fun skalIgnorereMeldingerMedAnnenBestillerid() {
        val feilApp = "annen-app"
        val varselHendelse = eksternVarselHendelseSendt(MinSideVarselId(UUID.randomUUID()), feilApp)

        val consumerRecord = createConsumerRecord(varselHendelse)
        eksternVarslingKvitteringConsumer!!.consume(consumerRecord)
        Mockito.verifyNoInteractions(minsideVarselService, kvitteringMetrikk)
    }

        @Test
        fun `skal ikke interagere med minsideVarselService ved ukjent status`() {
            var varselId = MinSideVarselId(UUID.randomUUID())
            val hendelseMedUkjentStatus = eksternVarselHendelseSendt(varselId, APP_NAME)
                .let(JsonUtils::toJson)
                .let { ObjectMapper().readTree(it) as ObjectNode }
                .set("status", TextNode("lol"))
                .toString()
            val consumerRecord = ConsumerRecord<String, String>(
                minsideVarselHendelseTopic, 0, 1,
                varselId.value.toString(),
                hendelseMedUkjentStatus
            )
            // When
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                eksternVarslingKvitteringConsumer!!.consume(
                    consumerRecord
                )
            }
            // Then
            Mockito.verifyNoMoreInteractions(minsideVarselService);
        }

    private fun createConsumerRecord(varselHendelse: TestVarselHendelseDTO): ConsumerRecord<String, String> {
        return ConsumerRecord<String, String>(
            minsideVarselHendelseTopic,
            0,
            1,
            varselHendelse.varselId.toString(),
            JsonUtils.toJson(varselHendelse)
        )
    }
}