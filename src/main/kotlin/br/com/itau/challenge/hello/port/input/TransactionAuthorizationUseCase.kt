package br.com.itau.challenge.hello.port.input

import br.com.itau.challenge.hello.domain.model.AccountTransactionResponse
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionResponse

data class TransactionAuthorizationResponse(
    val transaction: TransactionResponse,
    val account: AccountTransactionResponse
)

fun interface TransactionAuthorizationUseCase {
    fun createTransaction(transactionId: String, transactionRequest: TransactionRequest): TransactionAuthorizationResponse
}
