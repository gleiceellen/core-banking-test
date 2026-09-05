package br.com.itau.challenge.hello.adapter.output.dynamodb

import br.com.itau.challenge.hello.domain.exception.AccountInexistentException
import br.com.itau.challenge.hello.domain.exception.AccountNotFoundException
import br.com.itau.challenge.hello.domain.exception.CurrencyMismatchException
import br.com.itau.challenge.hello.domain.exception.IdempotencyConflictException
import br.com.itau.challenge.hello.domain.model.Amount
import br.com.itau.challenge.hello.domain.model.OperationType
import br.com.itau.challenge.hello.domain.model.TransactionRequest
import br.com.itau.challenge.hello.domain.model.TransactionStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountTransactionRepositoryTest {

    @Test
    fun `should create account if absent`() {
        val client = mock<DynamoDbClient>()
        whenever(client.putItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.PutItemRequest::class.java))).thenReturn(software.amazon.awssdk.services.dynamodb.model.PutItemResponse.builder().build())
        val repo = AccountTransactionRepository(client, "core_banking")
        repo.createIfAbsent(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 123L)
        // no exception
    }

    @Test
    fun `should ignore create when already exists`() {
        val client = mock<DynamoDbClient>()
        whenever(client.putItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.PutItemRequest::class.java)))
            .thenThrow(ConditionalCheckFailedException.builder().message("exists").build())
        val repo = AccountTransactionRepository(client, "core_banking")
        repo.createIfAbsent("id", "owner", 123L)
    }

    @Test
    fun `should throw currency mismatch`() {
        val accountId = UUID.randomUUID()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("10.00", "USD"))
        val client = mock<DynamoDbClient>()
        val accountItem = mapOf("currency" to AttributeValue.fromS("BRL"), "balance" to AttributeValue.fromN("1000"))
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s()
            if (sk?.startsWith("TRANS#") == true) GetItemResponse.builder().build()
            else GetItemResponse.builder().item(accountItem).build()
        }
        val repo = AccountTransactionRepository(client, "core_banking")
        assertFailsWith<CurrencyMismatchException> { repo.save(UUID.randomUUID().toString(), req) }
    }

    @Test
    fun `should detect idempotency conflict`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("10.00", "BRL"))
        val existing = mapOf(
            "type" to AttributeValue.fromS("DEBIT"),
            "amountValue" to AttributeValue.fromS("10.00"),
            "amountCurrency" to AttributeValue.fromS("BRL"),
            "accountId" to AttributeValue.fromS(accountId.toString()),
            "status" to AttributeValue.fromS("SUCCEEDED")
        )
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { GetItemResponse.builder().item(existing).build() }
        val repo = AccountTransactionRepository(client, "core_banking")
        assertFailsWith<IdempotencyConflictException> { repo.save(txId, req) }
    }

    @Test
    fun `should return idempotent same payload with current balance`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("10.00", "BRL"))
        val existing = mapOf(
            "type" to AttributeValue.fromS("CREDIT"),
            "amountValue" to AttributeValue.fromS("10.00"),
            "amountCurrency" to AttributeValue.fromS("BRL"),
            "accountId" to AttributeValue.fromS(accountId.toString()),
            "status" to AttributeValue.fromS("SUCCEEDED")
        )
        val client = mock<DynamoDbClient>()
        var call = 0
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { _ ->
            call++
            if (call == 1) GetItemResponse.builder().item(existing).build()
            else GetItemResponse.builder().item(mapOf("balance" to AttributeValue.fromN("5000"), "currency" to AttributeValue.fromS("BRL"))).build()
        }
        val repo = AccountTransactionRepository(client, "core_banking")
        val (resp, bal) = repo.save(txId, req)
        assertEquals(TransactionStatus.SUCCEEDED, resp.status)
        assertEquals(5000L, bal)
    }

    @Test
    fun `should save credit successfully`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("10.00", "BRL"))
        val client = mock<DynamoDbClient>()
        var call = 0
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s() ?: ""
            if (sk.startsWith("TRANS#")) GetItemResponse.builder().build()
            else if (call++ == 0) GetItemResponse.builder().item(mapOf("currency" to AttributeValue.fromS("BRL"), "balance" to AttributeValue.fromN("0"))).build()
            else GetItemResponse.builder().item(mapOf("balance" to AttributeValue.fromN("1000"))).build()
        }
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenReturn(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse.builder().build())
        val repo = AccountTransactionRepository(client, "core_banking")
        val (resp, bal) = repo.save(txId, req)
        assertEquals(TransactionStatus.SUCCEEDED, resp.status)
        assertEquals(1000L, bal)
    }

    @Test
    fun `should handle insufficient balance as FAILED`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.DEBIT, Amount("100.00", "BRL"))
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s() ?: ""
            if (sk.startsWith("TRANS#")) GetItemResponse.builder().build()
            else GetItemResponse.builder().item(mapOf("balance" to AttributeValue.fromN("50"), "currency" to AttributeValue.fromS("BRL"))).build()
        }
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenThrow(ConditionalCheckFailedException.builder().message("cond").build())
        val repo = AccountTransactionRepository(client, "core_banking")
        val (resp, bal) = repo.save(txId, req)
        assertEquals(TransactionStatus.FAILED, resp.status)
        assertEquals(50L, bal)
    }

    @Test
    fun `should throw when account not found on save`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.DEBIT, Amount("10.00", "BRL"))
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s() ?: ""
            if (sk.startsWith("TRANS#")) GetItemResponse.builder().build()
            else GetItemResponse.builder().build() // no account
        }
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenThrow(ConditionalCheckFailedException.builder().message("cond").build())
        val repo = AccountTransactionRepository(client, "core_banking")
        assertFailsWith<AccountNotFoundException> { repo.save(txId, req) }
    }

    @Test
    fun `should handle insufficient via TransactionCanceledException`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.DEBIT, Amount("100.00", "BRL"))
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s() ?: ""
            if (sk.startsWith("TRANS#")) GetItemResponse.builder().build()
            else GetItemResponse.builder().item(mapOf("balance" to AttributeValue.fromN("50"), "currency" to AttributeValue.fromS("BRL"))).build()
        }
        val ex = software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException.builder()
            .message("canceled")
            .cancellationReasons(
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("ConditionalCheckFailed").build(),
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("None").build()
            ).build()
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenThrow(ex)
        val repo = AccountTransactionRepository(client, "core_banking")
        val (resp, bal) = repo.save(txId, req)
        assertEquals(TransactionStatus.FAILED, resp.status)
        assertEquals(50L, bal)
    }

    @Test
    fun `should throw account not found via TransactionCanceledException`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.DEBIT, Amount("10.00", "BRL"))
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { inv ->
            val r = inv.arguments[0] as GetItemRequest
            val sk = r.key()["sk"]?.s() ?: ""
            if (sk.startsWith("TRANS#")) GetItemResponse.builder().build()
            else GetItemResponse.builder().build()
        }
        val ex = software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException.builder()
            .message("canceled")
            .cancellationReasons(
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("ConditionalCheckFailed").build(),
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("None").build()
            ).build()
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenThrow(ex)
        val repo = AccountTransactionRepository(client, "core_banking")
        assertFailsWith<AccountNotFoundException> { repo.save(txId, req) }
    }

    @Test
    fun `should throw idempotency conflict via TransactionCanceledException second item`() {
        val accountId = UUID.randomUUID()
        val txId = UUID.randomUUID().toString()
        val req = TransactionRequest(accountId, OperationType.CREDIT, Amount("10.00", "BRL"))
        val client = mock<DynamoDbClient>()
        whenever(client.getItem(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest::class.java))).thenAnswer { GetItemResponse.builder().build() }
        val ex = software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException.builder()
            .message("canceled")
            .cancellationReasons(
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("None").build(),
                software.amazon.awssdk.services.dynamodb.model.CancellationReason.builder().code("ConditionalCheckFailed").build()
            ).build()
        whenever(client.transactWriteItems(org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest::class.java))).thenThrow(ex)
        val repo = AccountTransactionRepository(client, "core_banking")
        assertFailsWith<IdempotencyConflictException> { repo.save(txId, req) }
    }
}
