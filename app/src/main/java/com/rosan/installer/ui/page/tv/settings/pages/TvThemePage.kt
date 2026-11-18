// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.preferences.theme.PaletteStyle
import com.rosan.installer.domain.settings.model.preferences.theme.ThemeColorSpec
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.theme.ThemeSettingsViewModel
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.ui.theme.material.PresetColors
import com.rosan.installer.ui.util.getDisplayName
import org.koin.androidx.compose.koinViewModel

/**
 * Theme settings on TV. TV is always dark and has no blur, floating bar or predictive back, so only
 * the color settings and icon source remain.
 */
@Composable
fun TvThemePage(viewModel: ThemeSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val paletteStyles = PaletteStyle.entries
    val availableSpecs = uiState.paletteStyle.supportedSpecs
    val activeSpec = uiState.paletteStyle.effectiveSpec(uiState.colorSpec)
    val specDescription = if (availableSpecs.size == 1) {
        stringResource(
            when (activeSpec) {
                ThemeColorSpec.SPEC_2026 -> R.string.theme_settings_color_spec_only_2026
                else -> R.string.theme_settings_color_spec_only_2021
            },
        )
    } else {
        null
    }

    val colors = uiState.availableColors
    val colorNames = colors.map { it.getDisplayName() }
    val showThemeColor = !uiState.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    val isWallpaperSeed = uiState.useDynamicColor &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
        // When wallpaper colors are unavailable the presets are shown instead.
        colors != PresetColors

    TvSettingsPage(title = stringResource(R.string.theme_settings)) {
        header(context.getString(R.string.theme_settings_google_ui))
        choice(
            key = "palette_style",
            title = context.getString(R.string.theme_settings_palette_style),
            icon = AppIcons.Palette,
            description = context.getString(R.string.theme_settings_palette_style_desc),
            options = paletteStyles.map { TvChoiceOption(it.displayName, it.desc.ifBlank { null }) },
            selectedIndex = paletteStyles.indexOf(uiState.paletteStyle),
            onSelect = { viewModel.dispatch(ThemeSettingsAction.SetPaletteStyle(paletteStyles[it])) },
        )
        choice(
            key = "color_spec",
            title = context.getString(R.string.theme_settings_color_spec),
            icon = AppIcons.Theme,
            enabled = availableSpecs.size > 1,
            description = specDescription,
            options = availableSpecs.map { TvChoiceOption(it.displayName) },
            selectedIndex = availableSpecs.indexOf(activeSpec).coerceAtLeast(0),
            onSelect = { viewModel.dispatch(ThemeSettingsAction.SetColorSpec(availableSpecs[it])) },
        )
        switch(
            key = "dynamic_color",
            title = context.getString(R.string.theme_settings_dynamic_color),
            summary = context.getString(R.string.theme_settings_dynamic_color_desc),
            icon = AppIcons.Palette,
            checked = uiState.useDynamicColor,
            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it)) },
        )
        switch(
            key = "follow_icon",
            title = context.getString(R.string.theme_settings_dynamic_color_follow_icon),
            summary = context.getString(R.string.theme_settings_dynamic_color_follow_icon_desc),
            icon = AppIcons.Dialog,
            checked = uiState.useDynColorFollowPkgIcon,
            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIcon(it)) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && uiState.showLiveActivity) {
            switch(
                key = "follow_icon_live_activity",
                title = context.getString(R.string.theme_settings_live_activity_dynamic_color_follow_icon),
                summary = context.getString(R.string.theme_settings_live_activity_dynamic_color_follow_icon_desc),
                icon = AppIcons.Notification,
                checked = uiState.useDynColorFollowPkgIconForLiveActivity,
                onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.SetDynColorFollowPkgIconForLiveActivity(it)) },
            )
        }

        if (showThemeColor) {
            header(context.getString(R.string.theme_settings_theme_color))
            choice(
                key = "theme_color",
                title = context.getString(R.string.theme_settings_theme_color),
                icon = AppIcons.Theme,
                options = colorNames.map { TvChoiceOption(it) },
                selectedIndex = colors.indexOfFirst { it.color == uiState.seedColor },
                onSelect = { index ->
                    viewModel.dispatch(ThemeSettingsAction.SetSeedColor(colors[index].color, isWallpaperSeed))
                },
            )
        }

        header(context.getString(R.string.theme_settings_package_icons))
        switch(
            key = "prefer_system_icon",
            title = context.getString(R.string.theme_settings_prefer_system_icon),
            summary = context.getString(R.string.theme_settings_prefer_system_icon_desc),
            icon = AppIcons.IconPack,
            checked = uiState.preferSystemIcon,
            onCheckedChange = { viewModel.dispatch(ThemeSettingsAction.ChangePreferSystemIcon(it)) },
        )
    }
}
