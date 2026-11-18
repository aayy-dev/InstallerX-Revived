// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog.inner

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.WarningTextBlock
import com.rosan.installer.ui.page.main.installer.components.failedIcon
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton
import com.rosan.installer.ui.page.main.installer.dialog.DialogInnerParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.DialogParamsType
import com.rosan.installer.ui.page.main.installer.dialog.dialogButtons
import com.rosan.installer.ui.page.main.installer.mapper.rememberInstallStateResult
import com.rosan.installer.ui.page.main.installer.mapper.resolveInstallPrepareTarget
import com.rosan.installer.ui.page.main.widget.chip.Chip
import com.rosan.installer.ui.page.main.widget.chip.InstallInfoChipGroup

@Composable
private fun installPrepareEmptyDialog(viewModel: InstallerViewModel) = DialogParams(
    icon = DialogInnerParams(
        DialogParamsType.IconError.id,
        failedIcon,
    ),
    title = DialogInnerParams(
        DialogParamsType.InstallerPrepare.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install))
    },
    text = DialogInnerParams(
        DialogParamsType.InstallerPrepareEmpty.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install_empty))
    },
    buttons = dialogButtons(
        DialogParamsType.ButtonsCancel.id,
    ) {
        listOf(
            DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(InstallerViewAction.InstallChoice)
            },
            DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(InstallerViewAction.Close)
            },
        )
    },
)

@Composable
private fun installPrepareTooManyDialog(viewModel: InstallerViewModel) = DialogParams(
    icon = DialogInnerParams(
        DialogParamsType.IconError.id,
        failedIcon,
    ),
    title = DialogInnerParams(
        DialogParamsType.InstallerPrepare.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install))
    },
    text = DialogInnerParams(
        DialogParamsType.InstallerPrepareTooMany.id,
    ) {
        Text(stringResource(R.string.installer_prepare_install_too_many))
    },
    buttons = dialogButtons(
        DialogParamsType.ButtonsCancel.id,
    ) {
        listOf(
            DialogButton(stringResource(R.string.previous)) {
                viewModel.dispatch(InstallerViewAction.InstallChoice)
            },
            DialogButton(stringResource(R.string.cancel)) {
                viewModel.dispatch(InstallerViewAction.Close)
            },
        )
    },
)

