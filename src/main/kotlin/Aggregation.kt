package net.bloemsma.guus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import net.bloemsma.guus.CardStatus.ACTIVE
import net.bloemsma.guus.CardType.CREDIT_CARD
import net.bloemsma.guus.CardType.DEBIT_CARD
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val log = LoggerFactory.getLogger("aggregation")

suspend fun CoroutineScope.aggregateAll(poaClient: PowerOfAttorneyClient): List<PowerOfAttorney>? = poaClient
    .getAllPowerOfAttorneys()
    .mapDeferred(this) { aggregateOne(poaClient, it.id) }
    .waitSuccessful()

suspend fun CoroutineScope.aggregateOne(
    poaClient: PowerOfAttorneyClient,
    id: String,
): PowerOfAttorney {
    val poa = poaClient.getPowerOfAttorneyDetail(id)
    val accountDeferred = transformAsyncOrNull(this) {
        // Using the last 9 chars insulates from errors in the "NL23RABO" prefix.
        // It could be argued that it should be validated, but I chose otherwise.
        poaClient.getAccountDetail(poa.account.takeLast(9)) }
    val creditCardsDeferred = poa.cards
        ?.filter { it.type == CREDIT_CARD }
        ?.mapDeferred(this) { it: CardReference -> poaClient.getCreditCardDetail(it.id) }
    val debitCardsDeferred = poa.cards
        ?.filter { it.type == DEBIT_CARD }
        ?.mapDeferred(this) { it: CardReference -> poaClient.getDebitCardDetail(it.id) }
    return poa.copy(
        cards = null,
        creditCards = creditCardsDeferred?.waitSuccessful()?.filter { it.status == ACTIVE },
        debitCards = debitCardsDeferred?.waitSuccessful()?.filter { it.status == ACTIVE },
        // account is ended when the date is equal or after today
        accountDetails = accountDeferred.await()?.takeIf { it.ended?.let { LocalDate.now().isBefore(it) } ?: true }
    )
}

/** Apply transform in parallel and map failure to null.*/
fun <R, T> List<T>.mapDeferred(
    scope: CoroutineScope,
    transform: suspend (T) -> R?,
): List<Deferred<R?>> = map { t: T -> // start all requests, creating a list of Deferreds
    t.transformAsyncOrNull<R, T>(scope, transform)
}

/** Apply transform asynchronously and map failure to null.*/
fun <R, T> T.transformAsyncOrNull(
    scope: CoroutineScope,
    transform: suspend (T) -> R?,
): Deferred<R?> {
    return scope.async {
        try {
            log.trace("  -->: ${this@transformAsyncOrNull}")
            val transformed = transform(this@transformAsyncOrNull)
            log.trace("<--  : $transformed")
            transformed
        } catch (e: Exception) {
            log.warn("$e")
            null
        }
    }
}

/** Wait for completion and retain only successful results.*/
suspend fun <R> List<Deferred<R?>>.waitSuccessful(): List<R> = awaitAll().mapNotNull { it }


