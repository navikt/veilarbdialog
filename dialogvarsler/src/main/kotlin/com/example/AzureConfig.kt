import java.util.Properties

object AzureConfig {
    fun get(
        acceptedAudience: String = System.getenv("AZURE_APP_CLIENT_ID"),
        wellKnownUrl: String = System.getenv("AZURE_APP_WELL_KNOWN_URL"),
    ): Properties {
        return Properties()
            .also {
                it["no.nav.security.jwt.issuers.0.issuer_name"] = "AzureAD"
                it["no.nav.security.jwt.issuers.0.discoveryurl"] = wellKnownUrl
                it["no.nav.security.jwt.issuers.0.accepted_audience"] = acceptedAudience
            }

    }

}