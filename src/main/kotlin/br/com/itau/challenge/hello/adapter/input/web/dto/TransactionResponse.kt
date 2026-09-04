package br.com.itau.challenge.hello.adapter.input.web.dto

import br.com.itau.challenge.hello.adapter.input.web.enum.Currency
import br.com.itau.challenge.hello.adapter.input.web.enum.OperationType
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import java.time.Instant
import java.util.UUID

//{
//  "transaction": {
//    "id": "8e8ae808-b154-48b5-9f3e-553935cc4543",
//    "type": "CREDIT",
//    "amount": { "value": 97.07, "currency": "BRL" },
//    "status": "SUCCEEDED",
//    "timestamp": "2025-07-08T15:57:55-03:00"
//  },
//  "account": {
//    "id": "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975",
//    "balance": { "amount": 183.12, "currency": "BRL" }
//  }
//}
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