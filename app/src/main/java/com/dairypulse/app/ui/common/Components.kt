package com.dairypulse.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp as DpUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope
import com.dairypulse.app.ui.theme.serif
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------- Text ----------

/** Small uppercase, letter-spaced label ("TODAY'S NET PROFIT"). */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = Dp.Muted, size: Int = 11) {
    Text(
        text.uppercase(), modifier,
        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = size.sp, letterSpacing = 0.14.em, color = color),
    )
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, size: Int = 13, color: Color = Dp.Muted, weight: FontWeight = FontWeight.Medium) {
    Text(text, modifier, style = TextStyle(fontFamily = Manrope, fontSize = size.sp, fontWeight = weight, color = color, lineHeight = (size * 1.45).sp))
}

@Composable
fun Strong(text: String, modifier: Modifier = Modifier, size: Int = 15, color: Color = Dp.Ink, weight: FontWeight = FontWeight.Bold) {
    Text(text, modifier, style = TextStyle(fontFamily = Manrope, fontSize = size.sp, fontWeight = weight, color = color))
}

@Composable
fun Serif(text: String, size: Int, modifier: Modifier = Modifier, color: Color = Dp.Heading, align: TextAlign? = null) {
    Text(text, modifier, style = serif(size, color), textAlign = align)
}

/** Eyebrow label, big serif title and a muted line, at the top of each tab. */
@Composable
fun PageIntro(eyebrow: String, title: String, subtitle: String? = null) {
    Column {
        Label(eyebrow)
        Serif(title, 32, Modifier.padding(top = 6.dp, bottom = 4.dp))
        if (subtitle != null) Muted(subtitle, size = 15)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Serif(title, 22, Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Kept for older call sites. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = SectionHeader(text, modifier)

// ---------- Surfaces ----------

@Composable
fun DpCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: Color = Dp.Border,
    background: Color = Dp.Card,
    radius: DpUnit = 22.dp,
    padding: DpUnit = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier.fillMaxWidth().clip(shape).background(background).border(1.dp, border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

/** Kept for older call sites. */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = DpCard(modifier, onClick, border = accent ?: Dp.Border, content = content)

@Composable
fun RowDivider() = HorizontalDivider(color = Dp.Divider, thickness = 1.dp)

/** Green gradient hero card (turns clay red when [loss] is true). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, loss: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(if (loss) Dp.lossGradient else Dp.heroGradient)
            .padding(22.dp),
        content = content,
    )
}

/** Figure tile: label, big serif value in an accent colour, small caption. */
@Composable
fun StatTile(label: String, value: String, valueColor: Color, caption: String, modifier: Modifier = Modifier, valueSize: Int = 30) {
    DpCard(modifier, padding = 16.dp) {
        Label(label)
        Serif(value, valueSize, Modifier.padding(top = 8.dp, bottom = 2.dp), color = valueColor)
        Muted(caption)
    }
}

@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)

@Composable
fun Badge(text: String, background: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        text, modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 9.dp, vertical = 3.dp),
        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color),
    )
}

@Composable
fun Avatar(name: String, size: DpUnit = 48.dp, background: Color = Dp.GreenPill, color: Color = Dp.GreenDark) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Serif(name.take(1).uppercase().ifEmpty { "?" }, (size.value * 0.42f).toInt(), color = color)
    }
}

@Composable
fun AlertBanner(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    var m = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Dp.ClayTint)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(m.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.WarningAmber, null, tint = Dp.ClayDark)
        Muted(text, size = 14, color = Dp.ClayDark, weight = FontWeight.SemiBold)
    }
}

/** Horizontal stacked bar (e.g. ration make-up, feed vs upkeep). */
@Composable
fun SplitBar(parts: List<Pair<Float, Color>>, modifier: Modifier = Modifier, height: DpUnit = 10.dp) {
    val total = parts.sumOf { it.first.toDouble() }.toFloat().takeIf { it > 0 } ?: return
    Row(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.filter { it.first > 0 }.forEach { (v, c) -> Box(Modifier.weight(v / total).height(height).background(c)) }
    }
}

@Composable
fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Muted(text, size = 12)
    }
}

val BarColors = listOf(Dp.Green, Dp.OchreBar, Dp.SlateBar, Dp.GreenSoft, Color(0xFFC9A27A), Color(0xFF9C8FB8))

// ---------- Buttons ----------

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) =
    DpButton(text, onClick, modifier, icon, enabled, if (enabled) Dp.Green else Dp.InputBorder, Color.White)

