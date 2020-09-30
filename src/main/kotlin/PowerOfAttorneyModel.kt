package net.bloemsma.guus

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import java.math.BigDecimal
import java.time.LocalDate

// This was generated from apidef.yaml by https://start.ktor.io/
// and slightly changed

data class PowerOfAttorneyReference(
    val id: String,
)

@JsonInclude(NON_NULL)
data class PowerOfAttorney(
    val id: String,
    val grantor: String,
    val grantee: String,
    val account: String,
    val direction: Direction,
    val authorizations: List<String>,
    val cards: List<CardReference>?,
    val creditCards: List<CreditCard>?,
    val debitCards: List<DebitCard>?,
    val accountDetails: Account?,
)

enum class Direction { GIVEN, RECEIVED }

data class CardReference(
    val id: String,
    val type: CardType,
)

enum class CardType { DEBIT_CARD, CREDIT_CARD }

data class DebitCard(
    val id: String,
    val cardNumber: Int,
    val sequenceNumber: Int,
    val cardHolder: String,
    val atmLimit: Limit,
    val posLimit: Limit,
    val contactless: Boolean,
    val status: CardStatus,
)

enum class CardStatus { ACTIVE, BLOCKED }

data class Limit(
    val limit: Int,
    val periodUnit: PeriodUnit,
)

enum class PeriodUnit { PER_DAY, PER_WEEK, PER_MONTH }

data class CreditCard(
    val id: String,
    val cardNumber: Int,
    val sequenceNumber: Int,
    val cardHolder: String,
    val monthlyLimit: Int,
    val status: CardStatus,
)

data class Account(
    val owner: String,
    // For most other amounts an Int makes sense, but a balance can definitely have cents.
    // My preference would be to handle amounts as a long expressed in cents, the amounts in the
    // sample files suggest that they are full amounts and can therefore potentially have fractions.
    // Kotlin has an alpha feature called inline classes that allows to use an unwrapped long for storage
    // but define custom operators to avoid mistakes with mixing amounts and other number.
    val balance: BigDecimal,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val created: LocalDate,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val ended: LocalDate?,
)