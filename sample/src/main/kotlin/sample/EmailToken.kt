package sample

import kredacted.Redacted

@JvmInline
value class EmailToken(
    @Redacted(mask = """/^\w+(?=@)/g""") val address: String
)
