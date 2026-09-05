package br.com.itau.challenge.hello.domain.model

import br.com.itau.challenge.hello.domain.exception.InvalidAmountException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyMinorTest {

    @Test
    fun `should convert string to minor without fraction`() {
        assertEquals(10000L, "100".toMinor())
        assertEquals(100L, "1".toMinor())
        assertEquals(1L, "0.01".toMinor())
    }

    @Test
    fun `should convert string with one decimal`() {
        assertEquals(150L, "1.5".toMinor())
        assertEquals(1050L, "10.5".toMinor())
    }

    @Test
    fun `should convert string with two decimals`() {
        assertEquals(9707L, "97.07".toMinor())
        assertEquals(18312L, "183.12".toMinor())
        assertEquals(1L, "0.01".toMinor())
    }

    @Test
    fun `should convert minor to major string`() {
        assertEquals("97.07", 9707L.toMajorString())
        assertEquals("1.50", 150L.toMajorString())
        assertEquals("0.01", 1L.toMajorString())
        assertEquals("100.00", 10000L.toMajorString())
    }

    @Test
    fun `should reject blank amount`() {
        assertFailsWith<InvalidAmountException> { "".toMinor() }
        assertFailsWith<InvalidAmountException> { "   ".toMinor() }
    }

    @Test
    fun `should reject zero and negative`() {
        assertFailsWith<InvalidAmountException> { "0".toMinor() }
        assertFailsWith<InvalidAmountException> { "0.00".toMinor() }
        assertFailsWith<InvalidAmountException> { "-5".toMinor() }
        assertFailsWith<InvalidAmountException> { "+5".toMinor() }
    }

    @Test
    fun `should reject more than two decimals`() {
        assertFailsWith<InvalidAmountException> { "97.070".toMinor() }
        assertFailsWith<InvalidAmountException> { "1.123".toMinor() }
    }

    @Test
    fun `should reject invalid format`() {
        assertFailsWith<InvalidAmountException> { "abc".toMinor() }
        assertFailsWith<InvalidAmountException> { "12.3.4".toMinor() }
        assertFailsWith<InvalidAmountException> { ".5".toMinor() }
        assertFailsWith<InvalidAmountException> { "1.5a".toMinor() }
    }

    @Test
    fun `should reject overflow`() {
        assertFailsWith<InvalidAmountException> { "9999999999999999999".toMinor() }
    }
}
