package br.com.itau.challenge.hello.domain.exception

class IdempotencyConflictException(transactionId: String) :
    RuntimeException("Transaction $transactionId was already submitted with a different payload")
