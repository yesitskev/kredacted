package kredacted

/**
 * Runtime support invoked by the kredacted compiler plugin's rewritten
 * `toString` to mask a property value. Not intended for direct use.
 */
object RedactedHelper {

    fun redact(
        value: String,
        mask: String,
        padToLength: Int,
        padDirection: PadDirection,
    ): String {
        val regex = Regex(normalizeMask(mask))
        var matched = false
        val masked = regex.replace(value) { match ->
            matched = true
            "*".repeat(match.value.length)
        }

        if (padToLength < 0 || masked.length == padToLength) return masked

        if (masked.length < padToLength) {
            val pad = "*".repeat(padToLength - masked.length)
            return when (padDirection) {
                PadDirection.START -> pad + masked
                PadDirection.END -> masked + pad
            }
        }

        if (!matched) return value

        return when (padDirection) {
            PadDirection.START -> masked.substring(masked.length - padToLength)
            PadDirection.END -> masked.substring(0, padToLength)
        }
    }

    private fun normalizeMask(mask: String): String {
        if (!mask.startsWith("/")) return mask
        val lastSlash = mask.lastIndexOf('/')
        if (lastSlash <= 0) return mask
        return mask.substring(1, lastSlash)
    }
}