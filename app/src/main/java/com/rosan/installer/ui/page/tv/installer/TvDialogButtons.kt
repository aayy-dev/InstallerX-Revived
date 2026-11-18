// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.installer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rosan.installer.ui.page.main.installer.dialog.DialogButton

/**
 * Renders a stage's dialog buttons as a vertical list of TV buttons. The button marked
 * [DialogButton.isPrimary] (or else the first enabled one) receives focus whenever the button set
 * changes, so OK on the remote runs the stage's main action.
 */
@Composable
internal fun TvDialogButtons(buttons: List<DialogButton>, modifier: Modifier = Modifier) {
    val primaryIndex = buttons.indexOfFirst { it.isPrimary && it.enabled }
        .takeIf { it >= 0 } ?: buttons.indexOfFirst { it.enabled }
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(buttons.map { it.text }) {
        if (primaryIndex >= 0) primaryFocusRequester.requestFocus()
    }

    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        buttons.forEachIndexed { index, button ->
            val isPrimary = index == primaryIndex
            Button(
                onClick = button.onClick,
                onLongClick = button.onLongClick,
                enabled = button.enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isPrimary) Modifier.focusRequester(primaryFocusRequester) else Modifier),
                scale = ButtonDefaults.scale(focusedScale = 1.04f),
                colors = ButtonDefaults.colors(
                    containerColor = if (isPrimary) colorScheme.primaryContainer else colorScheme.surfaceVariant,
                    contentColor = if (isPrimary) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    focusedContainerColor = colorScheme.onSurface,
                    focusedContentColor = colorScheme.surface,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Text(
                    text = button.text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
