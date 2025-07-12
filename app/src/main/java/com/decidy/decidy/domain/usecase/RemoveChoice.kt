package com.decidy.decidy.domain.usecase

import com.decidy.decidy.domain.model.Choice

class RemoveChoice {
    operator fun invoke(choice: Choice, options: MutableList<Choice>, choices: MutableList<Choice>): Pair<List<Choice>, List<Choice>> {
        return if (choices.contains(choice)) {
            choices - choice to options
        } else {
            choices to options - choice
        }
    }
}