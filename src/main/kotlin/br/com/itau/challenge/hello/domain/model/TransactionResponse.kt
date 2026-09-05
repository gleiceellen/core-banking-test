package br.com.itau.challenge.hello.domain.model

import java.time.Instant
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val type: OperationType,
    val amount: Amount,
    val status: TransactionStatus,
    val timestamp: String = Instant.now().toString()
)

data class AccountTransactionResponse(
    val id: UUID,
    val balance: Balance
)

data class Balance(
    val amount: Long,
    val currency: Currency
)
