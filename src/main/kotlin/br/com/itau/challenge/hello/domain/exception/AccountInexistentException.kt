package br.com.itau.challenge.hello.domain.exception

class AccountInexistentException(accountId: String) : RuntimeException("Account ${accountId} not found.")
