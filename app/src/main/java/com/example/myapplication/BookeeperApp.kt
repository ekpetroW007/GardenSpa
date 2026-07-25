package com.example.myapplication

import android.app.Application
import kotlin.getValue
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.BookeeperRepository

class BookeeperApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        BookeeperRepository(
            drugDao = database.drugDao(),
            plantDAO = database.plantDao(),
            gardenDAO = database.gardenDao(),
            taskDAO = database.taskDao()
        )
    }
}