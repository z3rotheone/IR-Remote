package com.z3rotheone.blazer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.z3rotheone.blazer.ui.AcScreen
import com.z3rotheone.blazer.ui.BlazerTheme
import com.z3rotheone.blazer.ui.HomeScreen
import com.z3rotheone.blazer.ui.RemoteScreen
import com.z3rotheone.blazer.ui.ScanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val ir = IrTransmitter(this)
        val store = RemoteStore(this)
        setContent {
            BlazerTheme { BlazerApp(ir, store) }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Scan : Screen
    data class Remote(val id: String) : Screen
}

@Composable
private fun BlazerApp(ir: IrTransmitter, store: RemoteStore) {
    var remotes by remember { mutableStateOf(store.load()) }
    var screen by remember {
        // Re-open the last remote used, so Blazer starts ready to go.
        val last = store.lastOpened
        mutableStateOf<Screen>(
            if (last != null && remotes.any { it.id == last }) Screen.Remote(last) else Screen.Home
        )
    }

    fun update(list: List<SavedRemote>) {
        remotes = list
        store.save(list)
    }

    fun open(id: String) {
        store.lastOpened = id
        screen = Screen.Remote(id)
    }

    fun goHome() {
        if (screen is Screen.Remote) store.lastOpened = null
        screen = Screen.Home
    }

    BackHandler(enabled = screen != Screen.Home) { goHome() }

    Crossfade(targetState = screen, label = "screen") { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                remotes = remotes,
                irAvailable = ir.available,
                onOpen = { open(it.id) },
                onAdd = { screen = Screen.Scan },
                onRename = { r, name -> update(remotes.map { if (it.id == r.id) it.copy(name = name) else it }) },
                onDelete = { r -> update(remotes.filterNot { it.id == r.id }) },
            )
            Screen.Scan -> ScanScreen(
                ir = ir,
                onBack = { goHome() },
                onSave = { name, profileId ->
                    val r = store.newRemote(name, profileId)
                    update(remotes + r)
                    open(r.id)
                },
            )
            is Screen.Remote -> {
                val remote = remotes.firstOrNull { it.id == s.id }
                val tv = remote?.let { Profiles.byId(it.profileId) }
                val ac = remote?.let { AcProfiles.byId(it.profileId) }
                if (remote != null && tv != null) {
                    RemoteScreen(remote, tv, ir, onBack = { goHome() })
                } else if (remote != null && ac != null) {
                    AcScreen(remote, ac, ir, store, onBack = { goHome() })
                } else {
                    LaunchedEffect(s) { goHome() }
                }
            }
        }
    }
}
