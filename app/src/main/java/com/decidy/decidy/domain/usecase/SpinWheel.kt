package com.decidy.decidy.domain.usecase

import com.decidy.decidy.domain.model.Choice

class SpinWheel {
    operator fun invoke(options: List<Choice>, winningIndex: Int): Pair<List<Choice>, List<Choice>> {
        if (winningIndex !in options.indices) return emptyList<Choice>() to options
        val winningChoice = options[winningIndex]
        val newChoices = listOf(winningChoice)
        val newOptions = options.toMutableList().apply { removeAt(winningIndex) }
        return newChoices to newOptions
    }
}