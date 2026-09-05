package br.com.itau.challenge.hello.port.output

import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionResponse

interface AccountRepository {
    fun save(transactionId: String, request: TransactionRequest): Pair<TransactionResponse, Long>
    fun createIfAbsent(accountId: String, owner: String, createdAtMicros: Long)
}
