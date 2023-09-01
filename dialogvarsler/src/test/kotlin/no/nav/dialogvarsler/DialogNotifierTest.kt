package no.nav.dialogvarsler

import io.kotest.core.spec.style.StringSpec
import org.apache.kafka.clients.consumer.ConsumerRecord

class DialogNotifierTest : StringSpec({

    "should serialize message".config(enabled = false) {
        val fnr = "12345678910"
        val messageToSend = """{ "sistOppdatert": 1693510558103 }"""
        val record = ConsumerRecord("topic", 0,  1, fnr, messageToSend)
        DialogNotifier.notifySubscribers(record)
    }

})
