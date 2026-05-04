package kredacted

/**
 * Side on which [Redacted.padToLength] adds `*` characters when padding a
 * masked value, and conversely, the side that is preserved when a masked
 * value longer than the target is trimmed.
 *
 * @see Redacted.padToLength
 * @see Redacted.padToLengthDirection
 */
enum class PadDirection {
    /**
     * Pad the **start** of the masked value with `*`. When trimming a result
     * that exceeds [Redacted.padToLength], the trailing portion of the value
     * is kept.
     *
     * Useful for "reveal the last *N* characters" patterns such as credit-card
     * numbers, phone numbers, or any identifier whose tail is the meaningful
     * disambiguator.
     *
     * ```
     * "5071"            → "******5071"
     * "83"              → "********83"
     * "4111111111115071" → "******5071"  // trimmed, suffix kept
     * ```
     */
    START,

    /**
     * Pad the **end** of the masked value with `*`. When trimming a result
     * that exceeds [Redacted.padToLength], the leading portion of the value
     * is kept.
     *
     * Useful for "reveal the first *N* characters" patterns such as
     * partially-revealed order ids or correlation tokens whose prefix
     * encodes a routing or tenant hint.
     *
     * ```
     * "AB1234"     → "AB******"
     * "AB"         → "AB******"
     * "AB12345678" → "AB******"  // trimmed, prefix kept
     * ```
     */
    END,
}
