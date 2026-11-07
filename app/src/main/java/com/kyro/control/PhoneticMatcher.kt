package com.kyro.control

class PhoneticMatcher {

    fun match(input: String, target: String): Float {
        val cleanInput = input.lowercase().trim()
        val cleanTarget = target.lowercase().trim()

        if (cleanInput == cleanTarget) return 1.0f
        if (cleanInput.isEmpty() || cleanTarget.isEmpty()) return 0f

        val distance = levenshteinDistance(cleanInput, cleanTarget)
        val maxLength = maxOf(cleanInput.length, cleanTarget.length)

        return 1.0f - (distance.toFloat() / maxLength)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) {
            for (j in 0..b.length) {
                if (i == 0) {
                    dp[i][j] = j
                } else if (j == 0) {
                    dp[i][j] = i
                } else {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j - 1] + cost, // substitution
                        dp[i - 1][j] + 1,        // deletion
                        dp[i][j - 1] + 1         // insertion
                    )
                }
            }
        }
        return dp[a.length][b.length]
    }
}