@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) =
    DpButton(text, onClick, modifier, icon, enabled, Dp.GreenTint, Dp.GreenDark)

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Dp.Neutral, icon: ImageVector? = null) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp),
        color = Color.Transparent, border = BorderStroke(1.5.dp, Color(0xFFD9CFBC)),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Strong(text, color = color, weight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DpButton(text: String, onClick: () -> Unit, modifier: Modifier, icon: ImageVector?, enabled: Boolean, bg: Color, fg: Color) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), color = bg) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Strong(text, size = 16, color = fg, weight = FontWeight.ExtraBold)
        }
    }
}

/** Small green pill used for "+ Add" next to a section title. */
@Composable
fun AddPill(text: String, onClick: () -> Unit, icon: ImageVector) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Dp.GreenTint, modifier = Modifier.heightIn(min = 40.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Dp.GreenDark, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Strong(text, size = 14, color = Dp.GreenDark)
        }
    }
}

@Composable
fun DangerTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 44.dp)) {
        Icon(Icons.Outlined.Delete, null, tint = Dp.Clay, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Strong(text, size = 14, color = Dp.Clay)
    }
}

/** Pill-shaped segmented switch (Today / 7 days / 30 days, sessions, filters). */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Dp.Card)
            .border(1.dp, Dp.Border, RoundedCornerShape(20.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(15.dp))
                    .background(if (on) Dp.Green else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o), maxLines = 1, textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (on) Color.White else Dp.Neutral),
                )
            }
        }
    }
}

/** Kept for older call sites: renders as a segmented switch. */
@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) =
    Segmented(options, selected, label, onSelect = onSelect)

// ---------- Fields ----------

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = Color.White,
    focusedBorderColor = Dp.Green, unfocusedBorderColor = Dp.InputBorder, disabledBorderColor = Dp.InputBorder,
    disabledTextColor = Dp.Ink, disabledTrailingIconColor = Dp.Muted,
)

private val fieldText = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Dp.Ink)

@Composable
private fun FieldLabel(text: String) =
    Text(text, Modifier.padding(bottom = 6.dp), style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Dp.Neutral))

/** Text field with its label above, as in the design. The label is also the field's accessible name. */
@Composable
fun TextInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, minLines: Int = 1) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        OutlinedTextField(
            value = value, onValueChange = onValueChange, singleLine = minLines == 1, minLines = minLines,
            shape = RoundedCornerShape(16.dp), colors = fieldColors(), textStyle = fieldText,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, suffix: String? = null) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
            singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors(), textStyle = fieldText,
            suffix = suffix?.let { { Strong(it, size = 14, color = Dp.Muted) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

/** Compact numeric box without a label above (per-cow litres, ration kg). */
@Composable
fun CompactNumber(value: String, onValueChange: (String) -> Unit, description: String, suffix: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
        singleLine = true, shape = RoundedCornerShape(14.dp), colors = fieldColors(),
        textStyle = fieldText.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
        placeholder = { Text("0", Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = fieldText.copy(color = Dp.Muted)) },
        suffix = { Strong(" $suffix", size = 14, color = Dp.Muted) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun PickerField(label: String, value: String, trailing: ImageVector, placeholder: String, modifier: Modifier, onOpen: () -> Unit) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        Box {
            OutlinedTextField(
                value = value, onValueChange = {}, readOnly = true, enabled = false, singleLine = true,
                shape = RoundedCornerShape(16.dp), colors = fieldColors(), textStyle = fieldText,
                placeholder = { Text(placeholder, style = fieldText.copy(color = Dp.Muted)) },
                trailingIcon = { Icon(trailing, null) },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClickLabel = label, onClick = onOpen))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    placeholder: String = "Pick a date",
) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, date?.pretty() ?: "", Icons.Outlined.CalendarMonth, placeholder, modifier) { open = true }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (allowClear) TextButton(onClick = { onDateChange(null); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        ) { DatePicker(state) }
    }
}

@Composable
fun <T> Dropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        PickerField(label, selected?.let(optionLabel) ?: "", Icons.Outlined.ExpandMore, "Choose…", Modifier) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}

// ---------- Sheets ----------

/**
 * Form shown as a sheet sliding up from the bottom, with Cancel / Save and an optional Delete.
 * (Name kept from the dialog version so call sites stay the same.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    eyebrow: String? = null,
    saveLabel: String = "Save",
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Dp.NavBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                if (eyebrow != null) Label(eyebrow)
                Serif(title, 24, Modifier.padding(top = if (eyebrow != null) 4.dp else 0.dp))
            }
            content()
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("Cancel", onDismiss, Modifier.weight(1f))
                PrimaryButton(saveLabel, onSave, Modifier.weight(1f), enabled = saveEnabled)
            }
            if (onDelete != null) DangerTextButton("Delete", onDelete, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
