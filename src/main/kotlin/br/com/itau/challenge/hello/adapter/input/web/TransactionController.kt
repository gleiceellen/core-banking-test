package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TransactionController(
    private val transactionAuthorizationUseCase: TransactionAuthorizationUseCase,
    private val meterRegistry: MeterRegistry,
) {

    @PostMapping("/transactions/{transactionId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun authorize(
        @PathVariable transactionId: String,
        @RequestBody transactionRequest: TransactionRequest,
    ): ResponseEntity<Any> {
        val result = transactionAuthorizationUseCase.createTransaction(transactionId, transactionRequest)
        meterRegistry.counter("transactions_authorized", "status", result.transaction.status.name).increment()
        return ResponseEntity.ok(result)
    }
}
