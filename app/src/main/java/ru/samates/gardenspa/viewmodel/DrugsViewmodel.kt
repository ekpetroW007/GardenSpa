package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class DrugsViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {
    val drugs = repository.allDrugs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addDrug(name: String, purpose: String, consumptionRate: String, applicationMethod: String = "UNSPECIFIED",
        onSaved: (DrugEntity) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val newDrug = DrugEntity(
                    name = name,
                    purpose = purpose,
                    consumptionRate = consumptionRate,
                    applicationMethod = applicationMethod
                )
                onSaved(repository.saveToMyProducts(newDrug))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось сохранить средство")
            }
        }
    }

    fun updateDrug(
        id: Int,
        name: String,
        purpose: String,
        consumptionRate: String,
        applicationMethod: String = "UNSPECIFIED",
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateDrug(
                    DrugEntity(
                        id = id,
                        name = name,
                        purpose = purpose,
                        consumptionRate = consumptionRate,
                        applicationMethod = applicationMethod
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
}
