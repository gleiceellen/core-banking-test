package br.com.itau.challenge.hello.port.input

import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionAuthorizationResponse
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionRequest
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionResponse

fun interface TransactionAuthorizationUseCase {
    fun createTransaction(transactionId: String, transactionRequest: TransactionRequest): TransactionAuthorizationResponse
}
