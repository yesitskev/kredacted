package sample

import kredacted.PadDirection
import kredacted.Redacted

data class OrderId(
    @Redacted(
        mask = """/(?<=^.{2}).+/g""",
        padToLength = 8,
        padToLengthDirection = PadDirection.END,
    ) val id: String
)
