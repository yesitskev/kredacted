# kredacted

A Kotlin compiler plugin that rewrites `toString()` on `@Redacted` classes and properties to mask sensitive values — with zero runtime reflection.

## How it works

The plugin hooks into the Kotlin IR pipeline and replaces the synthesized `toString` body of any annotated data class or value class at compile time. There is no runtime overhead beyond the string operations themselves, and no reflection.

## Setup

Apply the Gradle plugin and add the annotations dependency:

```kotlin
// build.gradle.kts
plugins {
    id("io.github.yesitskev.kredacted") version "0.1.0"
}
```

The plugin automatically wires up the compiler plugin and annotations artifact; no further configuration is needed.

## Usage

### Class-level redaction

Annotate the class to replace every property value with `*****` in `toString`:

```kotlin
@Redacted
data class Password(val password: String)

Password("hunter2").toString()
// → "Password(password=*****)"
```

Every property is masked, regardless of its value or length:

```kotlin
@Redacted
data class MultiFieldRedacted(val a: String, val b: String, val c: Int)

MultiFieldRedacted("hello", "world", 42).toString()
// → "MultiFieldRedacted(a=*****, b=*****, c=*****)"
```

Works on value classes too:

```kotlin
@JvmInline
@Redacted
value class Token(val value: String)

Token("abc123").toString()
// → "Token(value=*****)"
```

### Property-level redaction

Annotate individual properties to redact only those fields; siblings render normally:

```kotlin
data class Contact(
    val name: String,
    @Redacted val mobile: String,
    @Redacted val email: String,
)

Contact("Alice", "0825370599", "alice@example.com").toString()
// → "Contact(name=Alice, mobile=**********, email=*****************)"
```

### Mask patterns

The `mask` parameter accepts a regular expression selecting which characters to replace with `*`. Each matched character becomes a single `*`, preserving the surrounding text.

Patterns may be written as a plain regex or with JavaScript-style `/pattern/flags` delimiters — surrounding slashes and trailing flags are stripped before compilation.

**Mask the local part of an email address:**

```kotlin
data class EmailRedacted(
    @Redacted(mask = """/^\w+(?=@)/g""") val address: String
)

EmailRedacted("user@example.com").toString()
// → "EmailRedacted(address=****@example.com)"

EmailRedacted("not-an-email").toString()
// → "EmailRedacted(address=not-an-email)"  // mask matched nothing — value passes through
```

**Mask all but the last four digits of a phone number:**

```kotlin
data class PhoneRedacted(
    @Redacted(mask = """/\d+(?=\d{4}$)/g""") val number: String
)

PhoneRedacted("0825370599").toString()
// → "PhoneRedacted(number=******0599)"

PhoneRedacted("0599").toString()
// → "PhoneRedacted(number=0599)"  // fewer than 4 digits to reveal — untouched
```

### Fixed-width output with `padToLength`

Use `padToLength` to produce a deterministic-width redaction — useful when the length of a value itself would leak information.

**`PadDirection.START` (default) — keep the suffix, pad/trim the start:**

```kotlin
data class CardNumber(
    @Redacted(
        mask = """/.+(?=\d{4}$)/g""",
        padToLength = 10,
    ) val number: String
)

CardNumber("5071").toString()             // → "CardNumber(number=******5071)"  padded
CardNumber("83").toString()              // → "CardNumber(number=********83)"   padded
CardNumber("1234567890").toString()      // → "CardNumber(number=******7890)"   exact fit
CardNumber("4111111111115071").toString()// → "CardNumber(number=******5071)"   trimmed
```

If the mask never matches, the value is returned untouched even if it exceeds `padToLength`:

```kotlin
CardNumber("abcdefghijklm").toString()
// → "CardNumber(number=abcdefghijklm)"  // no trim when mask matched nothing
```

**`PadDirection.END` — keep the prefix, pad/trim the end:**

```kotlin
data class OrderId(
    @Redacted(
        mask = """/(?<=^.{2}).+/g""",
        padToLength = 8,
        padToLengthDirection = PadDirection.END,
    ) val id: String
)

OrderId("AB1234").toString()    // → "OrderId(id=AB******)"  padded
OrderId("AB").toString()        // → "OrderId(id=AB******)"  padded (mask matched nothing)
OrderId("AB123456").toString()  // → "OrderId(id=AB******)"  exact fit
OrderId("AB12345678").toString()// → "OrderId(id=AB******)"  trimmed
```

### Redacted classes compose naturally

A `@Redacted` class embedded inside a non-redacted class still masks its own `toString`:

```kotlin
@Redacted
data class Password(val password: String)

data class User(
    val username: String,
    val password: Password,
)

User("alice", Password("secret")).toString()
// → "User(username=alice, password=Password(password=*****))"
```

## Annotation reference

```kotlin
@Target(CLASS, PROPERTY)
@Retention(SOURCE)
annotation class Redacted(
    val mask: String = "/./g",           // regex selecting characters to replace with *
    val padToLength: Int = -1,           // fixed output width; -1 disables
    val padToLengthDirection: PadDirection = PadDirection.START,
)
```

| Parameter | Default | Description |
|---|---|---|
| `mask` | `/./g` (every character) | Regex selecting which characters become `*`. Non-matching characters are preserved. |
| `padToLength` | `-1` (disabled) | Enforces a fixed output width. Shorter results are padded with `*`; longer masked results are trimmed. Ignored at class level. |
| `padToLengthDirection` | `START` | `START` keeps the suffix and pads/trims the start. `END` keeps the prefix and pads/trims the end. |

Class-level `@Redacted` always renders `*****` for every property, ignoring `mask`, `padToLength`, and `padToLengthDirection`.

## License

MIT — see [LICENSE.md](LICENSE.md).