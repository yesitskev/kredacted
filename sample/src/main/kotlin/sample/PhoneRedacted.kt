package sample

import kredacted.Redacted

data class PhoneRedacted(
    @Redacted(mask = """/\d+(?=\d{4}$)/g""") val number: String
)
