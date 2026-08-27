package ru.samates.gardenspa.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.samates.gardenspa.data.database.entity.GardenWorkEntity
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.domain.GardenActivities
import ru.samates.gardenspa.domain.GardenWorkDraft
import ru.samates.gardenspa.domain.estimateGardenCalories

class GardenWorkViewModel(
    private val repository: BookeeperRepository
) : ViewModel() {
    val entries = repository.allGardenWorkEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun saveDay(
        date: LocalDate,
        weightKg: Double,
        work: List<GardenWorkDraft>,
        onSaved: () -> Unit = {}
    ) {
        val validWork = work.filter { it.minutes > 0 }
        viewModelScope.launch {
            try {
                val rows = validWork.map { item ->
                    val activity = GardenActivities.find(item.activityCode)
                    GardenWorkEntity(
                        workDate = date.toString(),
                        activityCode = activity.code,
                        activityName = activity.title,
                        minutes = item.minutes,
                        met = activity.met,
                        weightKg = weightKg,
                        calories = estimateGardenCalories(activity.met, weightKg, item.minutes)
                    )
                }
                repository.replaceGardenWorkForDate(date, rows)
                onSaved()
            } catch (error: Exception) {
                Log.d("saveGardenWork", error.toString())
            }
        }
    }
}

class GardenWorkViewModelFactory(
    private val repository: BookeeperRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GardenWorkViewModel::class.java)) {
            return GardenWorkViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
