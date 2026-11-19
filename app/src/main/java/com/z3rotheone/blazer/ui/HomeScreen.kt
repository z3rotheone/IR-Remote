package com.z3rotheone.blazer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z3rotheone.blazer.Catalog
import com.z3rotheone.blazer.SavedRemote

@Composable
fun HomeScreen(
    remotes: List<SavedRemote>,
    irAvailable: Boolean,
    onOpen: (SavedRemote) -> Unit,
    onAdd: () -> Unit,
    onRename: (SavedRemote, String) -> Unit,
    onDelete: (SavedRemote) -> Unit,
) {
    var renaming by remember { mutableStateOf<SavedRemote?>(null) }
    var deleting by remember { mutableStateOf<SavedRemote?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.backgroundGradient)
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(28.dp))
            Text(
                "Blazer",
                style = TextStyle(brush = Palette.accentGradient, fontSize = 40.sp, fontWeight = FontWeight.Bold),
            )
            Text("IR remote", color = Palette.TextDim, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))
            if (!irAvailable) {
                NoBlasterBanner()
                Spacer(Modifier.height(16.dp))
            }

            if (remotes.isEmpty()) {
                EmptyState(onAdd)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(remotes, key = { it.id }) { r ->
                        RemoteCard(
                            remote = r,
                            onOpen = { onOpen(r) },
                            onRename = { renaming = r },
                            onDelete = { deleting = r },
                        )
                    }
                }
            }
        }

        if (remotes.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = Palette.Accent,
                contentColor = Palette.Bg,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Add remote", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            )
        }
    }

    renaming?.let { r ->
        NameDialog(
            title = "Rename remote",
            initial = r.name,
            confirmLabel = "Rename",
            onDismiss = { renaming = null },
            onConfirm = { name -> renaming = null; onRename(r, name) },
        )
    }

    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Palette.Surface,
            title = { Text("Delete \"${r.name}\"?") },
            confirmButton = {
                TextButton(onClick = { deleting = null; onDelete(r) }) { Text("Delete", color = Palette.Power) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = Palette.TextDim) }
            },
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).background(Palette.Surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.SettingsRemote, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No remotes yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Scan to find the right codes for your TV, streaming box or air conditioner.",
            color = Palette.TextDim, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.Bg),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan for a remote", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun RemoteCard(
    remote: SavedRemote,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val profile = Catalog.byId(remote.profileId)
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).background(Palette.accentGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (profile?.brand ?: remote.name).take(1).uppercase(),
                color = Palette.Bg, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(remote.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Palette.Text)
            Text(profile?.name ?: "", color = Palette.TextDim, fontSize = 13.sp)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = Palette.TextDim)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}
