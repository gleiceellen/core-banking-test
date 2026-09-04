package br.com.itau.challenge.hello.domain.exception

class InsufficientBalanceException(accountId: String) : RuntimeException("Account $accountId not found or balance insufficient")
