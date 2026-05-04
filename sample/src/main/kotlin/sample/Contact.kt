package sample

import kredacted.Redacted

data class Contact(
    val name: String,
    @Redacted val mobile: String,
    @Redacted val email: String
)