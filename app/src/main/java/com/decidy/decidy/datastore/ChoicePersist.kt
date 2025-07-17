package com.decidy.decidy.datastore

import androidx.compose.ui.graphics.Color
import com.decidy.decidy.domain.model.Choice

data class ChoicePersist(
    val label: String,
    val weight: Float
)


fun ChoicePersist.toDomain(color: Color): Choice =
    Choice(label = label, weight = weight, color = color)
