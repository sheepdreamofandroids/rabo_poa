import io.kotest.assertions.json.matchJson
import io.kotest.matchers.should
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.withTestApplication
import net.bloemsma.guus.setup
import org.intellij.lang.annotations.Language
import org.junit.Test

class ComponentTest {
    private val jsonContentHeaders = headersOf("Content-Type", listOf(ContentType.Application.Json.toString()))
    private fun MockRequestHandleScope.respond(response: String) = respond(response, headers = jsonContentHeaders)
    private val mockEngine: HttpClientEngine = MockEngine { request ->
        when (val path = request.url.encodedPath) {
            "/power-of-attorneys" -> respond(poaList)
            "/power-of-attorneys/single" -> respond(poaSingle)
            "/power-of-attorneys/endedAccount" -> respond(poaEndedAccount)
            "/debit-cards/dddd" -> respond(poaDebitCard)
            "/credit-cards/cccc" -> respond(poaCreditCard)
            "/accounts/enAccount" -> respond(openAccount)
            "/accounts/edAccount" -> respond(endedAccount)
            else -> when {
                path.endsWith("error", true) -> respondError(HttpStatusCode.InternalServerError, "oops")
                path.endsWith("notFound", true) -> respondError(HttpStatusCode.NotFound, "oops")
                else -> error("Unhandled ${request.url.fragment}")
            }
        }

    }

    @Test
    fun `test single aggregation`() {
        withTestApplication(moduleFunction = { this.setup(mockEngine) }) {
            handleRequest(setup = {
                this.method = HttpMethod.Get
                uri = "/powers-of-attorney/single"
            }).apply {
                response.content should matchJson(singleAggregation)
            }
        }
    }

    @Test
    fun `test full aggregation`() {
        withTestApplication(moduleFunction = { this.setup(mockEngine) }) {
            handleRequest(setup = {
                this.method = HttpMethod.Get
                uri = "/powers-of-attorney"
            }).apply {
                response.content should matchJson(fullAggregation)
            }
        }
    }

//    @Test
//    fun `404 for undefined locations`() {
//        withTestApplication(moduleFunction = Application::configure) {
//            handleRequest(HttpMethod.Get, "/").apply {
//                requestHandled shouldBe true
//            }
//            handleRequest(HttpMethod.Get, "/slartibartfast").apply {
//                requestHandled shouldBe false
//            }
//        }
//    }
//
//    @Test
//    fun `export swagger json`() {
//        val path = Path.of("..", "generated", "api.json")
//        val currentContent = try {
//            Files.readString(path)
//        } catch (e: Exception) {
//            ""
//        }
//        withTestApplication(moduleFunction = Application::configure) {
//            handleRequest(HttpMethod.Get, "/openapi.json").apply {
//                requestHandled shouldBe true
//                val newContent = response.content
//                Files.writeString(path, newContent)
//                newContent shouldBe currentContent
//            }
//        }
//    }
}

@Language("JSON")
val poaList = """[ { "id": "giveserror" }, { "id": "isnotfound" }, { "id": "single" }, { "id": "endedAccount" } ]"""

@Language("JSON")
val poaSingle = """{
  "id": "single",
  "grantor": "Super duper company",
  "grantee": "Fellowship of the ring",
  "account": "openAccount",
  "direction": "GIVEN",
  "authorizations": [
    "DEBIT_CARD",
    "VIEW",
    "PAYMENT"
  ],
  "cards": [
    {
      "id": "dddd",
      "type": "DEBIT_CARD"
    },
    {
      "id": "error",
      "type": "DEBIT_CARD"
    },
    {
      "id": "notFound",
      "type": "DEBIT_CARD"
    },
    {
      "id": "cccc",
      "type": "CREDIT_CARD"
    },
    {
      "id": "error",
      "type": "CREDIT_CARD"
    },
    {
      "id": "notFound",
      "type": "CREDIT_CARD"
    }
  ]
}
"""

@Language("JSON")
val poaEndedAccount = """{
  "id": "endedAccount",
  "grantor": "endedAccount",
  "grantee": "endedAccount",
  "account": "endedAccount",
  "direction": "GIVEN",
  "authorizations": [ ],
  "cards": [
    {
      "id": "dddd",
      "type": "DEBIT_CARD"
    },
    {
      "id": "2222",
      "type": "DEBIT_CARD"
    },
    {
      "id": "3333",
      "type": "CREDIT_CARD"
    }
  ]
}
"""

