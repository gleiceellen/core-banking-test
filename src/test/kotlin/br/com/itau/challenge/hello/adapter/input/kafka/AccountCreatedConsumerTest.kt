package br.com.itau.challenge.hello.adapter.input.kafka

import br.com.itau.challenge.hello.port.input.InitializeAccountUseCase
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.UUID

class AccountCreatedConsumerTest {

    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `should deserialize and delegate to use case`() {
        val calls = mutableListOf<String>()
        val useCase = InitializeAccountUseCase { id, _, _, status -> calls.add("$id-$status") }
        val consumer = AccountCreatedConsumer(useCase, objectMapper)
        val id = UUID.randomUUID().toString()
        val owner = UUID.randomUUID().toString()
        val payload = """{"account":{"id":"$id","owner":"$owner","created_at":1634874339000000,"status":"ENABLED"}}"""
        consumer.consume(payload)
        assertEquals(1, calls.size)
        assertTrue(calls[0].startsWith(id))
    }

    @Test
    fun `should ignore invalid payload without throwing`() {
        val useCase = InitializeAccountUseCase { _, _, _, _ -> error("should not be called") }
        val consumer = AccountCreatedConsumer(useCase, objectMapper)
        consumer.consume("not a json")
        consumer.consume("""{"account":{"id":"bad-uuid","owner":"x"}}""")
        // should not throw
    }

    @Test
    fun `should handle BLOCKED status`() {
        var statusReceived: String? = null
        val useCase = InitializeAccountUseCase { _, _, _, status -> statusReceived = status }
        val consumer = AccountCreatedConsumer(useCase, objectMapper)
        val id = UUID.randomUUID().toString()
        val owner = UUID.randomUUID().toString()
        val payload = """{"account":{"id":"$id","owner":"$owner","created_at":123,"status":"BLOCKED"}}"""
        consumer.consume(payload)
        assertEquals("BLOCKED", statusReceived)
    }
}
