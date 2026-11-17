package dev.anilbeesetti.nextplayer.settings.backup

import android.content.Context
import android.net.Uri
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.ui.R
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single

@Single
class SettingsBackupService(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) {
    private val codec = SettingsBackupCodec(
        subtitleEncodings = context.resources.getStringArray(R.array.charsets_list).map {
            it.substringAfterLast(
                delimiter = "(",
                missingDelimiterValue = "",
            )
                .substringBeforeLast(delimiter = ")")
        }.toSet(),
    )

    suspend fun exportSettings(uri: Uri, includeNetworkConnections: Boolean = false): Unit =
        withContext(Dispatchers.IO) {
            val bytes = codec.encode(
                applicationPreferences = preferencesRepository.applicationPreferences.value,
                playerPreferences = preferencesRepository.playerPreferences.value,
                networkConnections = null,
            ).toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                output.write(bytes)
            } ?: error("Unable to open output stream for $uri")
        }

    suspend fun readSettings(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream for $uri")
        val content = stream.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val output = ByteArrayOutputStream()
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_BACKUP_BYTES)
                output.write(buffer, 0, count)
            }
            output.toByteArray().decodeToString(throwOnInvalidSequence = true)
                .removePrefix("\uFEFF")
        }
        codec.parse(content)
        content
    }

    suspend fun importSettings(content: String): Unit = withContext(Dispatchers.IO) {
        val imported = codec.decode(
            content = codec.parse(content),
            applicationPreferences = preferencesRepository.applicationPreferences.value,
            playerPreferences = preferencesRepository.playerPreferences.value,
            networkConnections = null,
        )
        preferencesRepository.updateApplicationPreferences { imported.applicationPreferences }
        preferencesRepository.updatePlayerPreferences { imported.playerPreferences }
        check(
            withTimeoutOrNull(5_000) {
                preferencesRepository.applicationPreferences.first {
                    it == imported.applicationPreferences
                }
                preferencesRepository.playerPreferences.first { it == imported.playerPreferences }
                true
            } == true,
        )
    }

    companion object {
        const val MAX_BACKUP_BYTES: Int = 1_000_000
    }
}
