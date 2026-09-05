package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.domain.model.Amount
import br.com.itau.challenge.hello.domain.model.Currency
import br.com.itau.challenge.hello.domain.model.OperationType
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionResponse
import br.com.itau.challenge.hello.domain.model.TransactionStatus
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationUseCase
import br.com.itau.challenge.hello.port.input.TransactionAuthorizationResponse
import br.com.itau.challenge.hello.domain.model.AccountTransactionResponse
import br.com.itau.challenge.hello.domain.model.Balance
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest2 {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @MockitoBean lateinit var useCase: TransactionAuthorizationUseCase

    @Test
    fun `should return 200 with transaction response`() {
        val txId = UUID.randomUUID().toString()
        val accountId = UUID.randomUUID()
        val txResponse = TransactionResponse(UUID.fromString(txId), OperationType.CREDIT, Amount("97.07", "BRL"), TransactionStatus.SUCCEEDED)
        val accResponse = AccountTransactionResponse(accountId, Balance(9707L, Currency.BRL))
        `when`(useCase.createTransaction(any(), any())).thenReturn(TransactionAuthorizationResponse(txResponse, accResponse))

        val body = mapOf("account_id" to accountId.toString(), "type" to "CREDIT", "amount" to mapOf("value" to "97.07", "currency" to "BRL"))
        mockMvc.perform(post("/transactions/$txId").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transaction.id").value(txId))
            .andExpect(jsonPath("$.account.id").value(accountId.toString()))
    }
}
