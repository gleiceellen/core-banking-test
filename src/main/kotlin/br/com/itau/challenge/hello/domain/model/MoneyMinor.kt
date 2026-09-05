package br.com.itau.challenge.hello.domain.model

import br.com.itau.challenge.hello.domain.exception.InvalidAmountException

fun String.toMinor(): Long {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) throw InvalidAmountException("amount must not be blank")
    if (trimmed.startsWith("-") || trimmed.startsWith("+")) throw InvalidAmountException("amount must be greater than 0")

    val parts = trimmed.split(".")
    if (parts.size > 2) throw InvalidAmountException("amount has invalid format")

    val intPart = parts[0]
    val fracPart = if (parts.size == 2) parts[1] else ""

    if (intPart.isEmpty() || !intPart.all { it.isDigit() }) throw InvalidAmountException("amount has invalid format")
    if (fracPart.isNotEmpty() && !fracPart.all { it.isDigit() }) throw InvalidAmountException("amount has invalid format")
    if (fracPart.length > 2) throw InvalidAmountException("amount must have at most 2 decimal places")

    val centsFromInt = try {
        intPart.toLong() * 100L
    } catch (e: NumberFormatException) {
        throw InvalidAmountException("amount is too large")
    }

    val centsFromFrac = when (fracPart.length) {
        0 -> 0L
        1 -> fracPart.toLong() * 10L
        2 -> fracPart.toLong()
        else -> 0L
    }

    val total = try {
        Math.addExact(centsFromInt, centsFromFrac)
    } catch (e: ArithmeticException) {
        throw InvalidAmountException("amount is too large")
    }

    if (total <= 0) throw InvalidAmountException("amount must be greater than 0")
    return total
}

fun Long.toMajorString(): String {
    val negative = this < 0
    val abs = kotlin.math.abs(this)
    val intPart = abs / 100
    val fracPart = abs % 100
    val formatted = "$intPart.${fracPart.toString().padStart(2, '0')}"
    return if (negative) "-$formatted" else formatted
}
