package com.dairypulse.app.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

fun money(currency: String, amount: Double): String =
    "$currency " + String.format(Locale.US, "%,.2f", amount)

/** "41.5k" style used on the summary card. */
fun compact(amount: Double): String = when {
    abs(amount) >= 1_000_000 -> String.format(Locale.US, "%.1fM", amount / 1_000_000)
    abs(amount) >= 1_000 -> String.format(Locale.US, "%.1fk", amount / 1_000)
    else -> String.format(Locale.US, "%.0f", amount)
}

fun num(value: Double, decimals: Int = 1): String {
    val s = String.format(Locale.US, "%,.${decimals}f", value)
    return if (decimals > 0) s.trimEnd('0').trimEnd('.') else s
}

fun pct(value: Double): String = String.format(Locale.US, "%+.1f%%", value)

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
fun LocalDate.pretty(): String = format(dateFmt)

/** Parses user input like "1,250.50"; returns null when blank or invalid. */
fun String.toAmount(): Double? = replace(",", "").trim().toDoubleOrNull()
