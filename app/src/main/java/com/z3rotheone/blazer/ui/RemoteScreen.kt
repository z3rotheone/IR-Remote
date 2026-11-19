package com.z3rotheone.blazer.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z3rotheone.blazer.IrTransmitter
import com.z3rotheone.blazer.Key
import com.z3rotheone.blazer.RemoteProfile
import com.z3rotheone.blazer.SavedRemote

@Composable
fun RemoteScreen(
    remote: SavedRemote,
    profile: RemoteProfile,
    ir: IrTransmitter,
    onBack: () -> Unit,
) {
    val send: (Key, Boolean) -> Unit = { key, isRepeat ->
        profile.keys[key]?.let { ir.send(it, droppable = isRepeat) }
    }
    fun has(key: Key) = profile.keys.containsKey(key)

    Column(
        modifier = Modifier
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
        }

        if (!ir.available) {
            Spacer(Modifier.height(12.dp))
            NoBlasterBanner()
        }

        Spacer(Modifier.height(28.dp))

        // Mute · Power · Source
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundKey(Icons.AutoMirrored.Rounded.VolumeOff, profile.label(Key.MUTE), has(Key.MUTE)) { send(Key.MUTE, it) }
            PressButton(
                onPress = { send(Key.POWER, it) },
                modifier = Modifier.size(84.dp),
                shape = CircleShape,
                background = SolidColor(Palette.Power),
                enabled = has(Key.POWER),
            ) {
                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Power", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            RoundKey(Icons.AutoMirrored.Rounded.Input, profile.label(Key.SOURCE), has(Key.SOURCE)) { send(Key.SOURCE, it) }
        }

        Spacer(Modifier.height(32.dp))
        DPad(profile, send)
        Spacer(Modifier.height(28.dp))

        // Back · Home · Menu
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillKey(Icons.Rounded.Undo, profile.label(Key.BACK), has(Key.BACK), Modifier.weight(1f)) { send(Key.BACK, it) }
            PillKey(Icons.Rounded.Home, profile.label(Key.HOME), has(Key.HOME), Modifier.weight(1f)) { send(Key.HOME, it) }
            PillKey(Icons.Rounded.Menu, profile.label(Key.MENU), has(Key.MENU), Modifier.weight(1f)) { send(Key.MENU, it) }
        }

        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Rocker("VOL", has(Key.VOL_UP), { send(Key.VOL_UP, it) }, { send(Key.VOL_DOWN, it) })
            Rocker("CH", has(Key.CH_UP), { send(Key.CH_UP, it) }, { send(Key.CH_DOWN, it) })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NoBlasterBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Palette.Power.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            "No IR blaster detected on this phone — buttons won't send anything.",
            color = Palette.Power,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RoundKey(icon: ImageVector, label: String, enabled: Boolean, onPress: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PressButton(onPress = onPress, modifier = Modifier.size(62.dp), shape = CircleShape, enabled = enabled) {
            Icon(icon, contentDescription = label, tint = Palette.Text)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = Palette.TextDim)
    }
}

@Composable
private fun PillKey(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, onPress: (Boolean) -> Unit) {
    PressButton(onPress = onPress, modifier = modifier.height(54.dp), shape = RoundedCornerShape(27.dp), enabled = enabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Palette.Text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, color = Palette.Text, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DPad(profile: RemoteProfile, send: (Key, Boolean) -> Unit) {
    val arrow = Modifier.size(76.dp)
    val transparent = SolidColor(Color.Transparent)
    Box(
        Modifier
            .size(248.dp)
            .background(Palette.Surface, CircleShape)
    ) {
        PressButton({ send(Key.UP, it) }, arrow.align(Alignment.TopCenter).padding(top = 4.dp), CircleShape, transparent, enabled = profile.keys.containsKey(Key.UP), repeat = true) {
            Icon(Icons.Rounded.KeyboardArrowUp, "Up", tint = Palette.Text, modifier = Modifier.size(34.dp))
        }
        PressButton({ send(Key.DOWN, it) }, arrow.align(Alignment.BottomCenter).padding(bottom = 4.dp), CircleShape, transparent, enabled = profile.keys.containsKey(Key.DOWN), repeat = true) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Down", tint = Palette.Text, modifier = Modifier.size(34.dp))
        }
        PressButton({ send(Key.LEFT, it) }, arrow.align(Alignment.CenterStart).padding(start = 4.dp), CircleShape, transparent, enabled = profile.keys.containsKey(Key.LEFT), repeat = true) {
            Icon(Icons.Rounded.KeyboardArrowLeft, "Left", tint = Palette.Text, modifier = Modifier.size(34.dp))
        }
        PressButton({ send(Key.RIGHT, it) }, arrow.align(Alignment.CenterEnd).padding(end = 4.dp), CircleShape, transparent, enabled = profile.keys.containsKey(Key.RIGHT), repeat = true) {
            Icon(Icons.Rounded.KeyboardArrowRight, "Right", tint = Palette.Text, modifier = Modifier.size(34.dp))
        }
        PressButton({ send(Key.OK, it) }, Modifier.size(96.dp).align(Alignment.Center), CircleShape, Palette.accentGradient, enabled = profile.keys.containsKey(Key.OK)) {
            Text("OK", color = Palette.Bg, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun Rocker(label: String, enabled: Boolean, onUp: (Boolean) -> Unit, onDown: (Boolean) -> Unit) {
    val transparent = SolidColor(Color.Transparent)
    Column(
        Modifier
            .width(88.dp)
            .background(Palette.Surface, RoundedCornerShape(44.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PressButton(onUp, Modifier.width(88.dp).height(76.dp), RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp), transparent, enabled = enabled, repeat = true) {
            Icon(Icons.Rounded.Add, "$label up", tint = Palette.Text, modifier = Modifier.size(30.dp))
        }
        Text(label, fontSize = 13.sp, color = Palette.TextDim, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        PressButton(onDown, Modifier.width(88.dp).height(76.dp), RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp), transparent, enabled = enabled, repeat = true) {
            Icon(Icons.Rounded.Remove, "$label down", tint = Palette.Text, modifier = Modifier.size(30.dp))
        }
    }
}
