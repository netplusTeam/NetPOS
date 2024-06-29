package com.woleapp.netpos.util


object XorUtil {
    fun xorHex(a: String, b: String): String {
        val chars = CharArray(a.length)
        for (i in chars.indices) {
            chars[i] = toHex(fromHex(a[i]) xor fromHex(b[i]))
        }
        return String(chars)
    }

    private fun fromHex(c: Char): Int {
        if (c in '0'..'9') {
            return c - '0'
        }
        if (c in 'A'..'F') {
            return c - 'A' + 10
        }
        if (c in 'a'..'f') {
            return c - 'a' + 10
        }
        throw IllegalArgumentException()
    }

    private fun toHex(nybble: Int): Char {
        require(!(nybble < 0 || nybble > 15))
        return "0123456789ABCDEF"[nybble]
    }
}
