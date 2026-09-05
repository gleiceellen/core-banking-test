package br.com.itau.challenge.hello.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import java.util.UUID

class DomainModelsTest {

    @Test
    fun `should create amount and transaction request`() {
        val amount = Amount("97.07", "BRL")
        assertEquals("97.07", amount.value)
        assertEquals("BRL", amount.currency)
        val req = TransactionRequest(UUID.randomUUID(), OperationType.CREDIT, amount)
        assertEquals(amount, req.amount)
    }

    @Test
    fun `should create transaction response`() {
        val amount = Amount("97.07", "BRL")
        val tr = TransactionResponse(UUID.randomUUID(), OperationType.CREDIT, amount, TransactionStatus.SUCCEEDED)
        assertEquals(TransactionStatus.SUCCEEDED, tr.status)
        assertEquals(OperationType.CREDIT, tr.type)
    }

    @Test
    fun `should enum values`() {
        assertEquals(2, OperationType.values().size)
        assertEquals(1, Currency.values().size)
        assertEquals(2, TransactionStatus.values().size)
    }

    @Test
    fun `should balance and account response`() {
        val bal = Balance(1000L, Currency.BRL)
        assertEquals(1000L, bal.amount)
        val acc = AccountTransactionResponse(UUID.randomUUID(), bal)
        assertEquals(bal, acc.balance)
    }
}
