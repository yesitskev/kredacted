package kredacted

import org.intellij.lang.annotations.Language
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Marks a class or property whose `toString` representation should be redacted.
 *
 * The kredacted compiler plugin rewrites the synthesized `toString` of any class
 * that carries this annotation, or that has at least one property carrying it.
 * Two distinct redaction modes are supported.
 *
 * ## Class-level redaction
 *
 * When applied to a class, every property in the generated `toString` is rendered
 * as the literal `*****` (five stars). The [mask], [padToLength], and
 * [padToLengthDirection] fields are **ignored** at class level — the literal
 * five-star rendering always wins.
 *
 * ```
 * @Redacted
 * data class Password(val value: String)
 *
 * Password("hunter2").toString() // → "Password(value=*****)"
 * ```
 *
 * ## Property-level redaction
 *
 * When applied to a property, only that property is redacted; sibling properties
 * render normally. The [mask] regular expression decides which characters become
 * `*` (one star per matched character). [padToLength] and [padToLengthDirection]
 * optionally enforce a fixed output width.
 *
 * ```
 * data class Contact(
 *     val name: String,
 *     @Redacted(mask = "/^\\w+(?=@)/g") val email: String,
 * )
 *
 * Contact("Alice", "alice@example.com").toString()
 * // → "Contact(name=Alice, email=****@example.com)"
 * ```
 *
 * ## Fixed-width output
 *
 * Combine [mask] with [padToLength] to produce a deterministic-width redaction
 * — useful for credit-card numbers, tokens, and other identifiers where the
 * mere length of the value can leak information.
 *
 * ```
 * data class CardNumber(
 *     @Redacted(
 *         mask = "/.+(?=\\d{4}$)/g",
 *         padToLength = 10,
 *     ) val number: String,
 * )
 *
 * CardNumber("4111111111115071").toString() // → "CardNumber(number=******5071)"
 * CardNumber("5071").toString()             // → "CardNumber(number=******5071)"
 * CardNumber("83").toString()               // → "CardNumber(number=********83)"
 * ```
 *
 * @property mask Regular expression selecting the characters to replace with
 *   `*`. Each matched character becomes a single `*`, preserving the matched
 *   substring's length in the output. The pattern may be written either as a
 *   plain regex (`\d+`) or wrapped in JavaScript-style delimiters (`/\d+/g`);
 *   surrounding slashes and trailing flags are stripped before compilation.
 *   The default `/./g` masks every character. Ignored when the annotation is
 *   applied at class level.
 *
 * @property padToLength Optional fixed output width. Behaviour when this is
 *   non-negative:
 *
 *   - **Shorter than target** — the masked result is padded with `*` on the
 *     side specified by [padToLengthDirection].
 *   - **Longer than target, mask matched at least once** — the result is
 *     trimmed to this length, keeping the side opposite [padToLengthDirection].
 *   - **Longer than target, mask matched nothing** — the value is returned
 *     untouched (no trim). This protects values that legitimately differ from
 *     the expected shape from being silently truncated.
 *
 *   A negative value (the default `-1`) disables padding and trimming entirely.
 *   Ignored when the annotation is applied at class level.
 *
 * @property padToLengthDirection Side on which [padToLength] adds stars when
 *   padding, and conversely, the side that gets trimmed when the masked value
 *   exceeds [padToLength]. [PadDirection.START] keeps the value's suffix
 *   (e.g. last four digits of a card); [PadDirection.END] keeps the prefix
 *   (e.g. first two characters of an order id). Ignored when [padToLength] is
 *   negative or when the annotation is applied at class level.
 *
 * @see PadDirection
 */
@Target(CLASS, PROPERTY)
@Retention(SOURCE)
annotation class Redacted(
    @Language("RegExp") val mask: String = "/./g",
    val padToLength: Int = -1,
    val padToLengthDirection: PadDirection = PadDirection.START,
)
