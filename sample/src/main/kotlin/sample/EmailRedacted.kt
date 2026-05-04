package sample

import kredacted.Redacted

data class EmailRedacted(
    @Redacted(mask = """/^\w+(?=@)/g""") val address: String
)
