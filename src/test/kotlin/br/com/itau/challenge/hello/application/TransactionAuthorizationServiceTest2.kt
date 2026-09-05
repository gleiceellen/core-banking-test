package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.domain.model.Amount
import br.com.itau.challenge.hello.domain.model.OperationType
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionResponse
import br.com.itau.challenge.hello.domain.model.TransactionStatus
import br.com.itau.challenge.hello.port.output.AccountRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionAuthorizationServiceTest2 {

    @Test
    fun `should delegate to repository and build response`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("97.07", "BRL"))
        val txResponse = TransactionResponse(UUID.fromString(txId), OperationType.CREDIT, Amount("97.07", "BRL"), TransactionStatus.SUCCEEDED)
        val repo = object : AccountRepository {
            override fun save(transactionId: String, request: TransactionRequest): Pair<TransactionResponse, Long> {
                assertEquals(txId, transactionId)
                assertEquals(req, request)
                return Pair(txResponse, 9707L)
            }
            override fun createIfAbsent(accountId: String, owner: String, createdAtMicros: Long) {}
        }
        val service = TransactionAuthorizationService(repo)
        val result = service.createTransaction(txId, req)
        assertEquals(txResponse, result.transaction)
        assertEquals(accountId, result.account.id)
        assertEquals(9707L, result.account.balance.amount)
    }
}
