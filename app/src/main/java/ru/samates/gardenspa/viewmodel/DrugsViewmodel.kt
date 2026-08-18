package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.samates.gardenspa.domain.FolkFertilizerRecipe

class DrugsViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {
    val drugs = repository.allDrugs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val recipes = repository.allRecipes
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

    fun updateDrug(
        id: Int,
        name: String,
        purpose: String,
        consumptionRate: String,
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateDrug(
                    DrugEntity(
                        id = id,
                        name = name,
                        purpose = purpose,
                        consumptionRate = consumptionRate
                    )
                )
                onUpdated()
            } catch (e: Exception) {
                Log.d("updateDrug", e.toString())
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

    fun updateRecipe(recipe: FolkFertilizerRecipe) {
        viewModelScope.launch {
            try {
                repository.updateRecipe(recipe)
            } catch (e: Exception) {
                Log.d("updateRecipe", e.toString())
            }
        }
    }

    fun deleteRecipe(recipe: FolkFertilizerRecipe) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipe)
            } catch (e: Exception) {
                Log.d("deleteRecipe", e.toString())
            }
        }
    }
}
