// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.installer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Checkbox
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.rosan.installer.R
import com.rosan.installer.data.engine.parser.getDisplayName
import com.rosan.installer.data.engine.parser.getSplitDisplayName
import com.rosan.installer.domain.engine.model.install.MmzSelectionMode
import com.rosan.installer.domain.engine.model.install.SessionMode
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.SelectInstallEntity
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvBackdrop
import com.rosan.installer.ui.page.tv.components.TvKit
import com.rosan.installer.ui.page.tv.components.TvPageTitle
import com.rosan.installer.ui.page.tv.components.TvRow
import com.rosan.installer.ui.page.tv.components.TvSectionHeader
import com.rosan.installer.ui.page.tv.components.rememberInitialFocus
import com.rosan.installer.ui.util.getSupportSubtitle
import com.rosan.installer.ui.util.getSupportTitle

/**
 * TV version of the package/split choice step. Selection goes through the same view-model actions
 * as the phone dialog; the enable and back rules mirror it.
 */
@Composable
fun TvInstallChoiceScreen(viewModel: InstallerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val analysisResults = uiState.analysisResults
    val sourceType = analysisResults.firstOrNull()?.appEntities?.firstOrNull()?.app?.sourceType ?: DataType.NONE
    val isMultiApk = (analysisResults.firstOrNull()?.sessionMode ?: SessionMode.Single) == SessionMode.Batch
    val isModuleApk = sourceType == DataType.MIXED_MODULE_APK
    val isMixedModuleZip = sourceType == DataType.MIXED_MODULE_ZIP
    var selectionMode by rememberSaveable(sourceType) { mutableStateOf(MmzSelectionMode.INITIAL_CHOICE) }

    val allEntities = remember(analysisResults) { analysisResults.flatMap { it.appEntities } }
    val selected = allEntities.filter { it.selected }
    val selectedModules = selected.count { it.app is AppEntity.ModuleEntity }
    val selectedApps = selected.size - selectedModules
    val totalModules = allEntities.count { it.app is AppEntity.ModuleEntity }
    val isMixedError = selectedModules > 0 && selectedApps > 0
    val isMultiModuleError = selectedModules > 1
    val errorMessage = when {
        isMixedError -> stringResource(R.string.installer_error_mixed_selection)
        isMultiModuleError -> stringResource(R.string.installer_error_multiple_modules)
        else -> null
    }
    val canProceed = selected.isNotEmpty() && !isMixedError && !isMultiModuleError
    val isMmzBack = isMixedModuleZip && selectionMode == MmzSelectionMode.APK_CHOICE

    val actions = buildList {
        if ((!isModuleApk && !isMixedModuleZip) || isMmzBack) {
            add(
                TvAction(
                    text = stringResource(if (isMultiApk) R.string.install else R.string.next),
                    enabled = canProceed,
                    isDefault = true,
                    onClick = {
                        if (canProceed) {
                            viewModel.dispatch(if (isMultiApk) InstallerViewAction.InstallMultiple else InstallerViewAction.InstallPrepare)
                        }
                    },
                ),
            )
        }
        add(
            TvAction(
                text = stringResource(if (isMmzBack) R.string.back else R.string.cancel),
                onClick = {
                    if (isMmzBack) {
                        viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
                        selectionMode = MmzSelectionMode.INITIAL_CHOICE
                    } else {
                        viewModel.dispatch(InstallerViewAction.Close)
                    }
                },
            ),
        )
    }

    // Remote Back: a split choice returns to the app page when the selection is valid (it only edits
    // the selection), the module-zip APK list returns to its first step, and every other choice
    // cancels like the Cancel button, so Back never starts an install.
    val isSplitChoice = !isMultiApk && !isModuleApk && !isMixedModuleZip
    BackHandler {
        when {
            isMmzBack -> {
                viewModel.dispatch(InstallerViewAction.SetApkSelection(false))
                selectionMode = MmzSelectionMode.INITIAL_CHOICE
            }

            isSplitChoice && canProceed -> viewModel.dispatch(InstallerViewAction.InstallPrepare)

            else -> viewModel.dispatch(InstallerViewAction.Close)
        }
    }

    val baseGroupTitle = stringResource(R.string.split_name_base_group_title)
    val installAsModule = stringResource(R.string.installer_choice_install_as_module)
    val installAsApp = stringResource(R.string.installer_choice_install_as_app)
    val installAsAppDesc = stringResource(R.string.installer_choice_install_as_app_desc)
    val splitGroupTitles = com.rosan.installer.domain.engine.model.source.SplitType.entries.associateWith { it.getDisplayName() }

    // Focus starts on the first choice, so the list is usable without first moving off the buttons.
    val listFocusRequester = rememberInitialFocus(selectionMode)

    TvBackdrop(accent = uiState.seedColor) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) {
                TvPageTitle(sourceType.getSupportTitle())
                sourceType.getSupportSubtitle(selectionMode)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                errorMessage?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, fontSize = 17.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = TvKit.SafeVertical)) {
                    TvActionRow(actions = actions, requestInitialFocus = false)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .focusRequester(listFocusRequester),
                contentPadding = PaddingValues(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
            ) {
                when {
                    isModuleApk -> {
                        allEntities.firstOrNull { it.app is AppEntity.BaseEntity }?.let { entity ->
                            val base = entity.app as AppEntity.BaseEntity
                            item {
                                TvRow(base.label ?: base.packageName, summary = base.packageName) {
                                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(false))
                                }
                            }
                        }
                        allEntities.firstOrNull { it.app is AppEntity.ModuleEntity }?.let { entity ->
                            val module = entity.app as AppEntity.ModuleEntity
                            item {
                                TvRow(module.name, summary = module.id) {
                                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(true))
                                }
                            }
                        }
                    }

                    isMixedModuleZip && selectionMode == MmzSelectionMode.INITIAL_CHOICE && totalModules == 1 -> {
                        allEntities.firstOrNull { it.app is AppEntity.ModuleEntity }?.let { entity ->
                            item {
                                TvRow(installAsModule, summary = (entity.app as AppEntity.ModuleEntity).id) {
                                    viewModel.dispatch(InstallerViewAction.SelectMixedModuleType(true))
                                }
                            }
                        }
                        if (allEntities.any { it.app is AppEntity.BaseEntity }) {
                            item {
                                TvRow(installAsApp, summary = installAsAppDesc) {
                                    if (uiState.config.apkChooseAll) viewModel.dispatch(InstallerViewAction.SetApkSelection(true))
                                    selectionMode = MmzSelectionMode.APK_CHOICE
                                }
                            }
                        }
                    }

                    isMultiApk || isMmzBack -> {
                        val results = if (isMmzBack) {
                            analysisResults.mapNotNull { result ->
                                result.appEntities
                                    .filter { it.app is AppEntity.BaseEntity || it.app is AppEntity.SplitEntity || it.app is AppEntity.DexMetadataEntity }
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { result.copy(appEntities = it) }
                            }
                        } else {
                            analysisResults
                        }
                        results.forEach { result ->
                            val bases = result.appEntities.filter { it.app is AppEntity.BaseEntity }
                            if (bases.size <= 1) {
                                val entity = bases.firstOrNull() ?: result.appEntities.first()
                                item(key = result.packageName) {
                                    TvEntityRow(entity, multiSelect = true) {
                                        viewModel.dispatch(InstallerViewAction.TogglePackageSelection(result.packageName, entity))
                                    }
                                }
                            } else {
                                // Several base APKs of one package are version alternatives: pick one.
                                val label = bases.firstNotNullOfOrNull { (it.app as AppEntity.BaseEntity).label } ?: result.packageName
                                item(key = "${result.packageName}_header") { TvSectionHeader(label) }
                                result.appEntities
                                    .sortedByDescending { (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0 }
                                    .forEach { entity ->
                                        item {
                                            TvEntityRow(entity, multiSelect = false) {
                                                viewModel.dispatch(InstallerViewAction.ToggleSelection(result.packageName, entity, false))
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    else -> {
                        val entities = analysisResults.firstOrNull()?.appEntities.orEmpty()
                        val bases = entities.filter { it.app is AppEntity.BaseEntity || it.app is AppEntity.DexMetadataEntity }
                        val splitGroups = entities
                            .filter { it.app is AppEntity.SplitEntity }
                            .groupBy { (it.app as AppEntity.SplitEntity).type }
                            .toSortedMap(compareBy { it.ordinal })
                        entityGroup(baseGroupTitle, bases, viewModel)
                        splitGroups.forEach { (type, group) -> entityGroup(splitGroupTitles.getValue(type), group, viewModel) }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.entityGroup(title: String, entities: List<SelectInstallEntity>, viewModel: InstallerViewModel) {
    if (entities.isEmpty()) return
    item { TvSectionHeader(title) }
    entities.forEach { entity ->
        item {
            TvEntityRow(entity, multiSelect = true) {
                viewModel.dispatch(InstallerViewAction.ToggleSelection(entity.app.packageName, entity, true))
            }
        }
    }
}

@Composable
private fun TvEntityRow(entity: SelectInstallEntity, multiSelect: Boolean, onClick: () -> Unit) {
    val (title, summary) = when (val app = entity.app) {
        is AppEntity.BaseEntity -> (app.label ?: app.packageName) to
            stringResource(R.string.installer_version, app.versionName, app.versionCode.toString())

        is AppEntity.SplitEntity -> getSplitDisplayName(app.type, app.configValue, app.splitName) to
            stringResource(R.string.installer_file_name, app.name)

        is AppEntity.DexMetadataEntity -> app.dmName to app.packageName

        is AppEntity.ModuleEntity -> app.name to app.id
    }
    TvRow(
        title = title,
        summary = summary,
        trailing = {
            if (multiSelect) {
                Checkbox(checked = entity.selected, onCheckedChange = null)
            } else {
                RadioButton(selected = entity.selected, onClick = null)
            }
        },
        onClick = onClick,
    )
}
