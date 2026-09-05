package br.com.itau.challenge.hello.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class AccountCreatedEvent(
    val account: AccountDto
) {
    data class AccountDto(
        val id: UUID,
        val owner: UUID,
        @JsonProperty("created_at") val createdAt: Long,
        val status: String
    )
}
