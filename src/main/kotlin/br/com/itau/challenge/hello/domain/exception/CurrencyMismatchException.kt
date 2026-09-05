package br.com.itau.challenge.hello.domain.exception

class CurrencyMismatchException(
    val accountId: String,
    val accountCurrency: String,
    val requestedCurrency: String,
) : RuntimeException("Currency mismatch for account $accountId: account=$accountCurrency requested=$requestedCurrency")
