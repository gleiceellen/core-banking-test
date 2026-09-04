package br.com.itau.challenge.hello.adapter.output.dynamodb

import br.com.itau.challenge.hello.adapter.input.web.dto.Amount
import br.com.itau.challenge.hello.adapter.input.web.dto.TransactionResponse
import br.com.itau.challenge.hello.adapter.input.web.enum.OperationType
import br.com.itau.challenge.hello.adapter.input.web.enum.TransactionStatus
import br.com.itau.challenge.hello.domain.exception.AccountInexistentException
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.util.*

class AccountTransactionRepository(
    private val dynamoDbClient: DynamoDbClient,
) {
    val tableName = "account-transactions"

    private val logger = LoggerFactory.getLogger(AccountTransactionRepository::class.java)

    fun save(transaction: TransactionEntity): Pair<TransactionResponse, Long> {
        val sk_account = "PROFILE#METADATA"
        val amountInCents = transaction.amountValue.toBigDecimal()
            .multiply(java.math.BigDecimal(100))
            .toLong()

        val signedAmount = if (transaction.type == "CREDIT") amountInCents else -amountInCents
        val conditionExpression = if (transaction.type == "CREDIT") {
            "attribute_exists(balance)"
        } else {
            "attribute_exists(balance) AND balance + :amount > 0"
        }

        val updateRequest = Update.builder()
            .tableName(tableName)
            .key(
                mapOf(
                    "pk" to AttributeValue.fromS(transaction.pk),
                    "sk" to AttributeValue.fromS(sk_account)
                )
            )
            .updateExpression("ADD balance :amount")
            .expressionAttributeValues(
                mapOf(
                    ":amount" to AttributeValue.fromN(signedAmount.toString())
                )
            )
            .conditionExpression(conditionExpression)
            .build()

        val updateTransactItem = TransactWriteItem.builder()
            .update(updateRequest)
            .build()

        val itemMap = mapOf(
            "pk" to AttributeValue.fromS(transaction.pk),
            "sk" to AttributeValue.fromS(transaction.sk),
            "accountId" to AttributeValue.fromS(transaction.accountId),
            "transactionId" to AttributeValue.fromS(transaction.transactionId),
            "type" to AttributeValue.fromS(transaction.type),
            "amountValue" to AttributeValue.fromS(transaction.amountValue),
            "amountCurrency" to AttributeValue.fromS(transaction.amountCurrency),
            "status" to AttributeValue.fromS(transaction.status),
            "timestamp" to AttributeValue.fromS(transaction.timestamp)
        )

        val putRequest = Put.builder()
            .tableName(tableName)
            .item(itemMap)
            .build()

        val putTransactItem = TransactWriteItem.builder()
            .put(putRequest)
            .build()

        val transactRequest = TransactWriteItemsRequest.builder()
            .transactItems(updateTransactItem, putTransactItem)
            .build()

        fun getCurrentBalance(): Long {
            val getRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(
                    mapOf(
                        "pk" to AttributeValue.fromS(transaction.pk),
                        "sk" to AttributeValue.fromS(sk_account)
                    )
                )
                .consistentRead(true)
                .build()

            val response = dynamoDbClient.getItem(getRequest)
            val balanceAttr = response.item()?.get("balance")
                ?: throw AccountInexistentException(transaction.accountId)
            return balanceAttr.n().toLong()
        }

        return try {
            dynamoDbClient.transactWriteItems(transactRequest)

            val newBalance = getCurrentBalance()

            logger.info(
                "Transação {} executada com sucesso para conta {}. Novo saldo: {}",
                transaction.transactionId,
                transaction.accountId,
                newBalance
            )

            Pair(
                TransactionResponse(
                    id = UUID.fromString(transaction.transactionId),
                    type = OperationType.valueOf(transaction.type),
                    amount = Amount(transaction.amountValue, transaction.amountCurrency),
                    status = TransactionStatus.SUCCEEDED
                ),
                newBalance
            )
        } catch (e: ConditionalCheckFailedException) {
            val currentBalance = try {
                getCurrentBalance()
            } catch (ex: AccountInexistentException) {
                logger.error(
                    "Conta {} não encontrada ao tentar recuperar saldo após falha na transação {}",
                    transaction.accountId,
                    transaction.transactionId,
                    ex
                )
                throw ex
            }

            logger.warn(
                "Transação {} não criada para conta {}. Motivo: condição não atendida (saldo insuficiente ou conta inexistente). Saldo atual: {}",
                transaction.transactionId,
                transaction.accountId,
                currentBalance
            )

            Pair(
                TransactionResponse(
                    id = UUID.fromString(transaction.transactionId),
                    type = OperationType.valueOf(transaction.type),
                    amount = Amount(transaction.amountValue, transaction.amountCurrency),
                    status = TransactionStatus.FAILED
                ),
                currentBalance
            )
        }
    }
}