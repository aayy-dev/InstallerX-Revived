// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.rosan.installer.ui.theme.LocalTvLayout

/**
 * Margins that keep TV content clear of the screen edges, which some TVs crop (overscan).
 */
val TvSafeArea = PaddingValues(horizontal = 48.dp, vertical = 27.dp)

/**
 * Marks content as the TV layout and raises the focused-state alpha of Material 3 ripples so focus
 * on reused phone components (list rows, chips, switches) stays visible at TV viewing distance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvFocusEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTvLayout provides true,
        LocalRippleConfiguration provides RippleConfiguration(
            color = MaterialTheme.colorScheme.primary,
            rippleAlpha = RippleAlpha(
                draggedAlpha = 0.16f,
                focusedAlpha = 0.32f,
                hoveredAlpha = 0.08f,
                pressedAlpha = 0.24f,
            ),
        ),
        content = content,
    )
}
