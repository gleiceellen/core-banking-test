package br.com.itau.challenge.hello.adapter.output.dynamodb

import br.com.itau.challenge.hello.adapter.output.dynamodb.mapper.toEntity
import br.com.itau.challenge.hello.domain.exception.AccountInexistentException
import br.com.itau.challenge.hello.domain.exception.CurrencyMismatchException
import br.com.itau.challenge.hello.domain.exception.IdempotencyConflictException
import br.com.itau.challenge.hello.domain.model.Amount
import br.com.itau.challenge.hello.domain.model.OperationType
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionResponse
import br.com.itau.challenge.hello.domain.model.TransactionStatus
import br.com.itau.challenge.hello.domain.model.toMinor
import br.com.itau.challenge.hello.port.output.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.util.*

@Repository
class AccountTransactionRepository(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${dynamodb.table-name}") private val tableName: String,
) : AccountRepository {

    private val logger = LoggerFactory.getLogger(AccountTransactionRepository::class.java)

    override fun createIfAbsent(accountId: String, owner: String, createdAtMicros: Long) {
        val pk = "ACCOUNT#$accountId"
        val sk = "PROFILE#METADATA"
        val item = mapOf(
            "pk" to AttributeValue.fromS(pk),
            "sk" to AttributeValue.fromS(sk),
            "accountId" to AttributeValue.fromS(accountId),
            "owner" to AttributeValue.fromS(owner),
            "createdAt" to AttributeValue.fromN(createdAtMicros.toString()),
            "balance" to AttributeValue.fromN("0"),
            "currency" to AttributeValue.fromS("BRL"),
            "status" to AttributeValue.fromS("ENABLED")
        )
        try {
            dynamoDbClient.putItem {
                it.tableName(tableName)
                it.item(item)
                it.conditionExpression("attribute_not_exists(pk)")
                it.expressionAttributeNames(mapOf("#pk" to "pk"))
            }
            logger.info("Conta {} criada com saldo zero", accountId)
        } catch (e: ConditionalCheckFailedException) {
            logger.debug("Conta {} já existe, ignorando", accountId)
        }
    }

    override fun save(transactionId: String, request: TransactionRequest): Pair<TransactionResponse, Long> {
        val pk = "ACCOUNT#${request.accountId}"
        val sk = "TRANS#$transactionId"
        val entity = request.toEntity(pk, sk, transactionId)
        return saveInternal(entity)
    }

    private fun saveInternal(transaction: TransactionEntity): Pair<TransactionResponse, Long> {
        val existing = getTransactionItem(transaction.pk, transaction.sk)
        if (existing != null) {
            val sameType = existing["type"]?.s() == transaction.type
            val sameValue = existing["amountValue"]?.s() == transaction.amountValue
            val sameCurrency = existing["amountCurrency"]?.s() == transaction.amountCurrency
            val sameAccount = existing["accountId"]?.s() == transaction.accountId
            if (!sameType || !sameValue || !sameCurrency || !sameAccount) {
                throw IdempotencyConflictException(transaction.transactionId)
            }
            val currentBalance = getCurrentBalance(transaction.pk)
            val status = try {
                TransactionStatus.valueOf(existing["status"]?.s() ?: "SUCCEEDED")
            } catch (e: Exception) {
                TransactionStatus.SUCCEEDED
            }
            logger.info("Transação {} replay idempotente para conta {} status {}", transaction.transactionId, transaction.accountId, status)
            return Pair(
                TransactionResponse(
                    id = UUID.fromString(transaction.transactionId),
                    type = OperationType.valueOf(transaction.type),
                    amount = Amount(transaction.amountValue, transaction.amountCurrency),
                    status = status
                ),
                currentBalance
            )
        }

        val accountItem = getAccountItem(transaction.pk)
        if (accountItem != null) {
            val accountCurrency = accountItem["currency"]?.s() ?: "BRL"
            if (accountCurrency != transaction.amountCurrency) {
                throw CurrencyMismatchException(transaction.accountId, accountCurrency, transaction.amountCurrency)
            }
        }
        val sk_account = "PROFILE#METADATA"
        val amountInCents = transaction.amountValue.toMinor()

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

        return try {
            dynamoDbClient.transactWriteItems(transactRequest)

            val newBalance = getCurrentBalance(transaction.pk)

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
                getCurrentBalance(transaction.pk)
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

    private fun getCurrentBalance(pk: String): Long {
        val getRequest = GetItemRequest.builder()
            .tableName(tableName)
            .key(mapOf("pk" to AttributeValue.fromS(pk), "sk" to AttributeValue.fromS("PROFILE#METADATA")))
            .consistentRead(true)
            .build()
        val response = dynamoDbClient.getItem(getRequest)
        val balanceAttr = response.item()?.get("balance") ?: throw AccountInexistentException(pk)
        return balanceAttr.n().toLong()
    }

    private fun getTransactionItem(pk: String, sk: String): Map<String, AttributeValue>? {
        val resp = dynamoDbClient.getItem { it.tableName(tableName).key(mapOf("pk" to AttributeValue.fromS(pk), "sk" to AttributeValue.fromS(sk))).consistentRead(true) }
        return if (resp.hasItem()) resp.item() else null
    }

    private fun getAccountItem(pk: String): Map<String, AttributeValue>? {
        val resp = dynamoDbClient.getItem { it.tableName(tableName).key(mapOf("pk" to AttributeValue.fromS(pk), "sk" to AttributeValue.fromS("PROFILE#METADATA"))).consistentRead(true) }
        return if (resp.hasItem()) resp.item() else null
    }
}