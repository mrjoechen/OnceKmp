package io.github.mrjoechen

enum class OnceTimeUnit(private val millisPerUnit: Long) {
    MILLISECONDS(1L),
    SECONDS(1_000L),
    MINUTES(60_000L),
    HOURS(3_600_000L),
    DAYS(86_400_000L),
    ;

    fun toMillis(amount: Long): Long = amount * millisPerUnit
}
