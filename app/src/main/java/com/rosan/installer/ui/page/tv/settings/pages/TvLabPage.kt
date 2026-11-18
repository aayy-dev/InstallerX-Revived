// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowDownward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import com.rosan.installer.R
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.domain.device.model.ShizukuMode
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.domain.settings.model.preferences.SmartAuthorizerCandidate
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.lab.LabSettingsViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.components.TvKit
import com.rosan.installer.ui.page.tv.components.TvRadioRow
import com.rosan.installer.ui.page.tv.components.TvSwitchRow
import com.rosan.installer.ui.page.tv.components.tvFocusedContainer
import com.rosan.installer.ui.page.tv.components.tvFocusedContent
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val rootImplementations = listOf(RootMode.Magisk, RootMode.KernelSU, RootMode.APatch)
private val rootImplementationNames = listOf("Magisk", "KernelSU", "APatch")

@Composable
fun TvLabPage(viewModel: LabSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val isSystemApp = capabilityProvider.isSystemApp
    val rootMode by capabilityProvider.rootModeFlow.collectAsStateWithLifecycle()
    val shizukuMode by capabilityProvider.shizukuModeFlow.collectAsStateWithLifecycle()
    val shizukuAuthorized by capabilityProvider.shizukuAuthorizedFlow.collectAsStateWithLifecycle()
    val dhizukuAvailable by capabilityProvider.dhizukuAvailableFlow.collectAsStateWithLifecycle()
    val dhizukuAuthorized by capabilityProvider.dhizukuAuthorizedFlow.collectAsStateWithLifecycle()

    var showRootImplementationPicker by rememberSaveable { mutableStateOf(false) }
    var showFallbackList by rememberSaveable { mutableStateOf(false) }
    var showNonAdbConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showNonAdbConfirmation) {
        BackHandler { showNonAdbConfirmation = false }
        TvConfirmScreen(
            title = stringResource(R.string.lab_shizuku_non_adb_install),
            description = stringResource(R.string.lab_shizuku_non_adb_install_confirmation, context.packageName),
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.confirm), onClick = {
                            showNonAdbConfirmation = false
                            viewModel.dispatch(LabSettingsAction.LabChangeShizukuNonAdbInstall(true))
                        }),
                        TvAction(stringResource(R.string.cancel), onClick = { showNonAdbConfirmation = false }, isDefault = true),
                    ),
                )
            },
        )
        return
    }

    if (showRootImplementationPicker) {
        BackHandler { showRootImplementationPicker = false }
        TvSettingsPage(title = stringResource(R.string.lab_module_select_root_impl)) {
            rootImplementations.forEachIndexed { index, mode ->
                custom(key = "root_impl_${mode.name}") {
                    TvRadioRow(
                        title = rootImplementationNames[index],
                        selected = uiState.labRootMode == mode,
                        onClick = {
                            showRootImplementationPicker = false
                            // Same order as the phone dialog: save the implementation, then enable flashing.
                            viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(mode))
                            viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(true))
                        },
                    )
                }
            }
        }
        return
    }

    if (showFallbackList) {
        BackHandler { showFallbackList = false }
        TvSmartAuthorizerList(
            candidates = uiState.smartAuthorizerCandidates,
            rootMode = rootMode,
            shizukuMode = shizukuMode,
            shizukuAuthorized = shizukuAuthorized,
            dhizukuAvailable = dhizukuAvailable,
            dhizukuAuthorized = dhizukuAuthorized,
            isSystemApp = isSystemApp,
            onCandidatesChange = { viewModel.dispatch(LabSettingsAction.LabChangeSmartAuthorizerCandidates(it)) },
        )
        return
    }

    val smartAuthorizerSummary = uiState.smartAuthorizerCandidates
        .filter { it.enabled }
        .joinToString("->") { smartAuthorizerDisplayName(it.authorizer, isSystemApp, context::getString) }
    val rootImplementationOptions = rootImplementationNames.map { TvChoiceOption(it) }

    TvSettingsPage(title = stringResource(R.string.lab)) {
        info(key = "lab_tip", title = context.getString(R.string.lab), summary = context.getString(R.string.lab_tip), icon = AppIcons.Tip)

        header(context.getString(R.string.config_authorizer))
        switch(
            key = "try_multiple_authorizers",
            title = context.getString(R.string.config_try_multiple_authorizers_on_install),
            summary = context.getString(R.string.config_try_multiple_authorizers_on_install_desc),
            icon = AppIcons.Authorizer,
            checked = uiState.tryMultipleAuthorizersOnInstall,
            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeTryMultipleAuthorizersOnInstall(it)) },
        )
        if (uiState.tryMultipleAuthorizersOnInstall) {
            item(
                key = "fallback_list",
                title = context.getString(R.string.config_smart_authorizer_fallback_list),
                summary = context.getString(R.string.config_smart_authorizer_fallback_list_desc, smartAuthorizerSummary),
                icon = AppIcons.Authorizer,
                onClick = {
                    capabilityProvider.refreshPrivilegeStatus()
                    showFallbackList = true
                },
            )
        }

        header(context.getString(R.string.config_authorizer_root))
        switch(
            key = "install_as_system_uid",
            title = context.getString(R.string.lab_root_install_as_system_uid),
            summary = context.getString(R.string.lab_root_install_as_system_uid_desc),
            icon = AppIcons.InstallSource,
            checked = uiState.labRootInstallAsSystemUid,
            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRootInstallAsSystemUid(it)) },
        )
        switch(
            key = "module_flashing",
            title = context.getString(R.string.lab_module_flashing),
            summary = context.getString(R.string.lab_module_flashing_desc),
            icon = AppIcons.Root,
            checked = uiState.labRootEnableModuleFlash,
            onCheckedChange = { enable ->
                if (enable) {
                    showRootImplementationPicker = true
                } else {
                    viewModel.dispatch(LabSettingsAction.LabChangeRootModuleFlash(false))
                }
            },
        )
        if (uiState.labRootEnableModuleFlash) {
            choice(
                key = "root_impl",
                title = context.getString(R.string.lab_module_select_root_impl),
                icon = AppIcons.RootMethod,
                options = rootImplementationOptions,
                selectedIndex = rootImplementations.indexOf(uiState.labRootMode).coerceAtLeast(0),
                onSelect = { index ->
                    rootImplementations.getOrNull(index)?.let {
                        viewModel.dispatch(LabSettingsAction.LabChangeRootImplementation(it))
                    }
                },
            )
            switch(
                key = "module_show_art",
                title = context.getString(R.string.lab_module_flashing_show_art),
                summary = context.getString(R.string.lab_module_flashing_show_art_desc),
                icon = AppIcons.Terminal,
                checked = uiState.labRootShowModuleArt,
                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRootShowModuleArt(it)) },
            )
        }

        header(context.getString(R.string.lab_compatibility))
        switch(
            key = "shizuku_non_adb",
            title = context.getString(R.string.lab_shizuku_non_adb_install),
            summary = context.getString(R.string.lab_shizuku_non_adb_install_desc),
            icon = AppIcons.Authorizer,
            checked = uiState.labShizukuNonAdbInstall,
            onCheckedChange = { enabled ->
                if (enabled) {
                    showNonAdbConfirmation = true
                } else {
                    viewModel.dispatch(LabSettingsAction.LabChangeShizukuNonAdbInstall(false))
                }
            },
        )
        switch(
            key = "abi_override",
            title = context.getString(R.string.lab_enable_abi_override),
            summary = context.getString(R.string.lab_enable_abi_override_desc),
            icon = AppIcons.Authorizer,
            checked = uiState.labEnableAbiOverride,
            onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeEnableAbiOverride(it)) },
        )
        if (uiState.labEnableAbiOverride) {
            switch(
                key = "try_different_architecture",
                title = context.getString(R.string.lab_try_different_architecture),
                summary = context.getString(R.string.lab_try_different_architecture_desc),
                icon = AppIcons.Authorizer,
                checked = uiState.labTryDifferentArchitecture,
                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeTryDifferentArchitecture(it)) },
            )
        }

        if (AppConfig.isRespectPlatformInstallPolicyAvailable) {
            header(context.getString(R.string.lab_unstable_features))
            switch(
                key = "respect_platform_install_policy",
                title = context.getString(R.string.lab_respect_platform_install_policy),
                summary = context.getString(R.string.lab_respect_platform_install_policy_desc),
                icon = AppIcons.InstallRequester,
                checked = uiState.labRespectPlatformInstallPolicy,
                onCheckedChange = { viewModel.dispatch(LabSettingsAction.LabChangeRespectPlatformInstallPolicy(it)) },
            )
        }
    }
}

