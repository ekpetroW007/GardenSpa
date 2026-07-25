package com.example.myapplication.data.repository

import com.example.myapplication.data.database.dao.DrugDAO
import com.example.myapplication.data.database.dao.GardenDAO
import com.example.myapplication.data.database.dao.PlantDAO
import com.example.myapplication.data.database.dao.TaskDAO
import com.example.myapplication.data.database.entity.DrugEntity
import com.example.myapplication.data.database.entity.GardenEntity
import com.example.myapplication.data.database.entity.PlantEntity
import com.example.myapplication.data.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class BookeeperRepository(
    private val drugDao: DrugDAO,
    private val plantDAO: PlantDAO,
    private val taskDAO: TaskDAO,
    private val gardenDAO: GardenDAO
) {
    val allDrugs: Flow<List<DrugEntity>> = drugDao.getAllDrugs()
    val allGardens: Flow<List<GardenEntity>> = gardenDAO.getAllGardens()
    val allTasks: Flow<List<TaskEntity>> = taskDAO.getAllTasks()
    val allPlants: Flow<List<PlantEntity>> = plantDAO.getAllPlants()

    suspend fun insertDrug(drug: DrugEntity) {
        drugDao.insertDrug(drug)
    }

    suspend fun deleteDrug(id: Int) {
        drugDao.deleteDrug(id)
    }

    suspend fun deletePlant(id: Int) {
        plantDAO.deletePlant(id)
    }

    suspend fun insertTask(task: TaskEntity) {
        taskDAO.insertTask(task)
    }

    suspend fun deleteTask(id: Int) {
        taskDAO.deleteTask(id)
    }

    suspend fun insertGarden(garden: GardenEntity) {
        gardenDAO.insertGarden(garden)
    }

    suspend fun deleteGarden(id: Int) {
        gardenDAO.deleteGarden(id)
    }

    suspend fun insertPlant(plant: PlantEntity) {
        plantDAO.insertPlant(plant)
    }
}