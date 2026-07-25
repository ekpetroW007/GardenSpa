package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.repository.BookeeperRepository

class GardensViewmodelFactory(
    private val repository: BookeeperRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GardensViewmodel::class.java)) {
            return GardensViewmodel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}