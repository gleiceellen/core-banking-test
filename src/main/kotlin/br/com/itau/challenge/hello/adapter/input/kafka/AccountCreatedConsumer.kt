package br.com.itau.challenge.hello.adapter.input.kafka

import br.com.itau.challenge.hello.adapter.input.kafka.dto.AccountCreatedEvent
import br.com.itau.challenge.hello.port.input.InitializeAccountUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class AccountCreatedConsumer(
    private val initializeAccountUseCase: InitializeAccountUseCase,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(AccountCreatedConsumer::class.java)

    @KafkaListener(topics = ["\${account-created.topic-name}"])
    fun consume(payload: String) {
        try {
            val event = objectMapper.readValue(payload, AccountCreatedEvent::class.java)
            initializeAccountUseCase.initialize(
                event.account.id,
                event.account.owner,
                event.account.createdAt,
                event.account.status
            )
        } catch (e: Exception) {
            logger.error("Falha ao processar evento Kafka payload={}", payload, e)
        }
    }
}
