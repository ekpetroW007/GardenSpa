package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.domain.RepeatType
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlantsViewmodel(private val repository: BookeeperRepository) : ViewModel() {
    val plants = repository.allPlants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun deletePlant(id: Int) {
        viewModelScope.launch {
            try {
                repository.deletePlant(id)
            } catch (e: Exception) {
                Log.d("deletePlant", e.toString())
            }
        }
    }

    fun updateFertilizingPeriod(
        plant: PlantEntity,
        repeatType: RepeatType,
        repeatInterval: Int,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val weeklyDay = runCatching {
                    LocalDate.parse(plant.creationDate).dayOfWeek.value.toString()
                }.getOrDefault("")
                repository.insertPlant(
                    plant.copy(
                        wateringInterval = repeatInterval.coerceAtLeast(1),
                        repeatType = repeatType.name,
                        repeatInterval = repeatInterval.coerceAtLeast(1),
                        repeatDaysOfWeek = if (repeatType == RepeatType.WEEKLY) {
                            plant.repeatDaysOfWeek.ifBlank { weeklyDay }
                        } else {
                            ""
                        },
                        repeatEndType = "NEVER",
                        repeatEndDate = null,
                        repeatCount = null
                    )
                )
                onSaved()
            } catch (e: Exception) {
                Log.d("updateFertilizingPeriod", e.toString())
            }
        }
    }

    fun addPlant(
        plantName: String,
        taskName: String,
        wateringInterval: Int,
        creationDate: String,
        drugId: Int?,
        gardenId: Int?,
        drugName: String,
        gardenName: String,
        repeatType: String = "NONE",
        repeatInterval: Int = 1,
        repeatDaysOfWeek: String = "",
        repeatEndType: String = "NEVER",
        repeatEndDate: String? = null,
        repeatCount: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val newPlant = PlantEntity(
                    plantName = plantName,
                    wateringInterval = wateringInterval,
                    creationDate = creationDate,
                    taskName = taskName,
                    drugId = drugId,
                    gardenId = gardenId,
                    drugName = drugName,
                    gardenName = gardenName,
                    repeatType = repeatType,
                    repeatInterval = repeatInterval.coerceAtLeast(1),
                    repeatDaysOfWeek = repeatDaysOfWeek,
                    repeatEndType = repeatEndType,
                    repeatEndDate = repeatEndDate,
                    repeatCount = repeatCount
                )
                repository.insertPlant(newPlant)
            } catch (e: Exception) {
                Log.d("addPlant", e.toString())
            }
        }
    }

}
