package br.com.itau.challenge.hello.adapter.output.dynamodb.mapper

import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionRequest
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import br.com.itau.challenge.hello.adapter.output.dynamodb.TransactionEntity

fun TransactionRequest.toEntity(pk: String, sk: String, transctionId: String): TransactionEntity {

    return TransactionEntity.Builder()
        .pk(pk)
        .sk(sk)
        .accountId(this.accountId.toString())
        .transactionId(transctionId)
        .type(this.type.toString())
        .amountValue(this.amount.value)
        .amountCurrency(this.amount.currency.toString())
        .status(TransactionStatus.SUCCEEDED.toString())
        .build()
}