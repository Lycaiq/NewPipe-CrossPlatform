package net.newpipe.app.backup

class IOSBackupRepository : BackupRepository {
    override suspend fun exportData(): Boolean = false
    override suspend fun importData(): Boolean = false
}
