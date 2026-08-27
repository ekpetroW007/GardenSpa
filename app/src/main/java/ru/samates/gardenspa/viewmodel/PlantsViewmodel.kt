package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.domain.GeneratedCareProgram
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.NO_REMAINING_CARE_MESSAGE
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

    fun deletePlantCard(plant: PlantEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePlantCard(plant.resolvedCardId)
                onDeleted()
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
        repeatCount: Int? = null,
        reminderDaysBefore: Int = 1
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
                    reminderDaysBefore = reminderDaysBefore,
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
        reminderDaysBefore: Int = 1,
        photoUri: String? = null,
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
                        reminderDaysBefore = reminderDaysBefore,
                        plantCardId = cardId,
                        programId = previous?.programId,
                        programVersion = previous?.programVersion,
                        programStepId = previous?.programStepId,
                        programImportKey = previous?.programImportKey,
                        programNote = previous?.programNote.orEmpty(),
                        userLockedDate = previous?.programId != null || previous?.userLockedDate == true,
                        photoUri = photoUri ?: previous?.photoUri
                    )
                }

                repository.replacePlantCard(cardId, cardRows)
                onSaved()
            } catch (e: Exception) {
                Log.d("savePlantCard", e.toString())
            }
        }
    }

    fun importCareProgram(
        program: GeneratedCareProgram,
        gardenId: Int?,
        gardenName: String,
        reminderDaysBefore: Int = 1,
        photoUri: String? = null,
        onSaved: (List<String>) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                require(program.steps.isNotEmpty()) { NO_REMAINING_CARE_MESSAGE }
                val rows = program.steps.map { step ->
                    val recurrence = step.recurrence
                    PlantEntity(
                        plantName = program.plantName,
                        taskName = step.title,
                        wateringInterval = recurrence?.interval ?: 1,
                        creationDate = step.scheduledDate.toString(),
                        drugId = null,
                        gardenId = gardenId,
                        drugName = step.productDescription ?: NO_DRUG_REQUIRED_LABEL,
                        gardenName = gardenName,
                        repeatType = recurrence?.type?.name ?: RepeatType.NONE.name,
                        repeatInterval = recurrence?.interval ?: 1,
                        repeatDaysOfWeek = if (recurrence?.type == RepeatType.WEEKLY) {
                            step.scheduledDate.dayOfWeek.value.toString()
                        } else {
                            ""
                        },
                        repeatEndType = if (recurrence == null) "NEVER" else "COUNT",
                        repeatCount = recurrence?.count,
                        reminderDaysBefore = reminderDaysBefore,
                        plantCardId = program.instanceId,
                        programId = program.templateId,
                        programVersion = program.templateVersion,
                        programStepId = step.templateStepId,
                        programImportKey = "${program.instanceId}:${program.templateId}:v${program.templateVersion}:${step.templateStepId}",
                        programNote = listOfNotNull(
                            step.productDescription?.let { "Категория средства: $it" },
                            step.note,
                            step.explanation
                        ).filter(String::isNotBlank).joinToString("\n"),
                        userLockedDate = false,
                        photoUri = photoUri
                    )
                }
                repository.replacePlantCard(program.instanceId, rows)
                onSaved(rows.map { it.taskName })
            } catch (e: Exception) {
                Log.d("importCareProgram", e.toString())
                onError(e.message ?: "Не удалось добавить программу")
            }
        }
    }

    fun updateImportedProgramCard(
        plantName: String,
        existingRows: List<PlantEntity>,
        taskNames: List<String>,
        taskDates: List<LocalDate>,
        gardenId: Int?,
        gardenName: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val first = existingRows.firstOrNull() ?: return@launch
                if (taskNames.size != existingRows.size || taskDates.size != existingRows.size) return@launch
                val updated = existingRows.mapIndexed { index, row ->
                    row.copy(
                        plantName = plantName.trim(),
                        taskName = taskNames[index].trim(),
                        creationDate = taskDates[index].toString(),
                        gardenId = gardenId,
                        gardenName = gardenName,
                        repeatDaysOfWeek = if (row.repeatType == RepeatType.WEEKLY.name) {
                            taskDates[index].dayOfWeek.value.toString()
                        } else {
                            row.repeatDaysOfWeek
                        },
                        userLockedDate = true
                    )
                }
                repository.replacePlantCard(first.resolvedCardId, updated)
                onSaved()
            } catch (e: Exception) {
                Log.d("updateImportedProgramCard", e.toString())
            }
        }
    }

}
