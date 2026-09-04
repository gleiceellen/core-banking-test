package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.adapter.input.web.dto.Amount
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionRequest
import br.com.itau.challenge.hello.adapter.input.web.enum.Currency
import br.com.itau.challenge.hello.adapter.input.web.enum.OperationType
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class TransactionAuthorizationServiceTest(
 val transactionAuthorizationService: TransactionAuthorizationService
) {

  @Test
  fun `shouldAuthorizeTransactionWhenOperationIsCreditAndAccountExists`(){
   val transactionRequest = createSuccessfulTransactionRequest()
   val transactionId = "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975"

   val result = transactionAuthorizationService.createTransaction(transactionId, transactionRequest)

   assertEquals(TransactionStatus.SUCCEEDED, result.status)
  }

 private fun createSuccessfulTransactionRequest(): TransactionRequest {
  return TransactionRequest(
   UUID.randomUUID(),
   OperationType.CREDIT,
   Amount("97.07", Currency.BRL)
  )
 }
 }