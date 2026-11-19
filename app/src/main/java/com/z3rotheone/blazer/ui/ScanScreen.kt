package com.z3rotheone.blazer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.z3rotheone.blazer.Catalog
import com.z3rotheone.blazer.Category
import com.z3rotheone.blazer.IrTransmitter
import com.z3rotheone.blazer.ScanCandidate

@Composable
fun ScanScreen(
    ir: IrTransmitter,
    onBack: () -> Unit,
    onSave: (name: String, profileId: String) -> Unit,
) {
    var category by remember { mutableStateOf(Category.TV) }
    var brand by remember { mutableStateOf<String?>(null) }
    var index by remember { mutableIntStateOf(-1) } // -1 = not scanning
    var noMatch by remember { mutableStateOf(false) }
    var matched by remember { mutableStateOf<ScanCandidate?>(null) }

    // Skip code sets whose test signal is identical to one already tried (e.g. LG and Vizio power).
    val candidates = remember(category, brand) {
        val list = Catalog.candidates.filter { it.category == category && (brand == null || it.brand == brand) }
        list.filterIndexed { i, c -> list.take(i).none { it.testSignal.sameAs(c.testSignal) } }
    }
    val scanning = index >= 0

    fun reset() { index = -1; noMatch = false }

    fun test(i: Int) = ir.send(candidates[i].testSignal)

    fun startScan() {
        noMatch = false
        index = 0
        test(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.backgroundGradient)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Palette.Text)
            }
            Text("Add a remote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        if (!ir.available) {
            Spacer(Modifier.height(12.dp))
            NoBlasterBanner()
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Surface, RoundedCornerShape(16.dp))
                .padding(4.dp),
        ) {
            Category.entries.forEach { c ->
                val selected = c == category
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Palette.SurfaceHi else Color.Transparent)
                        .clickable { category = c; brand = null; reset() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        c.title,
                        color = if (selected) Palette.Text else Palette.TextDim,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Brand", color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf<String?>(null) + Catalog.brands(category)).forEach { b ->
                FilterChip(
                    selected = brand == b,
                    onClick = { brand = b; reset() },
                    label = { Text(b ?: "Any brand") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.Accent,
                        selectedLabelColor = Palette.Bg,
                    ),
                )
            }
        }

        Spacer(Modifier.height(44.dp))
        ScanOrb(scanning = scanning, onClick = { startScan() })
        Spacer(Modifier.height(32.dp))

        when {
            scanning -> {
                val p = candidates[index]
                Text(
                    "Code set ${index + 1} of ${candidates.size}",
                    color = Palette.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (category == Category.AC) "Sent ${p.testLabel}. Did the unit beep or turn on?"
                    else "Sent ${p.testLabel}. Did your device respond?",
                    color = Palette.TextDim, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (index + 1 < candidates.size) { index += 1; test(index) }
                            else { index = -1; noMatch = true }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text("No, try next", color = Palette.Text) }
                    Button(
                        onClick = { matched = p },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.Bg),
                    ) { Text("Yes, it worked", fontWeight = FontWeight.SemiBold) }
                }
                TextButton(onClick = { test(index) }) { Text("Send again", color = Palette.TextDim) }
            }
            noMatch -> {
                Text("No match found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Make sure the IR blaster on top of your phone points at the device, then tap Scan to try again.",
                    color = Palette.TextDim, textAlign = TextAlign.Center,
                )
            }
            category == Category.AC -> {
                Text(
                    "Point the top of your phone at the indoor unit (or AC) and tap Scan. " +
                        "Blazer sends \"on, cool, 24°C\" from each code set — tell it when the unit beeps or turns on.",
                    color = Palette.TextDim, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tip: stand within a few feet of the unit for the best results.",
                    color = Palette.TextDim.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center,
                )
            }
            else -> {
                Text(
                    "Point the top of your phone at your TV or streaming box and tap Scan. " +
                        "Blazer sends a test signal from each code set — just tell it when the device responds.",
                    color = Palette.TextDim, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tip: if your TV turns off, that's a match — tap Yes and turn it back on from your new remote.",
                    color = Palette.TextDim.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    matched?.let { p ->
        // Brands that share this exact test signal (e.g. LG and Vizio) — let the user pick theirs.
        val options = Catalog.candidates.filter { it.category == p.category && it.testSignal.sameAs(p.testSignal) }
        SaveDialog(
            options = options,
            initial = p,
            onDismiss = { matched = null },
            onConfirm = { name, id -> matched = null; onSave(name, id) },
        )
    }
}

@Composable
private fun SaveDialog(
    options: List<ScanCandidate>,
    initial: ScanCandidate,
    onDismiss: () -> Unit,
    onConfirm: (name: String, profileId: String) -> Unit,
) {
    var choice by remember { mutableStateOf(initial) }
    var text by remember { mutableStateOf(initial.name) }
    var edited by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text("Remote found") },
        text = {
            Column {
                if (options.size > 1) {
                    Text("This code works for several brands. Which is yours?", color = Palette.TextDim, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        options.forEach { o ->
                            FilterChip(
                                selected = choice.id == o.id,
                                onClick = { choice = o; if (!edited) text = o.name },
                                label = { Text(o.brand) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Palette.Accent,
                                    selectedLabelColor = Palette.Bg,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; edited = true },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim(), choice.id) }) {
                Text("Save", color = Palette.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextDim) }
        },
    )
}

@Composable
private fun ScanOrb(scanning: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pulse")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        if (scanning) {
            Box(
                Modifier
                    .size(170.dp)
                    .scale(pulse)
                    .border(2.dp, Palette.Accent.copy(alpha = (1.35f - pulse) / 0.35f * 0.6f), CircleShape)
            )
        }
        Box(
            Modifier
                .size(190.dp)
                .border(1.dp, Palette.Outline, CircleShape)
        )
        PressButton(
            onPress = { if (!it) onClick() },
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            background = if (scanning) SolidColor(Palette.SurfaceHi) else Palette.accentGradient,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.SettingsRemote, contentDescription = null,
                    tint = if (scanning) Palette.Accent else Palette.Bg, modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (scanning) "Restart" else "Scan",
                    color = if (scanning) Palette.Text else Palette.Bg,
                    fontWeight = FontWeight.Bold, fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text.trim()) },
            ) { Text(confirmLabel, color = Palette.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextDim) }
        },
    )
}
