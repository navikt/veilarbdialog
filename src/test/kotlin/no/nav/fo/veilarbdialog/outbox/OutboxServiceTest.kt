package no.nav.fo.veilarbdialog.outbox

import no.nav.fo.veilarbdialog.SpringBootTestBase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.support.TransactionTemplate

class OutboxServiceTest : SpringBootTestBase() {

    @Autowired
    lateinit var outboxDao: OutboxDao

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Value("\${application.topic.ut.endringPaaDialog}")
    lateinit var endringPaaDialogTopic: String

    @Test
    fun `lagreIOutbox kaster exception naar det ikke finnes en aktiv transaksjon`() {
        assertThatThrownBy {
            outboxService.lagreIOutbox(endringPaaDialogTopic, "aktorId-789", """{"aktorId":"aktorId-789"}""")
        }.isInstanceOf(IllegalTransactionStateException::class.java)

        assertThat(outboxDao.hentUsendteMeldinger()).isEmpty()
    }

    @Test
    fun `ingenting lagres i outbox naar transaksjonen rulles tilbake`() {
        val antallFoer = outboxDao.hentUsendteMeldinger().size

        runCatching {
            transactionTemplate.execute {
                outboxDao.lagre(endringPaaDialogTopic, "aktorId-123", """{"aktorId":"aktorId-123"}""")
                throw RuntimeException("Noe gikk galt")
            }
        }

        val meldinger = outboxDao.hentUsendteMeldinger()
        assertThat(meldinger).hasSize(antallFoer)
    }

    @Test
    fun `outbox ryddes opp etter at meldinger er sendt`() {
        val consumer = kafkaTestService.createStringStringConsumer(endringPaaDialogTopic)

        outboxDao.lagre(endringPaaDialogTopic, "aktorId-456", """{"aktorId":"aktorId-456"}""")
        assertThat(outboxDao.hentUsendteMeldinger()).isNotEmpty()

        outboxService.sendUsendteMeldinger()

        assertThat(outboxDao.hentUsendteMeldinger()).isEmpty()
        kafkaTestService.assertHasNewRecord(endringPaaDialogTopic, consumer)
    }
}
