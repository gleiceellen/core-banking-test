package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.domain.exception.AccountInexistentException
import br.com.itau.challenge.hello.domain.exception.AccountNotFoundException
import br.com.itau.challenge.hello.domain.exception.CurrencyMismatchException
import br.com.itau.challenge.hello.domain.exception.IdempotencyConflictException
import br.com.itau.challenge.hello.domain.exception.InvalidAmountException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkClientException

data class ErrorResponse(val message: String, val field: String? = null)
data class ValidationErrorResponse(val message: String, val errors: List<ErrorResponse>)

@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmount(ex: InvalidAmountException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Invalid amount"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { ErrorResponse(it.defaultMessage ?: "invalid", it.field) }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ValidationErrorResponse("Validation failed", errors))
    }

    @ExceptionHandler(AccountNotFoundException::class, AccountInexistentException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message ?: "Account not found"))
    }

    @ExceptionHandler(CurrencyMismatchException::class)
    fun handleCurrency(ex: CurrencyMismatchException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorResponse(ex.message ?: "Currency mismatch"))
    }

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleConflict(ex: IdempotencyConflictException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message ?: "Idempotency conflict"))
    }

    @ExceptionHandler(SdkClientException::class)
    fun handleSdkClient(ex: SdkClientException): ResponseEntity<ErrorResponse> {
        logger.error("DynamoDB client error", ex)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "2")
            .body(ErrorResponse("Service temporarily unavailable"))
    }

    @ExceptionHandler(AwsServiceException::class)
    fun handleAwsService(ex: AwsServiceException): ResponseEntity<ErrorResponse> {
        logger.error("DynamoDB service error status={}", ex.statusCode(), ex)
        val status = ex.statusCode()
        return if (status in 500..599) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "2")
                .body(ErrorResponse("Service temporarily unavailable"))
        } else {
            logger.error("DynamoDB 4xx bug", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Internal server error"))
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Internal server error"))
    }
}
