package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionRequest
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class TransactionController(
    private val transactionAuthorizationUseCase: TransactionAuthorizationUseCase,
) {

    @PostMapping("/transactions/{transactionId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun authorize(
        @PathVariable transactionId: String,
        @RequestBody transactionRequest: TransactionRequest,
    ): ResponseEntity<Any> {
        val result = transactionAuthorizationUseCase.createTransaction(transactionId, transactionRequest)
        return ResponseEntity.ok(result)
    }
}
