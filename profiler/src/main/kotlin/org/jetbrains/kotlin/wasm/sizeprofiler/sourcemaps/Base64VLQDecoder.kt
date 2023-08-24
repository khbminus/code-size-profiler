package org.jetbrains.kotlin.wasm.sizeprofiler.sourcemaps

class Base64VLQDecoder {
    fun decode(inputString: String): List<Int> = buildList {
        var currentValue = 0
        var currentShift = 0
        inputString.forEach { symbol ->
            val integerValue = getIndex(symbol)
            val continuationBit = integerValue and 32
            val remainderValue = integerValue and 31
            currentValue += remainderValue shl currentShift
            if (continuationBit != 0) {
                currentShift += 5
            } else {
                val shouldNegate = currentValue and 1
                currentValue = currentValue shr 1
                add(if (shouldNegate == 0) currentValue else if (currentValue == 0) Int.MIN_VALUE else -currentValue)
                currentValue = 0
                currentShift = 0
            }
        }
    }

    companion object {
        private fun getIndex(symbol: Char): Int = when (symbol) {
            in 'A'..'Z' -> symbol.code - 'A'.code
            in 'a'..'z' -> symbol.code - 'a'.code + 26
            in '0'..'9' -> symbol.code - '0'.code + 26 * 2
            '+' -> 26 * 2 + 10
            '/' -> 26 * 2 + 10 + 1
            '=' -> 26 * 2 + 10 + 2
            else -> error("Invalid symbol $symbol")
        }
    }
}