package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.port.output.AccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.UUID

class InitializeAccountServiceTest {

    @Test
    fun `should create account when status ENABLED`() {
        var called: Triple<String, String, Long>? = null
        val repo = object : AccountRepository {
            override fun save(transactionId: String, request: br.com.itau.challenge.hello.domain.model.TransactionRequest) = error("not needed")
            override fun createIfAbsent(accountId: String, owner: String, createdAtMicros: Long) {
                called = Triple(accountId, owner, createdAtMicros)
            }
        }
        val service = InitializeAccountService(repo)
        val id = UUID.randomUUID()
        val owner = UUID.randomUUID()
        service.initialize(id, owner, 1634874339000000L, "ENABLED")
        assertEquals(id.toString(), called?.first)
        assertEquals(owner.toString(), called?.second)
    }

    @Test
    fun `should ignore when status not ENABLED`() {
        var called = false
        val repo = object : AccountRepository {
            override fun save(transactionId: String, request: br.com.itau.challenge.hello.domain.model.TransactionRequest) = error("not needed")
            override fun createIfAbsent(accountId: String, owner: String, createdAtMicros: Long) { called = true }
        }
        val service = InitializeAccountService(repo)
        service.initialize(UUID.randomUUID(), UUID.randomUUID(), 123L, "BLOCKED")
        assertEquals(false, called)
        service.initialize(UUID.randomUUID(), UUID.randomUUID(), 123L, "DISABLED")
        assertEquals(false, called)
    }
}
