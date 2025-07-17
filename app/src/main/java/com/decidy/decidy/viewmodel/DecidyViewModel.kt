package com.decidy.decidy.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.decidy.decidy.datastore.ChoicePersist
import com.decidy.decidy.datastore.ChoiceStorage
import com.decidy.decidy.datastore.toDomain
import com.decidy.decidy.domain.model.Choice
import com.decidy.decidy.domain.usecase.RemoveChoice
import com.decidy.decidy.domain.usecase.SpinWheel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DecidyViewModel(
    app: Application,
    private val removeChoice: RemoveChoice = RemoveChoice(),
    private val spinWheel: SpinWheel = SpinWheel()
) : AndroidViewModel(app) {

    private val context = app.applicationContext

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

    private val defaultColors = listOf(
        Color(0xFFf28b82), Color(0xFFfbbc04), Color(0xFFfff475),
        Color(0xFFccff90), Color(0xFFa7ffeb), Color(0xFFcbf0f8),
        Color(0xFFaecbfa), Color(0xFFd7aefb), Color(0xFFfdcfe8)
    )

    init {
        ChoiceStorage.read(context).onEach { persisted ->
            _options.clear()
            _options.addAll(
                persisted.mapIndexed { index, it ->
                    it.toDomain(color = defaultColors[index % defaultColors.size])
                }
            )
        }.launchIn(viewModelScope)
    }

    fun updateChoice(input: String) {
        choice = input
    }

    fun add() {
        val color = defaultColors[_options.size % defaultColors.size]
        val newChoice = Choice(label = choice, color = color)
        _options.add(newChoice)
        persist()
    }

    fun remove(choice: Choice) {
        val (updatedChoices, updatedOptions) = removeChoice(choice, _options, _choices)
        _choices.clear()
        _choices.addAll(updatedChoices)
        _options.clear()
        _options.addAll(updatedOptions)
        persist()
    }

    fun clearPage() {
        _options.clear()
        _choices.clear()
        viewModelScope.launch {
            ChoiceStorage.clear(context)
        }
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
        persist()
    }

    fun updateWeight(index: Int, newWeight: Float) {
        if (index in _options.indices) {
            _options[index] = _options[index].copy(weight = newWeight)
            persist()
        }
    }

    private fun persist() {
        viewModelScope.launch {
            ChoiceStorage.save(
                context,
                _options.map { ChoicePersist(label = it.label, weight = it.weight) }
            )
        }
    }
}
