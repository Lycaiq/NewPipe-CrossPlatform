/*
* SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package net.newpipe.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.newpipe.app.platform.BuildInfo
import net.newpipe.app.screen.settings.model.SettingsCategoryType
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    buildInfo: BuildInfo,
    private val backupRepository: net.newpipe.app.backup.BackupRepository
) : ViewModel() {

    val categories: StateFlow<List<SettingsCategoryType>>
        field = MutableStateFlow(computeVisible(buildInfo))

    fun exportData() {
        viewModelScope.launch {
            val success = backupRepository.exportData()
            // Podríamos mostrar un Toast o Snackbar, pero por ahora logeamos o lo dejamos
        }
    }

    fun importData() {
        viewModelScope.launch {
            val success = backupRepository.importData()
            // Podríamos mostrar un Toast o Snackbar
        }
    }

    private fun computeVisible(buildInfo: BuildInfo): List<SettingsCategoryType> =
        SettingsCategoryType.entries.filter { type ->
            // Si es release mostramos la vaina de updates. Si es debug, pues el menú de debug.
            // Los demás settings se muestran siempre.
            when (type) {
                SettingsCategoryType.UPDATES -> buildInfo.isReleaseApk
                SettingsCategoryType.DEBUG -> buildInfo.isDebug
                else -> true
            }
        }
}

