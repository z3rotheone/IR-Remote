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
            }.filter { Profiles.byId(it.profileId) != null }
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
}
