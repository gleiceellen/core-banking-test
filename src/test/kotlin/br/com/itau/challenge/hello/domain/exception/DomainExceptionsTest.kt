package br.com.itau.challenge.hello.domain.exception

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainExceptionsTest {

    @Test
    fun `should carry messages`() {
        assertEquals("amount must be greater than 0", InvalidAmountException("amount must be greater than 0").message)
        assertEquals("Transaction tx1 was already submitted with a different payload", IdempotencyConflictException("tx1").message)
        assertTrue(CurrencyMismatchException("acc1", "BRL", "USD").message!!.contains("BRL"))
        assertEquals("Account acc1 not found", AccountNotFoundException("acc1").message)
        assertEquals("Account acc1 not found.", AccountInexistentException("acc1").message)
        assertTrue(InsufficientBalanceException("acc1").message!!.contains("acc1"))
    }
}
