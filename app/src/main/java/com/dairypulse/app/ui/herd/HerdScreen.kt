package com.dairypulse.app.ui.herd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.domain.calfCost
import com.dairypulse.app.domain.daysUntilCalving
import com.dairypulse.app.domain.isCalvingSoon
import com.dairypulse.app.ui.common.AlertBanner
import com.dairypulse.app.ui.common.CowAvatar
import com.dairypulse.app.ui.common.CowPhoto
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.LegendDot
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.PageIntro
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SplitBar
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.whole
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope
import java.time.LocalDate

private enum class HerdFilter { ALL, IN_MILK, DRY, SOLD }

@Composable
fun HerdScreen(onOpenCow: (Long) -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.collectAsState().value.currency
    val today = remember { LocalDate.now() }
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    val calfExpenses by remember { app.db.money().calfExpenses() }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf(HerdFilter.ALL) }

    val adults = cows.filter { it.status == CowStatus.IN_MILK || it.status == CowStatus.DRY }
    val young = cows.filter { it.status.isYoungStock }
    val inMilk = adults.count { it.status == CowStatus.IN_MILK }
    val dry = adults.count { it.status == CowStatus.DRY }
    val sold = cows.count { it.status == CowStatus.SOLD }
    val shown = when (filter) {
        HerdFilter.ALL -> adults
        HerdFilter.IN_MILK -> adults.filter { it.status == CowStatus.IN_MILK }
        HerdFilter.DRY -> adults.filter { it.status == CowStatus.DRY }
        HerdFilter.SOLD -> cows.filter { it.status == CowStatus.SOLD }
    }.sortedWith(compareBy<Cow> { !isCalvingSoon(it, today) }.thenBy { it.name.lowercase() })
    val dueSoon = adults.count { isCalvingSoon(it, today) }
    val byId = cows.associateBy { it.id }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PageIntro("Your animals", "Herd", "${adults.size} cows · $inMilk milking · $dry dry · ${young.size} young stock")

            Segmented(HerdFilter.entries, filter, {
                when (it) {
                    HerdFilter.ALL -> "All ${adults.size}"
                    HerdFilter.IN_MILK -> "Milking $inMilk"
                    HerdFilter.DRY -> "Dry $dry"
                    HerdFilter.SOLD -> "Sold $sold"
                }
            }) { filter = it }

            if (dueSoon > 0 && filter != HerdFilter.SOLD) {
                AlertBanner("$dueSoon ${if (dueSoon == 1) "cow is" else "cows are"} due to calve within 21 days. Prepare the calving pen.")
            }
            if (cows.isEmpty()) Muted("No animals yet. Tap \"Add animal\" to register your cows, heifers and calves.", size = 15)

            // Two photo cards per row; both cards in a row share the taller one's height.
            shown.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { cow -> CowCard(cow, today, Modifier.weight(1f).fillMaxHeight()) { onOpenCow(cow.id) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (filter != HerdFilter.SOLD && young.isNotEmpty()) {
                SectionHeader("Calves & young stock")
                young.forEach { calf ->
                    val cost = calfCost(calf.id, calfExpenses)
                    DpCard(onClick = { onOpenCow(calf.id) }) {
                        Row {
                            CowAvatar(calf.photo, calf.name, 48.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Serif(calf.name, 19)
                                    if (calf.tag.isNotBlank()) Muted("  #${calf.tag}")
                                }
                                Muted(
                                    listOfNotNull(
                                        (if (calf.isFemale) "Heifer" else "Bull") + if (calf.status == CowStatus.CALF) " calf" else "",
                                        calf.damId?.let(byId::get)?.let { "Dam: ${it.name}" },
                                        calf.birthDate?.let { "Born ${it.pretty()}" },
                                    ).joinToString(" · "),
                                    Modifier.padding(top = 2.dp),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Serif(whole(cost.total), 19)
                                Muted("$cur to date", size = 12)
                            }
                        }
                        if (cost.total > 0) {
                            SplitBar(listOf(cost.feed.toFloat() to Dp.OchreBar, cost.upkeep.toFloat() to Dp.SlateBar), Modifier.padding(top = 12.dp, bottom = 8.dp), height = 8.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                LegendDot("Feed ${whole(cost.feed)}", Dp.OchreBar)
                                LegendDot("Upkeep ${whole(cost.upkeep)}", Dp.SlateBar)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { onOpenCow(0) },
            icon = { Icon(Icons.Outlined.Add, null) },
            text = { Strong("Add animal", color = Color.White) },
            containerColor = Dp.Green, contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** Photo card: square picture, then the name and "tag · status", two to a row. */
@Composable
private fun CowCard(cow: Cow, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val soon = isCalvingSoon(cow, today)
    val shape = RoundedCornerShape(22.dp)
    val status = when {
        soon -> daysUntilCalving(cow, today)!!.let { if (it < 0) "Calving overdue" else "Calving in $it days" }
        cow.status == CowStatus.IN_MILK -> "In milk"
        else -> cow.status.label
    }
    Column(
        modifier.clip(shape).background(Dp.Card).border(1.dp, if (soon) Dp.ClayBorder else Dp.Border, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(14.dp),
    ) {
        CowPhoto(
            cow.photo, cow.name, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(16.dp),
            maxPx = 512, initialSize = 52, muted = cow.status != CowStatus.IN_MILK,
        )
        Text(
            cow.name, Modifier.padding(top = 14.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = Manrope, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Dp.Ink),
        )
        Text(
            listOfNotNull(cow.tag.takeIf { it.isNotBlank() }, status).joinToString(" · "),
            Modifier.padding(top = 4.dp),
            style = TextStyle(
                fontFamily = Manrope, fontSize = 15.sp, lineHeight = 20.sp,
                fontWeight = if (soon) FontWeight.Bold else FontWeight.Medium, color = if (soon) Dp.Clay else Dp.Muted,
            ),
        )
    }
}
