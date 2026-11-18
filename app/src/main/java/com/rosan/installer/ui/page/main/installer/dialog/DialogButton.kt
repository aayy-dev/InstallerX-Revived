// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.dialog

data class DialogButton(
    val text: String,
    val weight: Float = 1f,
    val enabled: Boolean = true,
    /** The button remote-driven UIs focus when the stage opens; the first enabled one if none. */
    val isDefaultFocus: Boolean = false,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)
