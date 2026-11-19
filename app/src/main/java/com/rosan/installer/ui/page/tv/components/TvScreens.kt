// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Full-screen dark background with a soft glow in [accent] from the top-right corner, like the
 * Play Store's app pages tinting the page from the app's artwork.
 */
@Composable
fun TvBackdrop(accent: Color?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val base = MaterialTheme.colorScheme.background
    val glow = accent ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.85f,
                    ),
                )
            },
        content = content,
    )
}

/** An app icon on a rounded tile, or [fallback] when the package has no icon yet. */
@Composable
fun TvAppIcon(bitmap: ImageBitmap?, size: Dp, modifier: Modifier = Modifier, fallback: ImageVector? = null) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else if (fallback != null) {
            Icon(fallback, contentDescription = null, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/**
 * Centered confirmation layout used by Google TV for destructive or blocking questions: a round badge,
 * a title, a short description and a row of pill buttons.
 */
@Composable
fun TvConfirmScreen(
    title: String,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    badge: (@Composable () -> Unit)? = null,
    description: String? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    TvBackdrop(accent = accent, modifier = modifier) {
        // Scrolls when the content is taller than the screen (a long app name plus options), so the
        // buttons are never squeezed out; focus brings them into view.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TvKit.SafeHorizontal, vertical = TvKit.SafeVertical),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) { badge() }
                Spacer(Modifier.height(28.dp))
            }
            Text(
                text = title,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = description,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (extra != null) {
                Spacer(Modifier.height(18.dp))
                extra()
            }
            Spacer(Modifier.height(36.dp))
            actions()
        }
    }
}

/** A thin rounded progress bar; [progress] below 0 shows an indeterminate sweep. */
@Composable
fun TvProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fill = tvFocusedContainer()
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "tv_progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        if (progress >= 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(fill),
            )
        } else {
            IndeterminateSweep(fill)
        }
    }
}

@Composable
private fun IndeterminateSweep(color: Color) {
    val transition = rememberInfiniteTransition(label = "tv_sweep")
    val fraction by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200)),
        label = "tv_sweep_fraction",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val trackWidth = constraints.maxWidth
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
                .offset { IntOffset((trackWidth * fraction).toInt(), 0) }
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** A full-screen layout that keeps content inside the TV safe area. */
@Composable
fun TvSafeColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TvKit.SafeHorizontal, vertical = TvKit.SafeVertical),
        verticalArrangement = Arrangement.Top,
    ) { content() }
}
