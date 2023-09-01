package no.nav.dialogvarsler

import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.dialogvarsler.plugins.*

//fun main() {
//    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
//        .start(wait = true)
//}
fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureAuthentication()
    configureMonitoring()
    configureSerialization()
    configureSockets()
    configureRouting()
    configureKafka()
}