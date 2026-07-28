package ru.samates.gardenspa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.samates.gardenspa.data.repository.BookeeperRepository

class TasksViewmodelFactory(
    private val repository: BookeeperRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewmodel::class.java)) {
            return TasksViewmodel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}