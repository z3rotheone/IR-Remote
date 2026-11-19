package com.z3rotheone.blazer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedRemote(val id: String, val name: String, val profileId: String)

/** Saves the user's remotes on the phone. */
class RemoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("blazer", Context.MODE_PRIVATE)

    fun load(): List<SavedRemote> {
        val raw = prefs.getString("remotes", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                SavedRemote(o.getString("id"), o.getString("name"), o.getString("profile"))
            }.filter { Catalog.isKnown(it.profileId) }
        }.getOrDefault(emptyList())
    }

    fun save(remotes: List<SavedRemote>) {
        val arr = JSONArray()
        remotes.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("profile", it.profileId))
        }
        prefs.edit().putString("remotes", arr.toString()).apply()
    }

    fun newRemote(name: String, profileId: String) =
        SavedRemote(UUID.randomUUID().toString(), name, profileId)

    var lastOpened: String?
        get() = prefs.getString("last", null)
        set(value) = prefs.edit().putString("last", value).apply()

    /** Last state sent to an air conditioner remote. */
    fun loadAc(remoteId: String): AcState {
        val raw = prefs.getString("ac_$remoteId", null) ?: return AcState()
        return runCatching {
            val o = JSONObject(raw)
            AcState(
                power = o.getBoolean("power"),
                mode = AcMode.valueOf(o.getString("mode")),
                temp = o.getInt("temp"),
                fan = AcFan.valueOf(o.getString("fan")),
            )
        }.getOrDefault(AcState())
    }

    fun saveAc(remoteId: String, s: AcState) {
        val o = JSONObject().put("power", s.power).put("mode", s.mode.name)
            .put("temp", s.temp).put("fan", s.fan.name)
        prefs.edit().putString("ac_$remoteId", o.toString()).apply()
    }

    var useFahrenheit: Boolean
        get() = prefs.getBoolean("fahrenheit", true)
        set(value) = prefs.edit().putBoolean("fahrenheit", value).apply()
}
