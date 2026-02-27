package no.nav.fo.veilarbdialog.kvp

import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.InternalServerErrorException
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.client.utils.graphql.GraphqlRequest
import no.nav.common.client.utils.graphql.GraphqlRequestBuilder
import no.nav.common.client.utils.graphql.GraphqlResponse
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.fo.veilarbdialog.clients.util.HttpClientWrapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Slf4j
@Service
@RequiredArgsConstructor
class KvpService(
    private val veilarboppfolgingClientWrapper: HttpClientWrapper
) {
    val log = LoggerFactory.getLogger(KvpService::class.java)

    private val path = "/graphql"
    fun kontorsperreEnhetId(fnr: Fnr): String? {
        try {
            val graphqlRequest: GraphqlRequest<*> = GraphqlRequestBuilder<KontorSperretEnhetQueryVariables>("graphql/veilarboppfolging/kontorSperreQuery.graphql")
                .buildRequest(KontorSperretEnhetQueryVariables(fnr = fnr.get()));
            val body: RequestBody = JsonUtils.toJson(graphqlRequest)
                .toRequestBody("application/json".toMediaType())

            val result = veilarboppfolgingClientWrapper
                .postAndReceive(path, body, KontorSperreGraphqlResult::class.java).get()
            if (result.errors != null && result.errors.isNotEmpty())    {
                val errormessage = "Kunne ikke hente kontorsperre fra veilarboppfolging: ${result.errors?.joinToString()}"
                log.error(errormessage)
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errormessage
                )
            }
            return result.data.brukerStatus?.kontorSperre?.kontorId
        } catch (e: ForbiddenException) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "veilarbdialog har ikke tilgang til å spørre om KVP-status."
            )
        } catch (e: InternalServerErrorException) {
            log.error("Kunnne ikke hente kontorsperre fra veilarboppfolging: ${e.message}", e)
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "veilarboppfolging har en intern bug, vennligst fiks applikasjonen."
            )
        }
    }
}

data class KontorSperretEnhetQueryVariables(val fnr: String)

data class KontorSperreDto(val kontorId: String? = null)
data class BrukerStatus(val kontorSperre: KontorSperreDto? = null)
data class KontorSperreQueryData(val brukerStatus: BrukerStatus? = null)
class KontorSperreGraphqlResult: GraphqlResponse<KontorSperreQueryData>()