/**
 * The fallback order for trying several authorizers. The phone drags rows; on TV each row toggles on
 * click, and Right moves focus to its up/down buttons.
 */
@Composable
private fun TvSmartAuthorizerList(
    candidates: List<SmartAuthorizerCandidate>,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    isSystemApp: Boolean,
    onCandidatesChange: (List<SmartAuthorizerCandidate>) -> Unit,
) {
    val context = LocalContext.current
    var listCandidates by remember { mutableStateOf(candidates) }

    LaunchedEffect(candidates) {
        if (listCandidates != candidates) listCandidates = candidates
    }

    fun updateCandidates(next: List<SmartAuthorizerCandidate>) {
        listCandidates = next
        onCandidatesChange(next)
    }

    fun toggleCandidate(candidate: SmartAuthorizerCandidate, enabled: Boolean) {
        if (!enabled && candidate.enabled && listCandidates.count { it.enabled } <= 1) {
            context.toast(R.string.config_smart_authorizer_must_choose_one)
            return
        }
        updateCandidates(listCandidates.toggle(candidate.authorizer, enabled))
    }

    val icons = listCandidates.associate { it.authorizer to smartAuthorizerIcon(it.authorizer) }
    val moveUpLabel = stringResource(R.string.move_item_up)
    val moveDownLabel = stringResource(R.string.move_item_down)

    TvSettingsPage(
        title = stringResource(R.string.config_smart_authorizer_fallback_list),
        subtitle = stringResource(R.string.config_smart_authorizer_fallback_list_tip_tv),
    ) {
        listCandidates.forEachIndexed { index, candidate ->
            custom(key = "candidate_${candidate.authorizer.value}") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TvSwitchRow(
                        title = smartAuthorizerDisplayName(candidate.authorizer, isSystemApp, context::getString),
                        summary = smartAuthorizerAvailabilityDescription(
                            authorizer = candidate.authorizer,
                            rootMode = rootMode,
                            shizukuMode = shizukuMode,
                            shizukuAuthorized = shizukuAuthorized,
                            dhizukuAvailable = dhizukuAvailable,
                            dhizukuAuthorized = dhizukuAuthorized,
                            isSystemApp = isSystemApp,
                            getString = context::getString,
                        ),
                        icon = icons[candidate.authorizer],
                        checked = candidate.enabled,
                        onCheckedChange = { toggleCandidate(candidate, it) },
                        modifier = Modifier.weight(1f),
                    )
                    TvMoveButton(
                        icon = AppIcons.ArrowUp,
                        contentDescription = moveUpLabel,
                        atEnd = index == 0,
                        onClick = { if (index > 0) updateCandidates(listCandidates.move(index, index - 1)) },
                    )
                    TvMoveButton(
                        icon = Icons.TwoTone.ArrowDownward,
                        contentDescription = moveDownLabel,
                        atEnd = index == listCandidates.lastIndex,
                        onClick = { if (index < listCandidates.lastIndex) updateCandidates(listCandidates.move(index, index + 1)) },
                    )
                }
            }
        }
    }
}