@Composable
fun installPrepareDialog(viewModel: InstallerViewModel): DialogParams {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val currentPackageName = uiState.currentPackageName
    val currentPackage = uiState.analysisResults.find { it.packageName == currentPackageName }
    val settings = uiState.viewSettings

    // If there is no specific package to prepare, show an empty/error dialog.
    if (currentPackage == null) {
        return if (uiState.analysisResults.size > 1) {
            installPrepareTooManyDialog(viewModel)
        } else {
            installPrepareEmptyDialog(viewModel)
        }
    }

    val target = resolveInstallPrepareTarget(currentPackage) ?: return installPrepareEmptyDialog(viewModel)
    val selectedEntities = target.selectedEntities
    val primaryEntity = target.primaryEntity
    val entityToInstall = target.entityToInstall
    val containerType = target.containerType
    val isSplitUpdateMode = target.isSplitUpdateMode

    var showChips by remember(settings.expandTemporarySettingsByDefault) {
        mutableStateOf(settings.expandTemporarySettingsByDefault)
    }
    // Call InstallInfoDialog for base structure
    val baseParams = installInfoDialog(
        viewModel = viewModel,
        onTitleExtraClick = { showChips = !showChips },
    )

    val installStateResult = rememberInstallStateResult(target, settings)
    val notices = installStateResult.notices
    val buttonTextId = installStateResult.buttonTextId

    return baseParams.copy(
        // Subtitle is inherited from InstallInfoDialog (shows new version + package name)
        text = DialogInnerParams(
            DialogParamsType.InstallerPrepareInstall.id,
        ) {
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    InstallInfoChipGroup(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        notices = notices,
                    )
                }
                item {
                    AnimatedVisibility(
                        visible = (primaryEntity is AppEntity.ModuleEntity) &&
                            primaryEntity.description.isNotBlank() &&
                            config.displaySdk,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(
                                text = (primaryEntity as AppEntity.ModuleEntity).description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                item {
                    AnimatedVisibility(
                        visible = showChips,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Chip(
                                selected = config.autoDelete, // Read directly from config
                                onClick = {
                                    // Update via ViewModel
                                    viewModel.updateConfig { it.copy(autoDelete = !it.autoDelete) }
                                },
                                label = stringResource(id = R.string.config_auto_delete),
                                icon = AppIcons.Delete,
                            )
                            Chip(
                                selected = config.displaySdk, // Read directly from config
                                onClick = {
                                    // Update via ViewModel
                                    viewModel.updateConfig { it.copy(displaySdk = !it.displaySdk) }
                                },
                                label = stringResource(id = R.string.config_display_sdk_version),
                                icon = AppIcons.Info,
                            )
                            Chip(
                                selected = config.displaySize, // Read directly from config
                                onClick = {
                                    // Update via ViewModel
                                    viewModel.updateConfig { it.copy(displaySize = !it.displaySize) }
                                },
                                label = stringResource(id = R.string.config_display_size),
                                icon = AppIcons.ShowSize,
                            )
                            if (DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
                            ) {
                                Chip(
                                    selected = settings.showOPPOSpecial, // From viewSettings
                                    onClick = {
                                        val newValue = !settings.showOPPOSpecial
                                        viewModel.dispatch(InstallerViewAction.SetTempShowOPPOSpecial(newValue))
                                    },
                                    label = stringResource(id = R.string.installer_show_oem_special),
                                    icon = AppIcons.OEMSpecial,
                                )
                            }
                        }
                    }
                }

                val isInvalidSplitInstall = currentPackage.installedAppInfo == null &&
                    entityToInstall == null &&
                    selectedEntities.any { it is AppEntity.SplitEntity }

                if (isInvalidSplitInstall) {
                    item {
                        WarningTextBlock(
                            listOf(Pair(stringResource(R.string.installer_splits_invalid_tip), MaterialTheme.colorScheme.error)),
                        )
                    }
                }
            }
        },
        buttons = dialogButtons(
            DialogParamsType.InstallerPrepareInstall.id,
        ) {
            // --- Use buildList to dynamically create buttons ---
            buildList {
                val isAPK =
                    containerType == DataType.APKS || containerType == DataType.XAPK || containerType == DataType.APKM ||
                        containerType == DataType.MIXED_MODULE_APK

                val canInstallBaseEntity = (primaryEntity as? AppEntity.BaseEntity)?.let { base ->
                    if (entityToInstall != null) {
                        // Installing Base: Check SDK
                        base.minSdk?.toIntOrNull()?.let { sdk -> sdk <= Build.VERSION.SDK_INT } ?: true
                    } else {
                        // Bundle Split Update: Allowed if installed
                        isSplitUpdateMode
                    }
                } ?: false

                val canInstallModuleEntity = (primaryEntity as? AppEntity.ModuleEntity)?.let {
                    settings.enableModuleInstall
                } ?: false

                val canInstallSplitEntity = (primaryEntity as? AppEntity.SplitEntity)?.let {
                    currentPackage.installedAppInfo != null
                } ?: false

                val canInstall = !installStateResult.isOplusOsdkIncompatible &&
                    (canInstallBaseEntity || canInstallModuleEntity || canInstallSplitEntity)

                // only when the entity is a split APK, XAPK, or APKM
                if (canInstall && settings.showExtendedMenu && isAPK) {
                    add(
                        DialogButton(stringResource(R.string.install_choice), 1f) {
                            viewModel.dispatch(InstallerViewAction.InstallChoice)
                        },
                    )
                }
                if (canInstall) {
                    add(
                        DialogButton(
                            text = stringResource(buttonTextId),
                            weight = 1f,
                            isDefaultFocus = true,
                            onLongClick = if (settings.longClickBackgroundInstall) {
                                {
                                    // Trigger install directly
                                    viewModel.dispatch(InstallerViewAction.Install(true))
                                    // Force background auto silent install regardless of settings
                                    if (!viewModel.isInstallingModule) {
                                        viewModel.dispatch(InstallerViewAction.Background)
                                    }
                                }
                            } else {
                                null
                            },
                            onClick = {
                                viewModel.dispatch(InstallerViewAction.Install(true))
                                if (settings.autoSilentInstall && !viewModel.isInstallingModule) {
                                    viewModel.dispatch(InstallerViewAction.Background)
                                }
                            },
                        ),
                    )
                }
                // else if app can be installed and extended menu is shown
                if (canInstall && settings.showExtendedMenu && primaryEntity !is AppEntity.ModuleEntity) {
                    add(
                        DialogButton(stringResource(R.string.menu), 1f) {
                            viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
                        },
                    )
                }
                if (canInstall && !settings.showExtendedMenu && isAPK) {
                    add(
                        DialogButton(stringResource(R.string.install_choice), 1f) {
                            viewModel.dispatch(InstallerViewAction.InstallChoice)
                        },
                    )
                }
                // Cancel button always shown
                add(
                    DialogButton(stringResource(R.string.cancel), 1f) {
                        viewModel.dispatch(InstallerViewAction.Close)
                    },
                )
            }
            // --- BuildList END ---
        },
    )
}
