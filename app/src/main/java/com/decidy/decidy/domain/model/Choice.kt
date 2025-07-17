package com.decidy.decidy.domain.model

import androidx.compose.ui.graphics.Color

data class Choice(
    val label: String,
    val weight: Float = 1f,
    val color: Color = Color.Unspecified
)