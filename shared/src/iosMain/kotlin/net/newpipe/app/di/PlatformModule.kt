package net.newpipe.app.di
import org.koin.core.module.Module
import org.koin.dsl.module
actual val platformModule: Module = module { 
    single<net.newpipe.app.backup.BackupRepository> { net.newpipe.app.backup.IOSBackupRepository() }
}
