package com.tvfile.app.util

import java.math.BigInteger

/**
 * 自然数混合文件名排序比较器 (Natural Order / Alphanumeric Sort)
 * 解决 1, 10, 100, 2, 20 乱序问题，实现符合人类直觉与系统文件管理器的 1, 2, 3... 10, 20, 100 排序。
 */
object NaturalOrderComparator : Comparator<String> {

    private val REGEX = Regex("(\\d+)|(\\D+)")

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val tokens1 = REGEX.findAll(s1).map { it.value }.toList()
        val tokens2 = REGEX.findAll(s2).map { it.value }.toList()

        val minLen = minOf(tokens1.size, tokens2.size)
        for (i in 0 until minLen) {
            val t1 = tokens1[i]
            val t2 = tokens2[i]

            val isDigit1 = t1.isNotEmpty() && t1[0].isDigit()
            val isDigit2 = t2.isNotEmpty() && t2[0].isDigit()

            if (isDigit1 && isDigit2) {
                val b1 = runCatching { BigInteger(t1) }.getOrDefault(BigInteger.ZERO)
                val b2 = runCatching { BigInteger(t2) }.getOrDefault(BigInteger.ZERO)
                val cmp = b1.compareTo(b2)
                if (cmp != 0) return cmp
                // 数值相等但前导零不同（例如 "01" vs "1"），短的排前
                val lenCmp = t1.length.compareTo(t2.length)
                if (lenCmp != 0) return lenCmp
            } else {
                val cmp = t1.compareTo(t2, ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }

        return tokens1.size.compareTo(tokens2.size)
    }
}
