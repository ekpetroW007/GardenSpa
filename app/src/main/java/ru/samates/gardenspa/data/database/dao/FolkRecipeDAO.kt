package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.samates.gardenspa.domain.FolkFertilizerRecipe

@Dao
interface FolkRecipeDAO {
    @Query("SELECT * FROM folk_recipe ORDER BY name COLLATE NOCASE")
    fun getAllRecipes(): Flow<List<FolkFertilizerRecipe>>

    @Update
    suspend fun updateRecipe(recipe: FolkFertilizerRecipe)

    @Delete
    suspend fun deleteRecipe(recipe: FolkFertilizerRecipe)
}
