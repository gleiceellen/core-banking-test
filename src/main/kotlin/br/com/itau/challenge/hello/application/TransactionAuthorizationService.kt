package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.adapter.input.web.dto.*
import br.com.itau.challenge.hello.adapter.input.web.enum.Currency
import br.com.itau.challenge.hello.adapter.output.dynamodb.AccountTransactionRepository
import br.com.itau.challenge.hello.adapter.output.dynamodb.mapper.toEntity
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import org.springframework.stereotype.Service

val ACCOUNT_PK = "ACCOUNT#"
val TRANSC_SK = "TRANS#"
@Service
class TransactionAuthorizationService(
    val accountTransactionRepository: AccountTransactionRepository
) : TransactionAuthorizationUseCase {

    override fun createTransaction(transactionId: String, transactionRequest: TransactionRequest): TransactionAuthorizationResponse {
        val pk = "$ACCOUNT_PK${transactionRequest.accountId}"
        val sk = "$TRANSC_SK${transactionId}"
        val transaction = transactionRequest.toEntity(pk, sk, transactionId)

        val transactionResult = accountTransactionRepository.save(transaction)
        val persistenceTransactionReponse = transactionResult.first
        val balanceReponse = transactionResult.second
        val account = AccountTransactionResponse(
            id = transactionRequest.accountId,
            balance = Balance(balanceReponse, Currency.BRL)
        )

    return TransactionAuthorizationResponse(
        persistenceTransactionReponse,
        account)
    }
}
