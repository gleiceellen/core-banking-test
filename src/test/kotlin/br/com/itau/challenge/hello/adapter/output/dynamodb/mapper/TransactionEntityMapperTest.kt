package br.com.itau.challenge.hello.adapter.output.dynamodb.mapper

import br.com.itau.challenge.hello.domain.model.Amount
import br.com.itau.challenge.hello.domain.model.OperationType
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionEntityMapperTest {

    @Test
    fun `should map request to entity`() {
        val req = TransactionRequest(UUID.randomUUID(), OperationType.CREDIT, Amount("97.07", "BRL"))
        val entity = req.toEntity("ACCOUNT#1", "TRANS#1", "tx1")
        assertEquals("ACCOUNT#1", entity.pk)
        assertEquals("TRANS#1", entity.sk)
        assertEquals("CREDIT", entity.type)
        assertEquals("97.07", entity.amountValue)
        assertEquals("BRL", entity.amountCurrency)
    }
}
