package net.newpipe.app.backup

class AndroidBackupRepository : BackupRepository {
    override suspend fun exportData(): Boolean = false
    override suspend fun importData(): Boolean = false
}
