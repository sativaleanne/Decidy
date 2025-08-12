package com.decidy.decidy.datastore

import androidx.compose.ui.graphics.Color
import com.decidy.decidy.domain.model.Choice

data class ChoicePersist(
    val id: String,
    val label: String,
    val weight: Float,
    val color: Color,
    val chosen: Boolean
)


fun ChoicePersist.toDomain(color: Color): Choice =
    Choice(id = id, label = label, weight = weight, color = color, chosen = chosen)
