package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.repository.BookeeperRepository

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