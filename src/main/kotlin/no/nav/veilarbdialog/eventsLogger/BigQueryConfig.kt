package no.nav.veilarbdialog.eventsLogger

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
open class BigQueryConfig(@Value("\${application.gcp.projectId}") val projectId: String) {

    @Bean
//    @Profile("!local")
    open fun bigQueryClient(): BigQueryClient {
        return BigQueryClientImplementation(projectId)
    }
}