@Language("JSON")
val poaDebitCard = """{
	"id": "dddd",
	"status": "ACTIVE",
	"cardNumber": 6527,
	"sequenceNumber": 1,
	"cardHolder": "Aragorn",
	"atmLimit": {
		"limit": 100,
		"periodUnit": "PER_DAY"
	},
	"posLimit": {
		"limit": 10000,
		"periodUnit": "PER_MONTH"
	},
	"contactless" : true
}"""

@Language("JSON")
val poaCreditCard = """{
	"id": "cccc",
	"status": "ACTIVE",
	"cardNumber": 5075,
	"sequenceNumber": 1,
	"cardHolder": "Boromir",
	"monthlyLimit": 3000
}"""

@Language("JSON")
val openAccount = """{
  "owner": "Geronima",
  "balance": 0.12,
  "created": "31-10-2003",
  "ended": "01-12-2111"
}"""

@Language("JSON")
val endedAccount = """{
  "owner": "Geronima",
  "balance": 0.12,
  "created": "31-10-0003",
  "ended": "01-12-2001"
}"""

@Language("JSON")
val fullAggregation =
    """{
  "powersOfAttorney": [
    {
      "id": "single",
      "grantor": "Super duper company",
      "grantee": "Fellowship of the ring",
      "account": "openAccount",
      "direction": "GIVEN",
      "authorizations": [
        "DEBIT_CARD",
        "VIEW",
        "PAYMENT"
      ],
      "creditCards": [
        {
          "id": "cccc",
          "cardNumber": 5075,
          "sequenceNumber": 1,
          "cardHolder": "Boromir",
          "monthlyLimit": 3000,
          "status": "ACTIVE"
        }
      ],
      "debitCards": [
        {
          "id": "dddd",
          "cardNumber": 6527,
          "sequenceNumber": 1,
          "cardHolder": "Aragorn",
          "atmLimit": {
            "limit": 100,
            "periodUnit": "PER_DAY"
          },
          "posLimit": {
            "limit": 10000,
            "periodUnit": "PER_MONTH"
          },
          "contactless": true,
          "status": "ACTIVE"
        }
      ],
      "accountDetails": {
        "owner": "Geronima",
        "balance": 0.12,
        "created": "31-10-2003",
        "ended": "01-12-2111"
      }
    },
    {
      "id": "endedAccount",
      "grantor": "endedAccount",
      "grantee": "endedAccount",
      "account": "endedAccount",
      "direction": "GIVEN",
      "authorizations": [],
      "creditCards": [],
      "debitCards": [
        {
          "id": "dddd",
          "cardNumber": 6527,
          "sequenceNumber": 1,
          "cardHolder": "Aragorn",
          "atmLimit": {
            "limit": 100,
            "periodUnit": "PER_DAY"
          },
          "posLimit": {
            "limit": 10000,
            "periodUnit": "PER_MONTH"
          },
          "contactless": true,
          "status": "ACTIVE"
        }
      ]
    }
  ]
}"""

@Language("JSON")
val singleAggregation="""{
  "id": "single",
  "grantor": "Super duper company",
  "grantee": "Fellowship of the ring",
  "account": "openAccount",
  "direction": "GIVEN",
  "authorizations": [
    "DEBIT_CARD",
    "VIEW",
    "PAYMENT"
  ],
  "creditCards": [
    {
      "id": "cccc",
      "cardNumber": 5075,
      "sequenceNumber": 1,
      "cardHolder": "Boromir",
      "monthlyLimit": 3000,
      "status": "ACTIVE"
    }
  ],
  "debitCards": [
    {
      "id": "dddd",
      "cardNumber": 6527,
      "sequenceNumber": 1,
      "cardHolder": "Aragorn",
      "atmLimit": {
        "limit": 100,
        "periodUnit": "PER_DAY"
      },
      "posLimit": {
        "limit": 10000,
        "periodUnit": "PER_MONTH"
      },
      "contactless": true,
      "status": "ACTIVE"
    }
  ],
  "accountDetails": {
    "owner": "Geronima",
    "balance": 0.12,
    "created": "31-10-2003",
    "ended": "01-12-2111"
  }
}"""