package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.domain.RepeatType
import java.time.LocalDate
import java.util.UUID
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

    fun deletePlantCard(plant: PlantEntity) {
        viewModelScope.launch {
            try {
                repository.deletePlantCard(plant.resolvedCardId)
            } catch (e: Exception) {
                Log.d("deletePlantCard", e.toString())
            }
        }
    }

    fun updateFertilizingPeriod(
        cardPlants: List<PlantEntity>,
        repeatType: RepeatType,
        repeatInterval: Int,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val plant = cardPlants.firstOrNull() ?: return@launch
                val weeklyDay = runCatching {
                    LocalDate.parse(plant.creationDate).dayOfWeek.value.toString()
                }.getOrDefault("")
                val updatedRows = cardPlants.map { row ->
                    row.copy(
                        wateringInterval = repeatInterval.coerceAtLeast(1),
                        repeatType = repeatType.name,
                        repeatInterval = repeatInterval.coerceAtLeast(1),
                        repeatDaysOfWeek = if (repeatType == RepeatType.WEEKLY) {
                            row.repeatDaysOfWeek.ifBlank { weeklyDay }
                        } else {
                            ""
                        },
                        repeatEndType = "NEVER",
                        repeatEndDate = null,
                        repeatCount = null
                    )
                }
                repository.replacePlantCard(plant.resolvedCardId, updatedRows)
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
                    repeatCount = repeatCount,
                    plantCardId = UUID.randomUUID().toString()
                )
                repository.insertPlant(newPlant)
            } catch (e: Exception) {
                Log.d("addPlant", e.toString())
            }
        }
    }

    fun savePlantCard(
        plantId: Int?,
        plantName: String,
        taskNames: List<String>,
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
        repeatCount: Int? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val selectedPlant = plantId?.let { id -> plants.value.firstOrNull { it.id == id } }
                val cardId = selectedPlant?.resolvedCardId ?: UUID.randomUUID().toString()
                val existing = plants.value
                    .filter { it.resolvedCardId == cardId }
                    .sortedBy(PlantEntity::id)
                val normalizedTasks = taskNames.map { it.trim() }.filter { it.isNotBlank() }
                if (normalizedTasks.isEmpty()) return@launch

                val cardRows = normalizedTasks.mapIndexed { index, taskName ->
                    val previous = existing.getOrNull(index)
                    PlantEntity(
                        id = previous?.id ?: 0,
                        plantName = plantName,
                        taskName = taskName,
                        wateringInterval = wateringInterval,
                        creationDate = creationDate,
                        drugId = drugId,
                        gardenId = gardenId,
                        drugName = drugName,
                        gardenName = gardenName,
                        repeatType = repeatType,
                        repeatInterval = repeatInterval.coerceAtLeast(1),
                        repeatDaysOfWeek = repeatDaysOfWeek,
                        repeatEndType = repeatEndType,
                        repeatEndDate = repeatEndDate,
                        repeatCount = repeatCount,
                        plantCardId = cardId
                    )
                }

                repository.replacePlantCard(cardId, cardRows)
                onSaved()
            } catch (e: Exception) {
                Log.d("savePlantCard", e.toString())
            }
        }
    }

}
