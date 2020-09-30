package net.bloemsma.guus

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.model.operation.OperationModel
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.RouteOpenAPIModule
import com.papsign.ktor.openapigen.modules.openapi.OperationModule
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.extensions.HashAlgorithm.SHA256
import io.ktor.network.tls.extensions.SignatureAlgorithm.RSA
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import java.text.SimpleDateFormat

// This was generated from apidef.yaml by https://start.ktor.io/
// and heavily modified.

fun main() {
    // ad-hoc selfsigned cert
    val keystore = buildKeyStore {
        certificate("localhost") {
            daysValid = 100
            hash = SHA256
            keySizeInBits = 512
            password = "changeIt"
            sign = RSA
        }
    }

    val env = applicationEngineEnvironment {
        // app configuration
        module(Application::module)

        // insecure API
        connector {
            host = "0.0.0.0"
            port = 8081
        }

        // "secure" API
        sslConnector(
            keyStore = keystore,
            keyAlias = "localhost",
            keyStorePassword = { "changeIt".toCharArray() },
            privateKeyPassword = { "changeIt".toCharArray() }) {
            host = "0.0.0.0"
            port = 8443
        }
    }

    embeddedServer(Netty, env).start(true)
}

fun Application.module() {
    setup(Apache.create {})
}

fun Application.setup(clientEngine: HttpClientEngine) {
    val client = HttpClient(clientEngine) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
//                dateFormat = SimpleDateFormat("dd-MM-yyyy")
            }
            accept(ContentType.Application.Json)
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            registerModule(JavaTimeModule())
        }
    }

    install(StatusPages) {
        exception<ClientRequestException> { cause ->
            if (cause.response.status == HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.NotFound)
            else
                call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(OpenAPIGen) {
        info {
            title = "Power of Attorney"
            description = "Aggregates all PoA info."
            contact {
                name = "Rabobank"
                url = "http://rabobank.nl"
                email = "info@rabobank.nl"
            }
        }
        server("http://localhost:8081/")
    }

    routing {
        // enable swagger UI
        get("/") {
            call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
        }
        get("/openapi.json") {
            call.respondTextWriter(contentType = ContentType.Application.Json) {
                jacksonObjectMapper().writeValue(this, openAPIGen.api.serialize())
            }
        }
    }

    val poaClient = PowerOfAttorneyClient("http://localhost:8080", client = client)

    apiRouting {
        route("powers-of-attorney") {
            get<Unit, AggregationResponse>(op {
                this.description = """Recursively finds all
                        | power of attorney records,
                        | including all cards. When errors occur,
                        | parts of the structure might be missing
                        | rather than failing completely."""
                    .trimMargin()
                summary = "Find all powers of attorney."
                operationId = "allPoA"
            }) {
                respond(AggregationResponse(aggregateAll(poaClient)))
            }

            get<PoAId, PowerOfAttorney>(op {
                summary = "Get one power of attorney."
                operationId = "onePoA"
            }) { poAId: PoAId -> respond(aggregateOne(poaClient, poAId.id)) }
        }
    }

}

// convenience, this should be in OpenAPI module.
fun op(config: OperationModel.() -> Unit): RouteOpenAPIModule = object : OperationModule, RouteOpenAPIModule {
    override fun configure(apiGen: OpenAPIGen, provider: ModuleProvider<*>, operation: OperationModel) {
        operation.config()
    }
}

/** Wrap the aggregation response in an object.*/
data class AggregationResponse(val powersOfAttorney: List<PowerOfAttorney>?)

@Path("{id}")
data class PoAId(@PathParam("Power of attorney ID") val id: String)

