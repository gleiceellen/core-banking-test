package br.com.itau.challenge.hello.adapter.input.web.dto

import br.com.itau.challenge.hello.adapter.input.web.enum.OperationType
import java.util.*

data class TransactionRequest(
    val accountId: UUID,
    val type: OperationType,
    val amount: Amount
)