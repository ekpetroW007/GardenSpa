package ru.samates.gardenspa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProceduresViewmodel(private val repository: BookeeperRepository) : ViewModel() {
    val procedures = repository.allProcedures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    fun markCompleted(plantId: Int, procedureName: String, scheduledDate: LocalDate) {
        markCompleted(plantId, procedureName, scheduledDate, scheduledDate)
    }

    fun markCompleted(
        plantId: Int,
        procedureName: String,
        originalDate: LocalDate,
        scheduledDate: LocalDate,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.insertProcedure(
                ProcedureEntity(
                    plantId = plantId,
                    procedureName = procedureName,
                    scheduledDate = originalDate.toString(),
                    rescheduledDate = scheduledDate.toString().takeIf { scheduledDate != originalDate },
                    completedDate = LocalDate.now().toString(),
                    status = "COMPLETED"
                )
            )
            onSaved()
        }
    }

    fun reschedule(
        plantId: Int,
        procedureName: String,
        originalDate: LocalDate,
        newDate: LocalDate,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.insertProcedure(
                ProcedureEntity(
                    plantId = plantId,
                    procedureName = procedureName,
                    scheduledDate = originalDate.toString(),
                    rescheduledDate = newDate.toString().takeIf { newDate != originalDate },
                    status = "PLANNED"
                )
            )
            onSaved()
        }
    }
}

class ProceduresViewmodelFactory(
    private val repository: BookeeperRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProceduresViewmodel::class.java)) {
            return ProceduresViewmodel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
