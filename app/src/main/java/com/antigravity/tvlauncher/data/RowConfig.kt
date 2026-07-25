package com.antigravity.tvlauncher.data

import org.json.JSONArray
import org.json.JSONObject

// ── Row types ─────────────────────────────────────────────────────────────────
enum class RowType {
    FAVORITES,           // User-pinned favorites
    RECOMMENDED,         // Curated / Recommended apps
    MOST_USED,           // Sorted by launchCount desc
    RECENTLY_INSTALLED,  // Installed in last 7 days
    STREAMING,           // Auto-populated from AppCategory.STREAMING
    GAMES,               // Auto-populated from AppCategory.GAME
    CUSTOM               // User-created row with manually added apps
}

/**
 * Describes one row on the home screen.
 * Rows are stored as a JSON array in DataStore to persist order, visibility, collapse state.
 */
data class RowConfig(
    val id: String,
    val title: String,
    val type: RowType,
    val isVisible: Boolean          = true,
    val isCollapsed: Boolean        = false,
    val accentHex: String?          = null, // per-row accent color override
    val customAppPackages: List<String> = emptyList() // for CUSTOM rows only
) {

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("type", type.name)
        put("isVisible", isVisible)
        put("isCollapsed", isCollapsed)
        accentHex?.let { put("accentHex", it) }
        put("customApps", JSONArray(customAppPackages))
    }

    companion object {
        fun fromJson(obj: JSONObject) = RowConfig(
            id          = obj.getString("id"),
            title       = obj.getString("title"),
            type        = RowType.valueOf(obj.getString("type")),
            isVisible   = obj.optBoolean("isVisible", true),
            isCollapsed = obj.optBoolean("isCollapsed", false),
            accentHex   = if (obj.has("accentHex")) obj.getString("accentHex") else null,
            customAppPackages = buildList {
                val arr = obj.optJSONArray("customApps") ?: return@buildList
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        )

        /** Factory — default row configuration for fresh install. */
        fun defaults(): List<RowConfig> = listOf(
            RowConfig("favorites",          "Favorites",         RowType.FAVORITES),
            RowConfig("recommended",        "Recommended Apps",  RowType.RECOMMENDED),
            RowConfig("most_used",          "Most Used",         RowType.MOST_USED),
            RowConfig("streaming",          "Streaming",         RowType.STREAMING),
            RowConfig("games",              "Games",             RowType.GAMES),
            RowConfig("recently_installed", "New Apps",          RowType.RECENTLY_INSTALLED),
        )
    }
}

// ── Extension helpers for serialisation ──────────────────────────────────────

fun List<RowConfig>.toJsonString(): String = JSONArray().also { arr ->
    forEach { arr.put(it.toJson()) }
}.toString()

fun String.toRowConfigs(): List<RowConfig> = try {
    val arr = JSONArray(this)
    (0 until arr.length()).map { RowConfig.fromJson(arr.getJSONObject(it)) }
} catch (_: Exception) {
    RowConfig.defaults()
}
