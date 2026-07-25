package com.antigravity.tvlauncher.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a user profile. Each profile has its own favorites, hidden apps,
 * and an optional kids mode that filters out non-approved content.
 *
 * PINs are stored as plain strings for this prototype; in production, hash them.
 */
data class Profile(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColorHex: String      = "#4FC3F7",
    val isKidsMode: Boolean         = false,
    val pin: String?                = null,    // null = no PIN protection
    val hiddenPackages: Set<String> = emptySet(),
    val favoritePackages: Set<String> = emptySet()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("initials", initials)
        put("avatarColorHex", avatarColorHex)
        put("isKidsMode", isKidsMode)
        pin?.let { put("pin", it) }
        put("hiddenPackages", JSONArray(hiddenPackages.toList()))
        put("favoritePackages", JSONArray(favoritePackages.toList()))
    }

    companion object {
        fun fromJson(obj: JSONObject) = Profile(
            id               = obj.getString("id"),
            name             = obj.getString("name"),
            initials         = obj.getString("initials"),
            avatarColorHex   = obj.optString("avatarColorHex", "#4FC3F7"),
            isKidsMode       = obj.optBoolean("isKidsMode", false),
            pin              = if (obj.has("pin")) obj.getString("pin") else null,
            hiddenPackages   = jsonArrayToSet(obj.optJSONArray("hiddenPackages")),
            favoritePackages = jsonArrayToSet(obj.optJSONArray("favoritePackages"))
        )

        private fun jsonArrayToSet(arr: JSONArray?): Set<String> {
            arr ?: return emptySet()
            return (0 until arr.length()).map { arr.getString(it) }.toSet()
        }

        /** The default profile pre-created on first launch. */
        fun default() = Profile(
            id             = "main",
            name           = "Main",
            initials       = "M",
            avatarColorHex = "#4FC3F7"
        )

        /** A kids-mode profile with warm yellow accent. */
        fun kids() = Profile(
            id             = "kids",
            name           = "Kids",
            initials       = "K",
            avatarColorHex = "#FFD600",
            isKidsMode     = true
        )
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────

fun List<Profile>.toJsonString(): String = JSONArray().also { arr ->
    forEach { arr.put(it.toJson()) }
}.toString()

fun String.toProfiles(): List<Profile> = try {
    val arr = JSONArray(this)
    (0 until arr.length()).map { Profile.fromJson(arr.getJSONObject(it)) }
} catch (_: Exception) {
    listOf(Profile.default(), Profile.kids())
}
