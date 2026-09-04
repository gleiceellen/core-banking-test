package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.adapter.input.web.dto.Amount
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionCategory
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionResponse
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionRequest
import br.com.itau.challenge.hello.adapter.input.web.enum.Currency
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class TransactionController(
    private val transactionAuthorizationUseCase: TransactionAuthorizationUseCase,
) {

    @PostMapping("/transaction/{transactionId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hello(@RequestParam transactionId: String,
              @RequestBody transactionRequest: TransactionRequest): TransactionResponse {
        val result = transactionAuthorizationUseCase.createTransaction(transactionId, transactionRequest)
        return TransactionResponse(
            UUID.randomUUID(),
            TransactionCategory.CREDIT,
            Amount("", Currency.BRL.toString()),
            TransactionStatus.SUCCEEDED)
    }
}
