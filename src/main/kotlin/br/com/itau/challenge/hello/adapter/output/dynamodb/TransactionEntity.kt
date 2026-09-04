package br.com.itau.challenge.hello.adapter.output.dynamodb

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant


@DynamoDbImmutable(builder = TransactionEntity.Builder::class)
data class TransactionEntity(
    @get:DynamoDbPartitionKey
    val pk: String,

    @get:DynamoDbSortKey
    val sk: String,

    val accountId: String,
    val transactionId: String,
    val type: String,
    val amountValue: String,
    val amountCurrency: String,
    val status: String,
    val timestamp: String
) {
    class Builder {
        private var pk: String = ""
        private var sk: String = ""
        private var accountId: String = ""
        private var transactionId: String = ""
        private var type: String = ""
        private var amountValue: String = ""
        private var amountCurrency: String = ""
        private var status: String = ""
        private var timestamp: String = Instant.now().toString()

        fun pk(pk: String) = apply { this.pk = pk }
        fun sk(sk: String) = apply { this.sk = sk }
        fun accountId(accountId: String) = apply { this.accountId = accountId }
        fun transactionId(transactionId: String) = apply { this.transactionId = transactionId }
        fun type(type: String) = apply { this.type = type }
        fun amountValue(amountValue: String) = apply { this.amountValue = amountValue }
        fun amountCurrency(amountCurrency: String) = apply { this.amountCurrency = amountCurrency }
        fun status(status: String) = apply { this.status = status }

        fun build() = TransactionEntity(
            pk = pk,
            sk = sk,
            accountId = accountId,
            transactionId = transactionId,
            type = type,
            amountValue = amountValue,
            amountCurrency = amountCurrency,
            status = status,
            timestamp = timestamp
        )
    }
}