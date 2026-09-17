package net.newpipe.app.backup

interface BackupRepository {
    suspend fun exportData(): Boolean
    suspend fun importData(): Boolean
}
