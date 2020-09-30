package net.bloemsma.guus

import io.ktor.client.*
import io.ktor.client.request.*

// This was generated from apidef.yaml by https://start.ktor.io/

/**
 * Power of attorney Client
 * 
 * This is a sample server power of attorney service
 */
open class PowerOfAttorneyClient(val endpoint: String, val client: HttpClient = HttpClient()) {
    /**
     * Get all power of attorneys
     * 
     * Provides list of power of attorneys for current user
     * 
     * @return successful operation
     */
    suspend fun getAllPowerOfAttorneys(
    ): List<PowerOfAttorneyReference> {
        return client.get<List<PowerOfAttorneyReference>>("$endpoint/power-of-attorneys") {
        }
    }

    /**
     * Get Detail of Power of Attorney
     * 
     * Get Detail of given Power of Attorney
     * 
     * @param id null
     * 
     * @return successful operation
     */
    suspend fun getPowerOfAttorneyDetail(
        id: String // PATH
    ): PowerOfAttorney {
        return client.get<PowerOfAttorney>("$endpoint/power-of-attorneys/$id") {
        }
    }

    /**
     * Get Detail of debit card
     * 
     * Get Detail of given debit card
     * 
     * @param id null
     * 
     * @return successful operation
     */
    suspend fun getDebitCardDetail(
        id: String // PATH
    ): DebitCard {
        return client.get<DebitCard>("$endpoint/debit-cards/$id") {
        }
    }

    /**
     * Get Detail of credit card
     *
     * Get Detail of given credit card
     *
     * @param id null
     *
     * @return successful operation
     */
    suspend fun getCreditCardDetail(
        id: String // PATH
    ): CreditCard {
        return client.get<CreditCard>("$endpoint/credit-cards/$id") {
        }
    }

    /**
     * Get Detail of credit card
     *
     * Get Detail of given credit card
     *
     * @param id null
     *
     * @return successful operation
     */
    suspend fun getAccountDetail(
        id: String // PATH
    ): Account {
        return client.get<Account>("$endpoint/accounts/$id") {
        }
    }
}
