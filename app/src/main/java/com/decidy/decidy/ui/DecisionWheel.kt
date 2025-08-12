package com.decidy.decidy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import com.decidy.decidy.domain.model.Choice
import com.decidy.decidy.viewmodel.WheelUiState
import kotlin.math.*

@Composable
fun DecisionWheel(
    state: WheelUiState,
    isSpinning: Boolean,
    onSpinEnd: (Int) -> Unit,
    onWeightChangeById: (id: String, newWeight: Float) -> Unit,
    onToggleChoiceById: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val choices = state.choices
    val expandedId = state.expandedId

    // Only the unchosen slices are visible & interactive
    val visibleChoices = remember(choices) { choices.filter { !it.chosen } }

    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }

    // Restart gesture scopes when selection/weights change
    val weightsKey = remember(visibleChoices) { visibleChoices.map { it.id to it.weight } }

    //Tap to expand/collapse
    val tapModifier = Modifier.pointerInput(expandedId, weightsKey) {
        detectTapGestures { pos ->
            val id = sliceIdAtTouch(
                pos,
                size.width.toFloat(),
                size.height.toFloat(),
                rotation.value,
                visibleChoices
            )
            if (id != null) onToggleChoiceById(id)
        }
    }

    //Drag ONLY when a slice has been tapped/expanded
    val dragModifier = Modifier.pointerInput(expandedId, weightsKey, isSpinning) {
        if (expandedId == null) return@pointerInput  // require tap first
        var last: Offset? = null
        detectDragGestures(
            onDragStart = { last = it },
            onDragCancel = { last = null },
            onDragEnd = { last = null },
            onDrag = { change, _ ->
                // If user collapses mid-gesture, ignore
                val targetId = state.expandedId ?: return@detectDragGestures
                val center = Offset(size.width / 2f, size.height / 2f)
                val prev = last ?: change.previousPosition
                val a1 = angleDeg(prev, center)
                val a2 = angleDeg(change.position, center)
                var d = a2 - a1
                if (d > 180f) d -= 360f
                if (d < -180f) d += 360f

                val total = visibleChoices
                    .sumOf { it.weight.toDouble() }
                    .toFloat()
                    .coerceAtLeast(0.0001f)

                val current = visibleChoices.firstOrNull { it.id == targetId }?.weight
                    ?: return@detectDragGestures

                val newWeight = current + (d / 360f) * total
                onWeightChangeById(targetId, newWeight)

                last = change.position
                change.consume()
            }
        )
    }

    // spin logic uses visibleChoices so winner index matches active list
    LaunchedEffect(isSpinning, visibleChoices) {
        if (isSpinning && !spinning && visibleChoices.isNotEmpty()) {
            val targetRotation = rotation.value + (720..1440).random()
            spinning = true
            rotation.animateTo(
                targetRotation,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )

            val total = visibleChoices.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
            val angles = visibleChoices.map { it.weight / total * 360f }
            val normalized = (rotation.value % 360f + 360f) % 360f
            val finalAngle = (normalized + 90f) % 360f
            onSpinEnd(findWinningIndex(finalAngle, angles))
            spinning = false
        }
    }

    // drawing uses only visibleChoices
    Canvas(modifier = modifier.then(tapModifier).then(dragModifier)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val total = visibleChoices.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        val angles = visibleChoices.map { it.weight / total * 360f }

        rotate(rotation.value, pivot = center) {
            var start = 0f
            for ((i, c) in visibleChoices.withIndex()) {
                val sweep = angles[i]
                val isSelected = c.id == expandedId
                val mid = start + sweep / 2f
                val rad = Math.toRadians(mid.toDouble())
                val bump = if (isSelected) 20f else 0f
                val dx = bump * cos(rad).toFloat()
                val dy = bump * sin(rad).toFloat()

                drawArc(
                    color = c.color.takeOrElse { Color.hsl((i * 40f) % 360f, 0.6f, 0.7f) },
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - radius + dx, center.y - radius + dy),
                    size = Size(radius * 2f, radius * 2f)
                )

                val textX = center.x + radius * 0.6f * cos(rad).toFloat()
                val textY = center.y + radius * 0.6f * sin(rad).toFloat()
                drawIntoCanvas { canvas ->
                    val p = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val wp = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.save()
                    val rotateText = if (mid % 360f in 90f..270f) mid + 180f else mid
                    canvas.nativeCanvas.rotate(rotateText, textX, textY)
                    canvas.nativeCanvas.drawText(c.label, textX, textY - 16f, p)
                    val percent = (c.weight / total * 100).roundToInt()
                    canvas.nativeCanvas.drawText("$percent%", textX, textY + 16f, wp)
                    canvas.nativeCanvas.restore()
                }

                start += sweep
            }
        }

        // pointer
        drawPath(
            Path().apply {
                moveTo(center.x, center.y - radius - 10)
                lineTo(center.x - 20, center.y - radius + 30)
                lineTo(center.x + 20, center.y - radius + 30)
                close()
            },
            color = Color.Black
        )
    }
}


private fun angleDeg(p: Offset, c: Offset): Float =
    Math.toDegrees(kotlin.math.atan2((p.y - c.y), (p.x - c.x)).toDouble()).toFloat()

private fun sliceIdAtTouch(
    pos: Offset,
    w: Float,
    h: Float,
    rotationDegrees: Float,
    choices: List<Choice>
): String? {
    if (choices.isEmpty()) return null
    val center = Offset(w / 2f, h / 2f)
    val dx = pos.x - center.x
    val dy = pos.y - center.y

    val normalizedRotation = (rotationDegrees % 360f + 360f) % 360f
    val touchAngle = ((kotlin.math.atan2(dy, dx) * (180f / Math.PI.toFloat())) - normalizedRotation + 360f) % 360f

    val total = choices.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    var start = 0f
    for (c in choices) {
        val sweep = c.weight / total * 360f
        val end = start + sweep
        if (touchAngle in start..end) return c.id
        start = end
    }
    return choices.last().id
}

private fun findWinningIndex(angle: Float, angles: List<Float>): Int {
    var cum = 0f
    for ((i, s) in angles.withIndex()) {
        cum += s
        if (angle <= cum) return i
    }
    return angles.lastIndex
}








