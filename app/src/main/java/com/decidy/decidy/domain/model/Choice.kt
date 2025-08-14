package com.decidy.decidy.domain.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class Choice(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val weight: Float = 1f,
    val color: Color = Color.Unspecified,
    val chosen: Boolean = false
)