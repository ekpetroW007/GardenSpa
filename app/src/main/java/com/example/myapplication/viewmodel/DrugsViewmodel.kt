package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.entity.DrugEntity
import com.example.myapplication.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrugsViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {
    val drugs = repository.allDrugs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addDrug(name: String, purpose: String, consumptionRate: String) {
        viewModelScope.launch {
            try {
                val newDrug = DrugEntity(
                    name = name,
                    purpose = purpose,
                    consumptionRate = consumptionRate
                )
                repository.insertDrug(newDrug)
            } catch (e: Exception) {
                Log.d("addDrug", e.toString())
            }
        }
    }

    fun deleteDrug(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteDrug(id)
            } catch (e: Exception) {
                Log.d("deleteDrug", e.toString())
            }
        }
    }
}