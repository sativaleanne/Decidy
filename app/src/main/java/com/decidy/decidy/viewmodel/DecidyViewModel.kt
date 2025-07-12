package com.decidy.decidy.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.decidy.decidy.domain.model.Choice
import com.decidy.decidy.domain.usecase.RemoveChoice
import com.decidy.decidy.domain.usecase.SpinWheel

class DecidyViewModel(
    private val removeChoice: RemoveChoice = RemoveChoice(),
    private val spinWheel: SpinWheel = SpinWheel()
) : ViewModel() {

    var choice by mutableStateOf("")
        private set

    var showAlert by mutableStateOf(false)

    private val _options = mutableStateListOf<Choice>()
    val options: List<Choice> get() = _options

    private val _choices = mutableStateListOf<Choice>()
    val choices: List<Choice> get() = _choices

    var selectedIndex by mutableStateOf<Int?>(null)
    var isSpinning by mutableStateOf(false)
        private set

    val canAdd get() = choice.isNotEmpty()
    val canPick get() = _options.isNotEmpty()

    fun updateChoice(input: String) {
        choice = input
    }

    fun add() {
        val color = defaultColors[_options.size % defaultColors.size]
        val newChoice = Choice(label = choice, color = color)
        _options.add(newChoice)
    }

    fun remove(choice: Choice) {
        val (updatedChoices, updatedOptions) = removeChoice(choice, _options, _choices)
        _choices.clear()
        _choices.addAll(updatedChoices)
        _options.clear()
        _options.addAll(updatedOptions)
    }

    fun clearPage() {
        _options.clear()
        _choices.clear()
    }

    fun spinWheel() {
        isSpinning = true
    }

    fun stopSpin(winningIndex: Int) {
        selectedIndex = winningIndex
        isSpinning = false
        val (newChoices, updatedOptions) = spinWheel(_options, winningIndex)
        _choices.addAll(newChoices)
        _options.clear()
        _options.addAll(updatedOptions)
    }

    fun updateWeight(index: Int, newWeight: Float) {
        if (index in _options.indices) {
            val current = _options[index]
            _options[index] = current.copy(weight = newWeight)
        }
    }

    private val defaultColors = listOf(
        Color(0xFFf28b82), Color(0xFFfbbc04), Color(0xFFfff475),
        Color(0xFFccff90), Color(0xFFa7ffeb), Color(0xFFcbf0f8),
        Color(0xFFaecbfa), Color(0xFFd7aefb), Color(0xFFfdcfe8)
    )
}