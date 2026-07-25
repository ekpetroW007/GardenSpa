package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.entity.GardenEntity
import com.example.myapplication.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GardensViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {

    val gardens = repository.allGardens.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun gardenAdd(name: String) {
        viewModelScope.launch {
            try {
                val newGarden = GardenEntity(
                    name = name
                )
                repository.insertGarden(newGarden)
            } catch (e: Exception) {
                Log.d("addGarden", e.toString())
            }
        }
    }

    fun deleteGarden(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteGarden(id)
            } catch (e: Exception) {
                Log.d("deleteGarden", e.toString())
            }
        }
    }
}