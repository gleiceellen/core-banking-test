package br.com.itau.challenge.hello.adapter.input.web.dto

import br.com.itau.challenge.hello.adapter.input.web.enum.Currency
import br.com.itau.challenge.hello.adapter.input.web.enum.OperationType
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import java.time.Instant
import java.util.*

class TransactionAuthorizationResponse (
    val transaction: TransactionResponse,
    val account: AccountTransactionResponse
)