/**
 * A round, focusable icon button that matches the TV row focus style. At the end of the list it is
 * dimmed but stays focusable, so focus does not drop when an entry reaches the top or bottom.
 */
@Composable
private fun TvMoveButton(icon: ImageVector, contentDescription: String, atEnd: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .alpha(if (atEnd && !focused) 0.38f else 1f),
        shape = ClickableSurfaceDefaults.shape(shape = TvKit.PillShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = tvFocusedContainer(),
            focusedContentColor = tvFocusedContent(),
            pressedContainerColor = tvFocusedContainer(),
            pressedContentColor = tvFocusedContent(),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (focused) 24.dp else 22.dp),
            )
        }
    }
}

private fun smartAuthorizerDisplayName(authorizer: Authorizer, isSystemApp: Boolean, getString: (Int) -> String): String = if (authorizer == Authorizer.None) {
    getString(if (isSystemApp) R.string.working_status_system_installer else R.string.config_authorizer_none)
} else {
    getString(authorizer.displayNameRes)
}

@Composable
private fun smartAuthorizerIcon(authorizer: Authorizer): ImageVector = when (authorizer) {
    Authorizer.None -> AppIcons.None
    Authorizer.Root -> AppIcons.Root
    Authorizer.Shizuku -> ImageVector.vectorResource(R.drawable.ic_shizuku)
    Authorizer.Dhizuku -> AppIcons.InstallAllowRestrictedPermissions
    else -> AppIcons.Authorizer
}

private fun smartAuthorizerAvailabilityDescription(
    authorizer: Authorizer,
    rootMode: RootMode,
    shizukuMode: ShizukuMode,
    shizukuAuthorized: Boolean,
    dhizukuAvailable: Boolean,
    dhizukuAuthorized: Boolean,
    isSystemApp: Boolean,
    getString: (Int) -> String,
): String = when (authorizer) {
    Authorizer.Root -> if (rootMode != RootMode.None) {
        "${getString(R.string.available)} (${rootMode.name})"
    } else {
        getString(R.string.unavailable)
    }

    Authorizer.Shizuku -> when {
        shizukuAuthorized -> "${getString(R.string.activate)} (${shizukuMode.desc})"
        shizukuMode != ShizukuMode.NONE -> getString(R.string.shizuku_not_authorized)
        else -> getString(R.string.shizuku_not_available)
    }

    Authorizer.Dhizuku -> when {
        dhizukuAuthorized -> getString(R.string.activate)
        dhizukuAvailable -> getString(R.string.dhizuku_not_authorized)
        else -> getString(R.string.dhizuku_not_available)
    }

    Authorizer.None -> getString(
        if (isSystemApp) R.string.working_status_system_installer_desc else R.string.working_status_none_authorizer_desc,
    )

    else -> authorizer.value
}

private fun List<SmartAuthorizerCandidate>.move(from: Int, to: Int): List<SmartAuthorizerCandidate> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private fun List<SmartAuthorizerCandidate>.toggle(authorizer: Authorizer, enabled: Boolean): List<SmartAuthorizerCandidate> = map { candidate ->
    if (candidate.authorizer == authorizer) candidate.copy(enabled = enabled) else candidate
}
