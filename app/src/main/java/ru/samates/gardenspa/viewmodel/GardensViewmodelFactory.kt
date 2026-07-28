package ru.samates.gardenspa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.samates.gardenspa.data.repository.BookeeperRepository

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