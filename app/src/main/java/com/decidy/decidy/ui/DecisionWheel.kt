package com.decidy.decidy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.decidy.decidy.domain.model.Choice
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun DecisionWheel(
    choices: List<Choice>,
    isSpinning: Boolean,
    onSpinEnd: (Int) -> Unit,
    onWeightChange: (index: Int, newWeight: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var selectedSegmentIndex by remember { mutableStateOf<Int?>(null) }

    val minWeight = 0.1f
    val sensitivityFactor = 0.01f

    val gestureModifier = Modifier.pointerInput(choices, isSpinning) {
        detectDragGestures(
            onDragStart = { offset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val dx = offset.x - center.x
                val dy = offset.y - center.y
                val touchAngle = ((atan2(dy, dx).toDegrees() + 360f - rotation.value) % 360f)

                val totalWeight = choices.sumOf { it.weight.toDouble() }.toFloat()
                val angles = choices.map { it.weight / totalWeight * 360f }

                var startAngle = 0f
                for ((index, angle) in angles.withIndex()) {
                    val endAngle = startAngle + angle
                    if (touchAngle in startAngle..endAngle) {
                        selectedSegmentIndex = index
                        break
                    }
                    startAngle = endAngle
                }
            },
            onDrag = { _, dragAmount ->
                selectedSegmentIndex?.let { i ->
                    val deltaWeight = dragAmount.x * sensitivityFactor
                    val newWeight = (choices[i].weight + deltaWeight).coerceAtLeast(minWeight)
                    onWeightChange(i, newWeight)
                }
            },
            onDragEnd = {
                selectedSegmentIndex = null
            }
        )
    }

    LaunchedEffect(isSpinning) {
        if (isSpinning && !spinning && choices.isNotEmpty()) {
            spinning = true
            val targetRotation = rotation.value + (720..1440).random()
            rotation.animateTo(
                targetRotation,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )
            val totalWeight = choices.sumOf { it.weight.toDouble() }.toFloat()
            val angles = choices.map { it.weight / totalWeight * 360f }
            val finalAngle = (270f - (rotation.value % 360f) + 360f) % 360f
            val winningIndex = findWinningIndex(finalAngle, angles)
            onSpinEnd(winningIndex)
            spinning = false
        }
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val totalWeight = choices.sumOf { it.weight.toDouble() }.toFloat()
        val angles = choices.map { it.weight / totalWeight * 360f }

        rotate(rotation.value, pivot = center) {
            var startAngle = 0f
            for ((i, choice) in choices.withIndex()) {
                val sweepAngle = angles[i]

                drawArc(
                    color = Color.hsl((i * 40f) % 360f, 0.6f, 0.7f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f)
                )

                val midAngle = startAngle + sweepAngle / 2f
                val angleRad = Math.toRadians(midAngle.toDouble())
                val textX = center.x + radius * 0.6f * cos(angleRad).toFloat()
                val textY = center.y + radius * 0.6f * sin(angleRad).toFloat()

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val weightPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.save()
                    val rotateAngle = if (midAngle % 360f in 90f..270f) midAngle + 180f else midAngle
                    canvas.nativeCanvas.rotate(rotateAngle, textX, textY)
                    canvas.nativeCanvas.drawText(choice.label, textX, textY - 16f, paint)
                    val percent = (choice.weight / totalWeight * 100).roundToInt()
                    canvas.nativeCanvas.drawText("$percent%", textX, textY + 16f, weightPaint)
                    canvas.nativeCanvas.restore()
                }

                startAngle += sweepAngle
            }
        }

        drawPath(
            path = Path().apply {
                moveTo(center.x, center.y - radius - 10)
                lineTo(center.x - 20, center.y - radius + 30)
                lineTo(center.x + 20, center.y - radius + 30)
                close()
            },
            color = Color.Black
        )
    }
}

private fun findWinningIndex(angle: Float, angles: List<Float>): Int {
    var cumulative = 0f
    for ((index, slice) in angles.withIndex()) {
        cumulative += slice
        if (angle <= cumulative) return index
    }
    return angles.lastIndex
}

private fun atan2(y: Float, x: Float): Float = kotlin.math.atan2(y, x)
private fun Float.toDegrees(): Float = this * (180f / Math.PI.toFloat())







