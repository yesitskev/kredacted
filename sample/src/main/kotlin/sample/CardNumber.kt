package sample

import kredacted.Redacted

data class CardNumber(
    @Redacted(
        mask = """/.+(?=\d{4}$)/g""",
        padToLength = 10,
    ) val number: String
)
