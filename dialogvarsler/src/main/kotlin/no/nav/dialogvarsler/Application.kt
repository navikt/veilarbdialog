package no.nav.dialogvarsler

import io.ktor.server.application.*
import io.ktor.server.netty.*
import no.nav.dialogvarsler.plugins.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
    val logger = LoggerFactory.getLogger(Application::class.java)
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        logger.error("Uncaught exception i thread: ${thread.name}", exception)
    }
}

fun Application.module() {
    configureAuthentication()
    configureMonitoring()
    configureMicrometer()
    configureSerialization()
    configureSockets()
    configureRouting()
    configureRedis()
}
