package com.dairypulse.app.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** "KSh 14,250" – whole amounts by default; pass decimals = 2 for rates like price per kg. */
fun money(currency: String, amount: Double, decimals: Int = 0): String {
    val sign = if (amount < 0) "−" else ""
    return "$sign$currency " + String.format(Locale.US, "%,.${decimals}f", abs(amount))
}

/** Price per unit, e.g. "KSh 42.00". */
fun rate(currency: String, amount: Double): String = money(currency, amount, 2)

/** Plain grouped number without currency, e.g. "41,500". */
fun whole(amount: Double): String {
    val sign = if (amount < 0) "−" else ""
    return sign + String.format(Locale.US, "%,.0f", abs(amount))
}

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
