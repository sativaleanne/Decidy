package com.decidy.decidy.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.decidy.decidy.domain.model.Choice
import kotlin.math.max

data class WheelUiState(
    val choices: List<Choice>,
    val expandedId: String? = null
)


class DecidyViewModel(application: Application) : AndroidViewModel(application) {

    // Input field
    var choice by mutableStateOf("")
        private set

    var showAlert by mutableStateOf(false)

    // Wheel state
    var wheelUiState by mutableStateOf(WheelUiState(emptyList()))
        private set

    // Spin / dialog state
    var isSpinning by mutableStateOf(false)
        private set

    var selectedIndex by mutableStateOf<Int?>(null)
    var activeChoicesBeforeSpin: List<Choice> = emptyList()
        private set


    // Core colors from icon with contrast-boosted variants
    private val Coral       = Color(0xFFE27D60) // Main coral-orange
    private val DarkCoral   = Color(0xFFB24A3C) // Darker coral for contrast
    private val Teal        = Color(0xFF85C5B8) // Main teal
    private val DeepTeal    = Color(0xFF3B8274) // Darker teal for contrast
    private val PaleMint    = Color(0xFFCCE6DA) // Soft mint
    private val OffWhite    = Color(0xFFF5F5F0) // Light accent

    // Sequence: alternating warm/cool, then light break
    private val defaultColors = listOf(
        Teal,
        Coral,
        DeepTeal,
        DarkCoral,
        PaleMint,
        OffWhite
    )

    private fun reflowColors(list: List<Choice>): List<Choice> =
        list.mapIndexed { i, ch -> ch.copy(color = defaultColors[i % defaultColors.size]) }

    // getters
    val choices: List<Choice> get() = wheelUiState.choices
    val activeChoiceId: String? get() = wheelUiState.expandedId
    val activeChoices: List<Choice> get() = choices.filter { !it.chosen }
    val chosenChoices: List<Choice> get() = choices.filter { it.chosen }

    val canAdd: Boolean get() = choice.isNotEmpty()
    val canPick: Boolean get() = activeChoices.isNotEmpty()

    // ui actions
    fun updateChoice(input: String) { choice = input }

    fun add() {
        if (choice.isBlank()) return
        val updated = wheelUiState.choices + Choice(label = choice, color = Color.Unspecified)
        wheelUiState = wheelUiState.copy(choices = reflowColors(updated))
        choice = ""
    }

    fun remove(target: Choice) {
        val updated = wheelUiState.choices.filterNot { it.id == target.id }
        wheelUiState = wheelUiState.copy(choices = reflowColors(updated))
    }

    fun clearPage() {
        wheelUiState = wheelUiState.copy(choices = emptyList(), expandedId = null)
        selectedIndex = null
        activeChoicesBeforeSpin = emptyList()
    }

    fun resetChosen() {
        val unchosen = choices.map { it.copy(chosen = false) }
        val equalized = unchosen.map { it.copy(weight = 1f) }
        wheelUiState = wheelUiState.copy(choices = equalized, expandedId = null)
        selectedIndex = null
        activeChoicesBeforeSpin = emptyList()
    }

    fun toggleActiveChoice(choice: Choice?) {
        val id = choice?.id
        wheelUiState = wheelUiState.copy(
            expandedId = if (id != null && id == wheelUiState.expandedId) null else id
        )
    }

    fun clearActiveChoice() {
        wheelUiState = wheelUiState.copy(expandedId = null)
    }

    // wheel spin flow
    fun spinWheel() {
        activeChoicesBeforeSpin = activeChoices
        isSpinning = true
        clearActiveChoice()
    }

    fun stopSpin(winningIndex: Int) {
        selectedIndex = winningIndex
        isSpinning = false

        val active = activeChoices
        if (winningIndex in active.indices) {
            val winner = active[winningIndex]
            wheelUiState = wheelUiState.copy(
                choices = choices.map { c ->
                    if (c.id == winner.id) c.copy(chosen = true) else c
                }
            )
        }
    }

    /**
     * Update the weight of a single active slice by id and proportionally rescale others,
     * keeping the total of active weights constant and each above a minimum.
     */
    fun updateWeightById(id: String, target: Float) {
        val active = activeChoices
        if (active.isEmpty()) return

        val minEach = 0.02f
        val total = active.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)

        val others = active.filter { it.id != id }
        if (others.isEmpty()) {
            // Only one active slice
            wheelUiState = wheelUiState.copy(
                choices = choices.map { if (it.id == id) it.copy(weight = total) else it }
            )
            return
        }

        // Clamp so others can have at least minEach
        val maxForTarget = total - minEach * others.size
        val clamped = target.coerceIn(minEach, maxForTarget)

        val remaining = total - clamped
        val sumOthers = others.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)

        val newWeights = buildMap {
            put(id, clamped)
            others.forEach { c ->
                val scaled = (c.weight / sumOthers) * remaining
                put(c.id, max(minEach, scaled))
            }
        }

        // Normalize to fix rounding
        val norm = newWeights.values.sum()
        val factor = total / norm

        wheelUiState = wheelUiState.copy(
            choices = choices.map { c ->
                if (c.chosen) c
                else {
                    val w = newWeights[c.id]
                    if (w != null) c.copy(weight = w * factor) else c
                }
            }
        )
    }

    /**
     * Backwards-compat overload used by MainView/DecisionWheel that send index within activeChoices.
     */
    fun updateWeight(indexInActive: Int, newWeight: Float) {
        val active = activeChoices
        if (indexInActive !in active.indices) return
        updateWeightById(active[indexInActive].id, newWeight)
    }
}

