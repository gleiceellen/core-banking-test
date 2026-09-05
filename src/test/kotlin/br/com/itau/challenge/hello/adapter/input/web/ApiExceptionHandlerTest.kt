package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.domain.exception.AccountNotFoundException
import br.com.itau.challenge.hello.domain.exception.CurrencyMismatchException
import br.com.itau.challenge.hello.domain.exception.IdempotencyConflictException
import br.com.itau.challenge.hello.domain.exception.InvalidAmountException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException
import kotlin.test.assertEquals

class ApiExceptionHandlerTest {

    private val handler = ApiExceptionHandler()

    @Test
    fun `should handle invalid amount`() {
        val resp = handler.handleInvalidAmount(InvalidAmountException("bad"))
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `should handle not found`() {
        val resp = handler.handleNotFound(AccountNotFoundException("acc1"))
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @Test
    fun `should handle currency mismatch`() {
        val resp = handler.handleCurrency(CurrencyMismatchException("acc1", "BRL", "USD"))
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.statusCode)
    }

    @Test
    fun `should handle conflict`() {
        val resp = handler.handleConflict(IdempotencyConflictException("tx1"))
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
    }

    @Test
    fun `should handle sdk client as 503`() {
        val ex = SdkClientException.builder().message("timeout").build()
        val resp = handler.handleSdkClient(ex)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.statusCode)
        assertEquals("2", resp.headers.getFirst("Retry-After"))
    }

    @Test
    fun `should handle aws 5xx as 503`() {
        val ex = AwsServiceException.builder().message("throttle").statusCode(500).build() as AwsServiceException
        val resp = handler.handleAwsService(ex)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.statusCode)
    }

    @Test
    fun `should handle aws 4xx as 500`() {
        val ex = AwsServiceException.builder().message("validation").statusCode(400).build() as AwsServiceException
        val resp = handler.handleAwsService(ex)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.statusCode)
    }

    @Test
    fun `should handle generic as 500`() {
        val resp = handler.handleGeneric(RuntimeException("boom"))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.statusCode)
    }
}
