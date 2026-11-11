package com.mizu.player.data

import android.content.Context
import android.content.SharedPreferences
import com.mizu.player.model.SortOption

/**
 * Persistent user settings manager using Android SharedPreferences.
 * Ensures view mode (List vs Grid), sort orders, and active tabs survive app process death.
 */
object UserPreferences {
    private const val PREFS_NAME = "mizu_user_prefs"
    private const val KEY_IS_GRID_VIEW = "is_grid_view"
    private const val KEY_SORT_OPTION = "sort_option"
    private const val KEY_SELECTED_TAB = "selected_tab"
    private const val PREFIX_PLAYBACK_POS = "pos_"
    private const val PREFIX_AUDIO_TRACK = "audio_track_"
    private const val PREFIX_SUB_TRACK = "sub_track_"
    private const val KEY_PREFERRED_AUDIO_LANGS = "pref_audio_langs"
    private const val KEY_PREFERRED_SUB_LANGS = "pref_sub_langs"
    private const val KEY_SUBTITLES_OFF_BY_DEFAULT = "pref_subs_off_by_default"

    const val SUBTITLE_OFF = "OFF"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getVideoKey(video: com.mizu.player.model.VideoItem): String {
        return if (video.id > 0) video.id.toString() else video.path.ifEmpty { video.uri.toString() }
    }

    fun getPlaybackPosition(context: Context, key: String): Long {
        return getPrefs(context).getLong(PREFIX_PLAYBACK_POS + key, 0L)
    }

    fun setPlaybackPosition(context: Context, key: String, positionMs: Long) {
        if (positionMs <= 0L) {
            getPrefs(context).edit().remove(PREFIX_PLAYBACK_POS + key).apply()
        } else {
            getPrefs(context).edit().putLong(PREFIX_PLAYBACK_POS + key, positionMs).apply()
        }
    }

    fun getAllPlaybackPositions(context: Context): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val all = getPrefs(context).all
        for ((k, v) in all) {
            if (k.startsWith(PREFIX_PLAYBACK_POS) && v is Long) {
                result[k.removePrefix(PREFIX_PLAYBACK_POS)] = v
            }
        }
        return result
    }

    fun isGridView(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_GRID_VIEW, true)
    }

    fun setGridView(context: Context, isGrid: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_GRID_VIEW, isGrid).apply()
    }

    fun getSortOption(context: Context): SortOption {
        val name = getPrefs(context).getString(KEY_SORT_OPTION, SortOption.DATE_DESC.name)
        return try {
            SortOption.valueOf(name ?: SortOption.DATE_DESC.name)
        } catch (_: Exception) {
            SortOption.DATE_DESC
        }
    }

    fun setSortOption(context: Context, option: SortOption) {
        getPrefs(context).edit().putString(KEY_SORT_OPTION, option.name).apply()
    }

    fun getSelectedTab(context: Context): Int {
        return getPrefs(context).getInt(KEY_SELECTED_TAB, 0)
    }

    fun setSelectedTab(context: Context, tab: Int) {
        getPrefs(context).edit().putInt(KEY_SELECTED_TAB, tab).apply()
    }

    // Per-video track selections
    fun getSavedAudioTrack(context: Context, videoKey: String): String? {
        return getPrefs(context).getString(PREFIX_AUDIO_TRACK + videoKey, null)
    }

    fun setSavedAudioTrack(context: Context, videoKey: String, trackIdOrLang: String?) {
        val editor = getPrefs(context).edit()
        if (trackIdOrLang == null) {
            editor.remove(PREFIX_AUDIO_TRACK + videoKey)
        } else {
            editor.putString(PREFIX_AUDIO_TRACK + videoKey, trackIdOrLang)
        }
        editor.apply()
    }

    fun getSavedSubtitleTrack(context: Context, videoKey: String): String? {
        return getPrefs(context).getString(PREFIX_SUB_TRACK + videoKey, null)
    }

    fun setSavedSubtitleTrack(context: Context, videoKey: String, trackIdOrLang: String?) {
        val editor = getPrefs(context).edit()
        if (trackIdOrLang == null) {
            editor.remove(PREFIX_SUB_TRACK + videoKey)
        } else {
            editor.putString(PREFIX_SUB_TRACK + videoKey, trackIdOrLang)
        }
        editor.apply()
    }

    // Language Priorities
    fun getPreferredAudioLanguages(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_PREFERRED_AUDIO_LANGS, "ja,en") ?: "ja,en"
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setPreferredAudioLanguages(context: Context, langs: List<String>) {
        val raw = langs.joinToString(",")
        getPrefs(context).edit().putString(KEY_PREFERRED_AUDIO_LANGS, raw).apply()
    }

    fun getPreferredSubtitleLanguages(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_PREFERRED_SUB_LANGS, "en") ?: "en"
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setPreferredSubtitleLanguages(context: Context, langs: List<String>) {
        val raw = langs.joinToString(",")
        getPrefs(context).edit().putString(KEY_PREFERRED_SUB_LANGS, raw).apply()
    }

    fun isSubtitlesOffByDefault(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SUBTITLES_OFF_BY_DEFAULT, false)
    }

    fun setSubtitlesOffByDefault(context: Context, off: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SUBTITLES_OFF_BY_DEFAULT, off).apply()
    }
}
