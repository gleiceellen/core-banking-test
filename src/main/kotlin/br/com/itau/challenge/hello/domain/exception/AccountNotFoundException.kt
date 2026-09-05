package br.com.itau.challenge.hello.domain.exception

class AccountNotFoundException(accountId: String) : RuntimeException("Account $accountId not found")
