package com.z3rotheone.blazer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z3rotheone.blazer.AcFan
import com.z3rotheone.blazer.AcMode
import com.z3rotheone.blazer.AcProfile
import com.z3rotheone.blazer.AcState
import com.z3rotheone.blazer.IrTransmitter
import com.z3rotheone.blazer.RemoteStore
import com.z3rotheone.blazer.SavedRemote
import kotlin.math.roundToInt

@Composable
fun AcScreen(
    remote: SavedRemote,
    profile: AcProfile,
    ir: IrTransmitter,
    store: RemoteStore,
    onBack: () -> Unit,
) {
    var state by remember { mutableStateOf(store.loadAc(remote.id).let { it.copy(temp = it.temp.coerceIn(profile.minTemp, profile.maxTemp)) }) }
    var fahrenheit by remember { mutableStateOf(store.useFahrenheit) }

    // Like a real AC remote, every change sends the full state — but only while the unit is on.
    fun apply(new: AcState, alwaysSend: Boolean = false) {
        state = new
        store.saveAc(remote.id, new)
        if (new.power || alwaysSend) ir.send(profile.encode(new))
    }

    val tempEnabled = state.mode != AcMode.FAN
    val ringColor by animateColorAsState(
        when {
            !state.power -> Palette.Outline
            state.mode == AcMode.HEAT -> Palette.Accent
            else -> Color(0xFF5CC8FF)
        },
        label = "ring",
    )

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
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(remote.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(profile.name, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
            }
            TextButton(onClick = { fahrenheit = !fahrenheit; store.useFahrenheit = fahrenheit }) {
                Text(if (fahrenheit) "°F" else "°C", color = Palette.Accent, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        if (!ir.available) {
            Spacer(Modifier.height(12.dp))
            NoBlasterBanner()
        }

        Spacer(Modifier.height(28.dp))

        // Temperature dial
        Box(
            Modifier
                .size(240.dp)
                .border(3.dp, ringColor, CircleShape)
                .padding(10.dp)
                .background(Palette.Surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (state.power) state.mode.label.uppercase() else "OFF",
                    color = if (state.power) ringColor else Palette.TextDim,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                )
                val shown = if (fahrenheit) (state.temp * 9 / 5f + 32).roundToInt() else state.temp
                Text(
                    if (tempEnabled) "$shown°" else "—",
                    color = if (state.power) Palette.Text else Palette.TextDim,
                    fontSize = 76.sp, fontWeight = FontWeight.Light,
                )
                Text("Fan ${state.fan.label}", color = Palette.TextDim, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PressButton(
                onPress = { if (!it) apply(state.copy(temp = state.temp - 1)) },
                modifier = Modifier.size(68.dp), shape = CircleShape,
                enabled = tempEnabled && state.temp > profile.minTemp,
            ) { Icon(Icons.Rounded.Remove, "Cooler", tint = Palette.Text, modifier = Modifier.size(30.dp)) }

            PressButton(
                onPress = { if (!it) apply(state.copy(power = !state.power), alwaysSend = true) },
                modifier = Modifier.size(84.dp), shape = CircleShape,
                background = SolidColor(if (state.power) Palette.Power else Palette.SurfaceHi),
            ) {
                Icon(
                    Icons.Rounded.PowerSettingsNew, "Power",
                    tint = if (state.power) Color.White else Palette.Power, modifier = Modifier.size(38.dp),
                )
            }

            PressButton(
                onPress = { if (!it) apply(state.copy(temp = state.temp + 1)) },
                modifier = Modifier.size(68.dp), shape = CircleShape,
                enabled = tempEnabled && state.temp < profile.maxTemp,
            ) { Icon(Icons.Rounded.Add, "Warmer", tint = Palette.Text, modifier = Modifier.size(30.dp)) }
        }

        Spacer(Modifier.height(32.dp))
        SectionLabel("Mode")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val icons = mapOf(
                AcMode.COOL to Icons.Rounded.AcUnit, AcMode.HEAT to Icons.Rounded.WbSunny,
                AcMode.DRY to Icons.Rounded.WaterDrop, AcMode.FAN to Icons.Rounded.Air,
                AcMode.AUTO to Icons.Rounded.Autorenew,
            )
            AcMode.entries.forEach { m ->
                Choice(m.label, icons.getValue(m), state.mode == m, Modifier.weight(1f)) {
                    apply(state.copy(mode = m))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Fan speed")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcFan.entries.forEach { f ->
                Choice(f.label, null, state.fan == f, Modifier.weight(1f)) { apply(state.copy(fan = f)) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            if (state.power) "Each change is sent to the unit right away."
            else "Turn the unit on to send changes.",
            color = Palette.TextDim.copy(alpha = 0.7f), fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = Palette.TextDim, fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

@Composable
private fun Choice(label: String, icon: ImageVector?, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val fg = if (selected) Palette.Bg else Palette.Text
    PressButton(
        onPress = { if (!it) onSelect() },
        modifier = modifier.height(if (icon != null) 68.dp else 46.dp),
        shape = RoundedCornerShape(16.dp),
        background = if (selected) Palette.accentGradient else SolidColor(Palette.Surface),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
            }
            Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
