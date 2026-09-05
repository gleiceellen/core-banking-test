package br.com.itau.challenge.hello.adapter.output.dynamodb

import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionEntityTest {

    @Test
    fun `should build entity`() {
        val entity = TransactionEntity.Builder()
            .pk("ACCOUNT#1")
            .sk("TRANS#1")
            .accountId("acc1")
            .transactionId("tx1")
            .type("CREDIT")
            .amountValue("10.00")
            .amountCurrency("BRL")
            .status("SUCCEEDED")
            .build()
        assertEquals("ACCOUNT#1", entity.pk)
        assertEquals("TRANS#1", entity.sk)
        assertEquals("acc1", entity.accountId)
    }
}
