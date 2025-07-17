package com.decidy.decidy.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DecidyViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecidyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DecidyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}