package com.mizu.player.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.ViewMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mizu_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val VIDEO_SORT_ORDER = stringPreferencesKey("video_sort_order")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val PLAYER_ENGINE = stringPreferencesKey("player_engine")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val REMEMBER_BRIGHTNESS = booleanPreferencesKey("remember_brightness")
        val REMEMBER_SPEED = booleanPreferencesKey("remember_speed")
        val DEFAULT_SPEED = stringPreferencesKey("default_speed")
        val SWIPE_GESTURE_SENSITIVITY = intPreferencesKey("swipe_sensitivity")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val DARK_MODE = stringPreferencesKey("dark_mode") // system, light, dark
        val AUDIO_PERMISSION_GRANTED = booleanPreferencesKey("audio_permission_granted")
        val SEEK_DURATION = intPreferencesKey("seek_duration")
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val videoSortOrder: Flow<SortOrder> = context.dataStore.data.map {
        try { SortOrder.valueOf(it[Keys.VIDEO_SORT_ORDER] ?: SortOrder.DATE_DESC.name) }
        catch (e: Exception) { SortOrder.DATE_DESC }
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map {
        try { ViewMode.valueOf(it[Keys.VIEW_MODE] ?: ViewMode.GRID.name) }
        catch (e: Exception) { ViewMode.GRID }
    }

    val playerEngine: Flow<PlayerEngine> = context.dataStore.data.map {
        try { PlayerEngine.valueOf(it[Keys.PLAYER_ENGINE] ?: PlayerEngine.AUTO.name) }
        catch (e: Exception) { PlayerEngine.AUTO }
    }

    val autoPlayNext: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.AUTO_PLAY_NEXT] ?: true
    }

    val resumePlayback: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.RESUME_PLAYBACK] ?: true
    }

    val darkMode: Flow<String> = context.dataStore.data.map {
        it[Keys.DARK_MODE] ?: "system"
    }

    val seekDuration: Flow<Int> = context.dataStore.data.map {
        it[Keys.SEEK_DURATION] ?: 10
    }

    val rememberSpeed: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.REMEMBER_SPEED] ?: false
    }

    val defaultSpeed: Flow<Float> = context.dataStore.data.map {
        (it[Keys.DEFAULT_SPEED] ?: "1.0").toFloatOrNull() ?: 1.0f
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setVideoSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.VIDEO_SORT_ORDER] = order.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setPlayerEngine(engine: PlayerEngine) {
        context.dataStore.edit { it[Keys.PLAYER_ENGINE] = engine.name }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_PLAYBACK] = enabled }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setSeekDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.SEEK_DURATION] = seconds.coerceIn(1, 120) }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed.toString() }
    }

    suspend fun setRememberSpeed(remember: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_SPEED] = remember }
    }
}
