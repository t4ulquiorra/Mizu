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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
}
