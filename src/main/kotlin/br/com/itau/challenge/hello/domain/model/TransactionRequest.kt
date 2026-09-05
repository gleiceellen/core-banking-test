package br.com.itau.challenge.hello.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class TransactionRequest(
    @JsonProperty("account_id") val accountId: UUID,
    val type: OperationType,
    val amount: Amount
)
