package no.nav.dialogvarsler

import io.kotest.core.spec.style.StringSpec
import no.nav.dialogvarsler.varsler.DialogNotifier

class DialogNotifierTest : StringSpec({

    "should serialize message" {
        val fnr = "12345678910"
        val messageToSend = """{ "eventType": "NY_MELDING", "fnr": "$fnr" }"""
        val record = messageToSend
        DialogNotifier.notifySubscribers(record)
    }

})
