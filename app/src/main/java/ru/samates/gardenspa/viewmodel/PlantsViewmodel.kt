package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.domain.PlantTaskDraft
import ru.samates.gardenspa.domain.RepeatType
import ru.samates.gardenspa.domain.GeneratedCareProgram
import ru.samates.gardenspa.domain.NO_DRUG_REQUIRED_LABEL
import ru.samates.gardenspa.domain.NO_REMAINING_CARE_MESSAGE
import ru.samates.gardenspa.domain.CultivationType
import ru.samates.gardenspa.domain.withProgramProduct
import ru.samates.gardenspa.domain.problemTreatment
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.domain.withPersonalProduct
import ru.samates.gardenspa.domain.installedRows
import ru.samates.gardenspa.domain.installation
import ru.samates.gardenspa.domain.EradicationCare
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.data.database.entity.DrugEntity
import kotlinx.coroutines.CancellationException
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
                        repeatCount = null,
                        userEdited = true
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
        tasks: List<PlantTaskDraft>,
        wateringInterval: Int,
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
        drugNote: String? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val selectedPlant = plantId?.let { id -> plants.value.firstOrNull { it.id == id } }
                val cardId = selectedPlant?.resolvedCardId ?: UUID.randomUUID().toString()
                val existing = plants.value
                    .filter { it.resolvedCardId == cardId }
                    .sortedBy(PlantEntity::id)
                val normalizedTasks = tasks.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotBlank() }
                if (normalizedTasks.isEmpty()) return@launch
                if (repeatType != "NONE" && repeatEndType == "UNTIL_DATE" && repeatEndDate != null) {
                    require(normalizedTasks.none { it.startDate.isAfter(LocalDate.parse(repeatEndDate)) })
                }

                val cardRows = normalizedTasks.map { task ->
                    val previous = existing.firstOrNull { it.id == task.id }
                    PlantEntity(
                        id = previous?.id ?: 0,
                        plantName = plantName,
                        taskName = task.name,
                        wateringInterval = wateringInterval,
                        creationDate = task.startDate.toString(),
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
                        programNote = drugNote ?: previous?.programNote.orEmpty(),
                        userLockedDate = previous?.programId != null || previous?.userLockedDate == true,
                        photoUri = photoUri ?: previous?.photoUri,
                        selectedVariantId = previous?.selectedVariantId,
                        variantChoices = previous?.variantChoices.orEmpty(),
                        userEdited = true,
                        programBaseline = previous?.programBaseline
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
                val rows = program.installedRows(gardenId, gardenName, reminderDaysBefore, photoUri)
                repository.importProgram(program.installation(), rows)
                onSaved(rows.map { it.taskName })
            } catch (e: Exception) {
                Log.d("importCareProgram", e.toString())
                onError(e.message ?: "Не удалось добавить программу")
            }
        }
    }

    fun updateProgramRepeat(
        plant: PlantEntity,
        intervalDays: Int?,
        onSaved: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateProgramRepeat(plant.id, intervalDays)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось изменить повтор. Повторите попытку.")
            }
        }
    }

    fun selectProgramVariant(
        plantId: Int,
        variantId: String,
        choices: String,
        onSaved: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.selectProgramVariant(plantId, variantId, choices)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError(error)
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
        reminderDaysBefore: Int? = null,
        photoUri: String? = null,
        originalRows: List<PlantEntity> = existingRows,
        onSaved: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (existingRows.isEmpty()) return@launch
                require(taskNames.size == existingRows.size && taskDates.size == existingRows.size) { "Список работ изменился. Откройте карточку заново." }
                val updated = existingRows.mapIndexed { index, row ->
                    row.copy(
                        plantName = plantName.trim(),
                        taskName = taskNames[index].trim(),
                        creationDate = taskDates[index].toString(),
                        gardenId = gardenId,
                        gardenName = gardenName,
                        reminderDaysBefore = reminderDaysBefore ?: row.reminderDaysBefore,
                        photoUri = photoUri ?: row.photoUri,
                        repeatDaysOfWeek = if (row.repeatType == RepeatType.WEEKLY.name) {
                            taskDates[index].dayOfWeek.value.toString()
                        } else {
                            row.repeatDaysOfWeek
                        },
                        userLockedDate = true,
                        userEdited = true
                    )
                }
                repository.replaceImportedProgramCard(originalRows, updated)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось сохранить программу")
            }
        }
    }

    fun saveProgramProduct(
        plant: PlantEntity, productId: String, date: LocalDate, reminder: Int,
        afterInspection: Boolean, problemId: String?, cultivation: CultivationType,
        onSaved: () -> Unit, onError: (String) -> Unit, personalProduct: DrugEntity? = null
    ) {
        viewModelScope.launch {
            try {
                require(!date.isBefore(LocalDate.now())) { "Выберите сегодняшнюю или будущую дату" }
                val replacement = personalProduct?.let { plant.withPersonalProduct(it, date, reminder, afterInspection, problemId) }
                    ?: if (afterInspection) plant.problemTreatment(problemId, productId, cultivation, date, reminder)
                    else plant.withProgramProduct(productId, date, reminder)
                if (afterInspection) repository.addProgramTreatment(plant, replacement)
                else repository.replaceUnusedProgramProduct(plant, replacement)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                onError(e.message ?: "Не удалось сохранить обработку. Повторите попытку.")
            }
        }
    }

    fun useTankMixture(plant: PlantEntity, recipe: FolkFertilizerRecipe, date: LocalDate,
        onSaved: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                require(recipe.isTankMix)
                require(!date.isBefore(LocalDate.now())) { "Выберите сегодняшнюю или будущую дату" }
                val savedDrug = repository.saveToMyProducts(DrugEntity(name = recipe.name,
                    purpose = recipe.purposeForDrug(), consumptionRate = recipe.consumptionRate, applicationMethod = "TREATMENT"))
                val crop = plant.programId ?: PlantCareCatalog.find(plant.plantName)?.id
                val autumnVariant = recipe.id == EradicationCare.AUTUMN_RECIPE_ID && EradicationCare.applies(crop.orEmpty())
                repository.addProgramTreatment(plant, plant.copy(
                    id = 0, plantCardId = plant.resolvedCardId,
                    taskName = "Использовать: ${recipe.name}",
                    creationDate = date.toString(), drugId = savedDrug.id, drugName = recipe.name,
                    programId = if (autumnVariant) crop else plant.programId,
                    programVersion = if (autumnVariant) EradicationCare.PROGRAM_VERSION else plant.programVersion,
                    programStepId = if (autumnVariant) EradicationCare.AUTUMN_MIX_STEP else "tank_mix:${recipe.id}", programImportKey = null,
                    programNote = recipe.purposeForDrug() + "\n" + recipe.consumptionRate,
                    repeatType = "NONE", repeatInterval = 1, wateringInterval = 1,
                    repeatDaysOfWeek = "", repeatEndType = "NEVER", repeatEndDate = null,
                    repeatCount = null, userLockedDate = true, userEdited = true, programBaseline = null,
                    selectedVariantId = null, variantChoices = ""
                ))
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError(error.message ?: "Не удалось добавить процедуру")
            }
        }
    }

}
