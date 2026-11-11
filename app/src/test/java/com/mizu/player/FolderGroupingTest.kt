package com.mizu.player

import com.mizu.player.util.FolderUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderGroupingTest {

    @Test
    fun testSanitizeStoragePathPrimary() {
        assertEquals("Internal storage", FolderUtils.sanitizeStoragePath("/storage/emulated/0"))
        assertEquals("Internal storage / Movies / Anime", FolderUtils.sanitizeStoragePath("/storage/emulated/0/Movies/Anime"))
    }

    @Test
    fun testSanitizeStoragePathSdCard() {
        assertEquals("SD card", FolderUtils.sanitizeStoragePath("/storage/1234-5678"))
        assertEquals("SD card / Movies / Anime", FolderUtils.sanitizeStoragePath("/storage/1234-5678/Movies/Anime"))
    }

    @Test
    fun testDisambiguateFolderNames() {
        val paths = listOf(
            "/storage/emulated/0/Movies/Anime",
            "/storage/emulated/0/Download/Anime",
            "/storage/emulated/0/Movies/Documentaries"
        )
        val disambiguated = FolderUtils.disambiguateFolderNames(paths)
        assertEquals("Anime (Movies)", disambiguated["/storage/emulated/0/Movies/Anime"])
        assertEquals("Anime (Download)", disambiguated["/storage/emulated/0/Download/Anime"])
        assertEquals("Documentaries", disambiguated["/storage/emulated/0/Movies/Documentaries"])
    }
}
