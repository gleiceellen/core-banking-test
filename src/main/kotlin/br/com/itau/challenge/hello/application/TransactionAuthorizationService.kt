package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.domain.model.AccountTransactionResponse
import br.com.itau.challenge.hello.domain.model.Balance
import br.com.itau.challenge.hello.domain.model.Currency
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationResponse
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import br.com.itau.challenge.hello.port.output.AccountRepository
import org.springframework.stereotype.Service

@Service
class TransactionAuthorizationService(
    private val accountRepository: AccountRepository,
) : TransactionAuthorizationUseCase {

    override fun createTransaction(transactionId: String, transactionRequest: TransactionRequest): TransactionAuthorizationResponse {
        val (transaction, balance) = accountRepository.save(transactionId, transactionRequest)
        val account = AccountTransactionResponse(
            id = transactionRequest.accountId,
            balance = Balance(balance, Currency.BRL)
        )
        return TransactionAuthorizationResponse(transaction, account)
    }
}
