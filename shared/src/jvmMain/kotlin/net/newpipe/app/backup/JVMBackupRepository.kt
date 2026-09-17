package net.newpipe.app.backup

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import co.touchlab.kermit.Logger

@Serializable
private data class AppBackup(
    val subscriptions: String? = null,
    val history: String? = null,
    val bookmarks: String? = null
)

class JVMBackupRepository(
    private val settings: Settings,
    private val subscriptionRepo: net.newpipe.app.subscription.SubscriptionRepository,
    private val historyRepo: net.newpipe.app.history.HistoryRepository,
    private val bookmarkRepo: net.newpipe.app.bookmark.BookmarkRepository
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun exportData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val subs = settings.getStringOrNull("subscriptions_v1")
            val hist = settings.getStringOrNull("history_videos_v1")
            val books = settings.getStringOrNull("saved_videos_list")

            val backup = AppBackup(subs, hist, books)
            val jsonString = json.encodeToString(backup)

            val dialog = FileDialog(null as Frame?, "Exportar datos de NewPipe", FileDialog.SAVE)
            dialog.file = "newpipe_backup.json"
            dialog.isVisible = true

            val dir = dialog.directory
            val file = dialog.file

            if (dir != null && file != null) {
                File(dir, file).writeText(jsonString)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e("JVMBackupRepository", e) { "Error al exportar datos" }
            false
        }
    }

    override suspend fun importData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, "Importar datos de NewPipe", FileDialog.LOAD)
            dialog.isVisible = true

            val dir = dialog.directory
            val file = dialog.file

            if (dir != null && file != null) {
                val jsonString = File(dir, file).readText()
                val backup = json.decodeFromString<AppBackup>(jsonString)

                backup.subscriptions?.let { settings.putString("subscriptions_v1", it) }
                backup.history?.let { settings.putString("history_videos_v1", it) }
                backup.bookmarks?.let { settings.putString("saved_videos_list", it) }

                // Recargar el estado reactivo desde Settings
                subscriptionRepo.reload()
                historyRepo.reload()
                bookmarkRepo.reload()

                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e("JVMBackupRepository", e) { "Error al importar datos" }
            false
        }
    }
}
