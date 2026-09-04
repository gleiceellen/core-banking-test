package br.com.itau.challenge.hello.adapter.input.web.dto

import java.util.UUID

class AccountResponse(
    val id: UUID,
    val balance: Amount
)