package com.example

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.clients.consumer.ConsumerRecord

class DialogNotifierTest : StringSpec({

    "should serialize message" {
        val fnr = "12345678910"
        val messageToSend = """{ "sistOppdatert": 1693510558103 }"""
        val record = ConsumerRecord("topic", 0,  1, fnr, messageToSend)
        DialogNotifier.notifySubscribers(record)
    }

})
