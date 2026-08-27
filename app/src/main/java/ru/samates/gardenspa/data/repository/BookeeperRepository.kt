package ru.samates.gardenspa.data.repository

import ru.samates.gardenspa.data.database.dao.DrugDAO
import ru.samates.gardenspa.data.database.dao.GardenDAO
import ru.samates.gardenspa.data.database.dao.GardenWorkDAO
import ru.samates.gardenspa.data.database.dao.PlantDAO
import ru.samates.gardenspa.data.database.dao.TaskDAO
import ru.samates.gardenspa.data.database.dao.ProcedureDAO
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.GardenWorkEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.TaskEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class BookeeperRepository(
    private val drugDao: DrugDAO,
    private val plantDAO: PlantDAO,
    private val taskDAO: TaskDAO,
    private val gardenDAO: GardenDAO,
    private val procedureDAO: ProcedureDAO,
    private val gardenWorkDAO: GardenWorkDAO
) {
    val allDrugs: Flow<List<DrugEntity>> = drugDao.getAllDrugs()
    val allGardens: Flow<List<GardenEntity>> = gardenDAO.getAllGardens()
    val allTasks: Flow<List<TaskEntity>> = taskDAO.getAllTasks()
    val allPlants: Flow<List<PlantEntity>> = plantDAO.getAllPlants()
    val allProcedures: Flow<List<ProcedureEntity>> = procedureDAO.getAllProcedures()
    val allGardenWorkEntries: Flow<List<GardenWorkEntity>> = gardenWorkDAO.getAllEntries()

    suspend fun insertDrug(drug: DrugEntity) {
        drugDao.insertDrug(drug)
    }

    suspend fun updateDrug(drug: DrugEntity) {
        drugDao.updateDrug(drug)
        plantDAO.updateDrugName(drug.id, drug.name)
    }

    suspend fun deleteDrug(id: Int) {
        drugDao.deleteDrug(id)
    }

    suspend fun deletePlant(id: Int) {
        plantDAO.deletePlant(id)
    }

    suspend fun deletePlantCard(cardId: String) {
        plantDAO.deletePlantCard(cardId)
    }

    suspend fun insertTask(task: TaskEntity) {
        taskDAO.insertTask(task)
    }

    suspend fun deleteTask(id: Int) {
        taskDAO.deleteTask(id)
    }

    suspend fun insertGarden(garden: GardenEntity): Long = gardenDAO.insertGarden(garden)

    suspend fun updateGarden(garden: GardenEntity) = gardenDAO.updateGarden(garden)

    suspend fun deleteGarden(id: Int) {
        gardenDAO.deleteGarden(id)
    }

    suspend fun insertPlant(plant: PlantEntity) {
        plantDAO.insertPlant(plant)
    }

    suspend fun updatePlant(plant: PlantEntity) {
        plantDAO.updatePlant(plant)
    }

    suspend fun replacePlantCard(cardId: String, plants: List<PlantEntity>) {
        plantDAO.replacePlantCard(cardId, plants)
    }

    suspend fun insertProcedure(procedure: ProcedureEntity) {
        procedureDAO.insertProcedure(procedure)
    }

    suspend fun getAllPlantsOnce(): List<PlantEntity> = plantDAO.getAllPlantsOnce()

    suspend fun getAllProceduresOnce(): List<ProcedureEntity> = procedureDAO.getAllProceduresOnce()

    suspend fun undoProcedureChange(plantId: Int, scheduledDate: LocalDate) =
        procedureDAO.deleteForSchedule(plantId, scheduledDate.toString())

    suspend fun getGardenWorkForDate(date: LocalDate): List<GardenWorkEntity> =
        gardenWorkDAO.getEntriesForDate(date.toString())

    suspend fun replaceGardenWorkForDate(date: LocalDate, entries: List<GardenWorkEntity>) {
        gardenWorkDAO.replaceEntriesForDate(date.toString(), entries)
    }
}
