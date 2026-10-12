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
import ru.samates.gardenspa.data.database.entity.ProgramInstallationEntity

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

    suspend fun saveToMyProducts(drug: DrugEntity): DrugEntity = drugDao.saveToMyProducts(drug)

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
        plantDAO.updatePlant(plant.copy(userEdited = true))
    }

    suspend fun importProgram(installation: ProgramInstallationEntity, plants: List<PlantEntity>) =
        plantDAO.importProgram(installation, plants)

    suspend fun refreshInstalledPrograms(today: LocalDate = LocalDate.now()): Int = plantDAO.refreshInstalledPrograms(today)

    suspend fun selectProgramVariant(plantId: Int, variantId: String, choices: String) =
        plantDAO.selectProgramVariant(plantId, variantId, choices)

    suspend fun updateProgramRepeat(plantId: Int, intervalDays: Int?) = plantDAO.updateProgramRepeat(plantId, intervalDays)

    suspend fun replaceImportedProgramCard(expected: List<PlantEntity>, replacement: List<PlantEntity>) =
        plantDAO.replaceImportedProgramCard(expected, replacement)

    suspend fun updateCompletionComment(procedureId: Int, comment: String) {
        require(comment.length <= 4000) { "Комментарий должен быть не длиннее 4000 символов" }
        require(procedureDAO.updateCompletionComment(procedureId, comment.trim()) == 1) { "Выполненная работа уже удалена" }
    }

    suspend fun replaceUnusedProgramProduct(expected: PlantEntity, replacement: PlantEntity) =
        plantDAO.replaceUnusedProgramProduct(expected, replacement)

    suspend fun addProgramTreatment(expected: PlantEntity, treatment: PlantEntity) =
        plantDAO.addProgramTreatment(expected, treatment)

    suspend fun replacePlantCard(cardId: String, plants: List<PlantEntity>) {
        plantDAO.replacePlantCard(cardId, plants)
    }

    suspend fun insertProcedure(procedure: ProcedureEntity): ProcedureEntity =
        procedureDAO.saveProcedureWithSnapshot(procedure)

    suspend fun scheduleNextSeason(procedureId: Int, today: LocalDate = LocalDate.now()): LocalDate =
        procedureDAO.scheduleNextSeason(procedureId, today)

    suspend fun getAllPlantsOnce(): List<PlantEntity> = plantDAO.getAllPlantsOnce()

    suspend fun getAllProceduresOnce(): List<ProcedureEntity> = procedureDAO.getAllProceduresOnce()

    suspend fun undoProcedureChange(plantId: Int, scheduledDate: LocalDate) =
        procedureDAO.undoChange(plantId, scheduledDate.toString())

    suspend fun undoCompletion(procedureId: Int): ProcedureEntity = procedureDAO.undoCompletion(procedureId)

    suspend fun getGardenWorkForDate(date: LocalDate): List<GardenWorkEntity> =
        gardenWorkDAO.getEntriesForDate(date.toString())

    suspend fun replaceGardenWorkForDate(date: LocalDate, entries: List<GardenWorkEntity>) {
        gardenWorkDAO.replaceEntriesForDate(date.toString(), entries)
    }
}
