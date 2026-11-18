// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

/**
 * Shared building blocks for the TV layouts, modeled on Google TV's own settings and store apps:
 * borderless rows that turn into a light pill when focused, pill buttons, and uppercase section labels.
 */
object TvKit {
    val SafeHorizontal = 58.dp
    val SafeVertical = 36.dp
    val PillShape = RoundedCornerShape(percent = 50)
}

/** Light, primary-tinted fill used for every focused element. */
@Composable
@ReadOnlyComposable
fun tvFocusedContainer(): Color = lerp(MaterialTheme.colorScheme.primary, Color.White, 0.72f)

/** Dark content color drawn on [tvFocusedContainer]. */
@Composable
@ReadOnlyComposable
fun tvFocusedContent(): Color = lerp(MaterialTheme.colorScheme.background, Color.Black, 0.4f)

@Composable
fun TvPageTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TvSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 26.dp, top = 22.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * A full-width focusable list row. Informational rows still take focus (with a no-op click) so long
 * pages stay scrollable with a remote, as in the system settings app.
 */
@Composable
fun TvRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onFocused: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable (focused: Boolean) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused?.invoke() }

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = TvKit.PillShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = tvFocusedContainer(),
            focusedContentColor = tvFocusedContent(),
            pressedContainerColor = tvFocusedContainer(),
            pressedContentColor = tvFocusedContent(),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp)
                .padding(start = if (icon != null) 14.dp else 26.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                val badge = if (focused) {
                    lerp(MaterialTheme.colorScheme.primary, Color.Black, 0.55f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badge),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (focused) tvFocusedContainer() else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = LocalContentColor.current.copy(alpha = 0.72f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(16.dp))
                trailing(focused)
            }
        }
    }
}

@Composable
fun TvSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onFocused: (() -> Unit)? = null,
) {
    TvRow(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onFocused = onFocused,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        onClick = { onCheckedChange(!checked) },
    )
}

@Composable
fun TvRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onFocused: (() -> Unit)? = null,
) {
    TvRow(
        title = title,
        summary = summary,
        modifier = modifier,
        onFocused = onFocused,
        trailing = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
    )
}

/**
 * A pill button as used on Google TV confirmation screens and store pages: dark until focused,
 * then filled with [tvFocusedContainer].
 */
@Composable
fun TvPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonDefaults.shape(shape = TvKit.PillShape),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = tvFocusedContainer(),
            focusedContentColor = tvFocusedContent(),
            pressedContainerColor = tvFocusedContainer(),
            pressedContentColor = tvFocusedContent(),
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 34.dp, vertical = 14.dp),
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** One action shown by [TvActionRow]. */
data class TvAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
)

/**
 * A horizontal row of pill buttons. The default action (or the first enabled one) takes focus when the
 * set of actions changes, and again after all actions were disabled and come back.
 */
@Composable
fun TvActionRow(actions: List<TvAction>, modifier: Modifier = Modifier, requestInitialFocus: Boolean = true) {
    val defaultIndex = actions.indexOfFirst { it.isDefault && it.enabled }
        .takeIf { it >= 0 } ?: actions.indexOfFirst { it.enabled }
    val defaultFocusRequester = remember { FocusRequester() }
    LaunchedEffect(actions.map { it.text }, actions.any { it.enabled }) {
        if (requestInitialFocus && defaultIndex >= 0) defaultFocusRequester.requestFocus()
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        actions.forEachIndexed { index, action ->
            TvPillButton(
                text = action.text,
                onClick = action.onClick,
                onLongClick = action.onLongClick,
                enabled = action.enabled,
                modifier = if (index == defaultIndex) Modifier.focusRequester(defaultFocusRequester) else Modifier,
            )
        }
    }
}

/**
 * Long text that the remote can scroll: the block takes focus, Up/Down scroll it, and at either end
 * the key is released so focus can move on.
 */
@Composable
fun TvScrollableText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (focused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> (scrollState.value < scrollState.maxValue).also { canScroll ->
                        if (canScroll) scope.launch { scrollState.animateScrollBy(160f) }
                    }

                    Key.DirectionUp -> (scrollState.value > 0).also { canScroll ->
                        if (canScroll) scope.launch { scrollState.animateScrollBy(-160f) }
                    }

                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .verticalScroll(scrollState)
            .padding(20.dp),
    ) {
        Text(text = text, color = color, fontSize = 16.sp, lineHeight = 24.sp)
    }
}

/** Calls [onFocused] whenever focus enters this element or one of its children. */
fun Modifier.onFocusEntered(onFocused: () -> Unit): Modifier = onFocusChanged { if (it.hasFocus) onFocused() }

/**
 * A [FocusRequester] that takes focus once the content is laid out, for screens that open on a
 * list rather than on their buttons. Attach it to a list (focus enters its first row) or to a row.
 */
@Composable
fun rememberInitialFocus(vararg keys: Any?): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(*keys) {
        withFrameNanos { }
        runCatching { requester.requestFocus() }
    }
    return requester
}
