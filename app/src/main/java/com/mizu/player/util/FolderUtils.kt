package com.mizu.player.util

object FolderUtils {

    private const val PRIMARY_STORAGE = "/storage/emulated/0"

    /**
     * Sanitizes full storage path to user-friendly label.
     * E.g. /storage/emulated/0/Movies/Anime -> "Internal storage / Movies / Anime"
     * /storage/1234-5678/Movies -> "SD card / Movies"
     */
    fun sanitizeStoragePath(fullPath: String): String {
        val trimmed = fullPath.trim().removeSuffix("/")
        return when {
            trimmed == PRIMARY_STORAGE -> "Internal storage"
            trimmed.startsWith(PRIMARY_STORAGE) -> {
                val relParts = trimmed.substring(PRIMARY_STORAGE.length).split("/").filter { it.isNotEmpty() }
                if (relParts.isEmpty()) "Internal storage" else "Internal storage / " + relParts.joinToString(" / ")
            }
            trimmed.matches(Regex("""^/storage/[A-Za-z0-9_-]+$""")) -> "SD card"
            trimmed.matches(Regex("""^/storage/[A-Za-z0-9_-]+/.*""")) -> {
                val parts = trimmed.split("/").filter { it.isNotEmpty() }
                if (parts.size > 2) {
                    val rel = parts.drop(2).joinToString(" / ")
                    "SD card / $rel"
                } else {
                    "SD card"
                }
            }
            else -> trimmed
        }
    }

    /**
     * Disambiguates duplicate folder names by appending parent folder when names clash.
     * E.g. two "Anime" folders at "/storage/emulated/0/Movies/Anime" and
     * "/storage/emulated/0/Downloads/Anime" become "Anime (Movies)" and "Anime (Downloads)".
     */
    fun disambiguateFolderNames(folderPaths: List<String>): Map<String, String> {
        val namesByCount = folderPaths.groupingBy {
            it.trimEnd('/').substringAfterLast('/')
        }.eachCount()

        return folderPaths.associateWith { path ->
            val cleanPath = path.trimEnd('/')
            val baseName = cleanPath.substringAfterLast('/')
            if ((namesByCount[baseName] ?: 0) > 1) {
                val parent = cleanPath.substringBeforeLast('/', "").substringAfterLast('/')
                if (parent.isNotEmpty() && parent != "0" && parent != "emulated") {
                    "$baseName ($parent)"
                } else {
                    baseName
                }
            } else {
                baseName
            }
        }
    }
}
