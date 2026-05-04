package sample

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactedToStringTest {

    @Test
    fun `class-level Redacted shows five stars regardless of value length`() {
        assertEquals("Password(password=*****)", Password("secret").toString())
    }

    @Test
    fun `class-level Redacted shows five stars even for an empty string`() {
        assertEquals("Password(password=*****)", Password("").toString())
    }

    @Test
    fun `class-level Redacted shows five stars even for a long string`() {
        assertEquals(
            "Password(password=*****)",
            Password("a-very-long-password-that-still-renders-as-five").toString()
        )
    }

    @Test
    fun `class-level Redacted applies five stars to every property`() {
        assertEquals(
            "MultiFieldRedacted(a=*****, b=*****, c=*****)",
            MultiFieldRedacted("hello", "world", 42).toString()
        )
    }

    @Test
    fun `class-level Redacted on a value class shows five stars`() {
        assertEquals("Token(value=*****)", Token("abc123").toString())
    }

    @Test
    fun `property-level Redacted with email mask leaves the domain intact`() {
        assertEquals(
            "EmailRedacted(address=****@example.com)",
            EmailRedacted("user@example.com").toString()
        )
    }

    @Test
    fun `property-level Redacted with email mask handles a multi-part TLD`() {
        assertEquals(
            "EmailRedacted(address=*****@example.co.za)",
            EmailRedacted("alice@example.co.za").toString()
        )
    }

    @Test
    fun `property-level Redacted leaves a value that does not match the mask untouched`() {
        assertEquals(
            "EmailRedacted(address=not-an-email)",
            EmailRedacted("not-an-email").toString()
        )
    }

    @Test
    fun `property-level Redacted with phone mask preserves the last four digits`() {
        assertEquals(
            "PhoneRedacted(number=******0599)",
            PhoneRedacted("0825370599").toString()
        )
    }

    @Test
    fun `property-level Redacted with phone mask leaves a four-digit value untouched`() {
        assertEquals("PhoneRedacted(number=0599)", PhoneRedacted("0599").toString())
    }

    @Test
    fun `property-level Redacted on a value class applies its mask`() {
        assertEquals(
            "EmailToken(address=****@example.com)",
            EmailToken("user@example.com").toString()
        )
    }

    @Test
    fun `non-annotated properties render normally alongside redacted ones`() {
        assertEquals(
            "Contact(name=Alice, mobile=**********, email=*****************)",
            Contact("Alice", "0825370599", "alice@example.com").toString()
        )
    }

    @Test
    fun `property-level Redacted with default mask redacts every character`() {
        assertEquals("Contact(name=A, mobile=****, email=*)", Contact("A", "0123", "x").toString())
    }

    @Test
    fun `property-level Redacted with default mask handles unicode characters`() {
        assertEquals("Contact(name=Z, mobile=****, email=)", Contact("Z", "café", "").toString())
    }

    @Test
    fun `redacted class embedded in a non-redacted parent stays redacted`() {
        assertEquals(
            "User(username=alice, password=Password(password=*****))",
            User("alice", Password("secret")).toString()
        )
    }

    // --- padToLength with PadDirection.START ---

    @Test
    fun `padToLength pads the start with stars when the value is shorter than the target`() {
        assertEquals("CardNumber(number=******5071)", CardNumber("5071").toString())
    }

    @Test
    fun `padToLength pads even very short values up to the target length`() {
        assertEquals("CardNumber(number=********83)", CardNumber("83").toString())
    }

    @Test
    fun `padToLength leaves a value that already meets the target unchanged after masking`() {
        assertEquals("CardNumber(number=******7890)", CardNumber("1234567890").toString())
    }

    @Test
    fun `padToLength trims a masked value that exceeds the target, keeping the unmasked tail`() {
        assertEquals(
            "CardNumber(number=******5071)",
            CardNumber("4111111111115071").toString()
        )
    }

    @Test
    fun `padToLength does not trim when the regex matched nothing`() {
        // No 4 trailing digits, so the mask never fires — value passes through untouched
        // even though it is longer than padToLength.
        assertEquals(
            "CardNumber(number=abcdefghijklm)",
            CardNumber("abcdefghijklm").toString()
        )
    }

    // --- padToLength with PadDirection.END ---

    @Test
    fun `padToLength with END direction pads the end with stars`() {
        assertEquals("OrderId(id=AB******)", OrderId("AB1234").toString())
    }

    @Test
    fun `padToLength with END direction pads short values that the mask cannot match`() {
        assertEquals("OrderId(id=AB******)", OrderId("AB").toString())
    }

    @Test
    fun `padToLength with END direction leaves a result that already meets the target unchanged`() {
        assertEquals("OrderId(id=AB******)", OrderId("AB123456").toString())
    }

    @Test
    fun `padToLength with END direction trims a masked value that exceeds the target, keeping the prefix`() {
        assertEquals("OrderId(id=AB******)", OrderId("AB12345678").toString())
    }

    // --- padToLength does not affect class-level redaction ---

    @Test
    fun `class-level Redacted ignores padToLength on the annotation`() {
        // Even if padToLength were set at class level, the literal five-star rendering wins.
        assertEquals("Password(password=*****)", Password("any value at all").toString())
    }
}
