package br.com.itau.challenge.hello.port.input

import java.util.UUID

fun interface InitializeAccountUseCase {
    fun initialize(accountId: UUID, owner: UUID, createdAtMicros: Long, status: String)
}
