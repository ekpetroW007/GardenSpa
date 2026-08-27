package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.domain.FolkFertilizers

class FolkFertilizersTest {
    @Test
    fun magicPlantDrinkContainsTheRequestedIngredients() {
        val recipe = FolkFertilizers.recipes.single { it.id == "magic_plant_drink_tank_mix" }

        assertEquals("Баковая смесь «Волшебный напиток для растений»", recipe.name)
        listOf(
            "Алирин-Б — 4 таблетки",
            "Гамаир — 4 таблетки",
            "Циркон — 2 мл",
            "Силиплант — 20 мл",
            "Фитоверм — 20 мл"
        ).forEach { ingredient ->
            assertTrue(recipe.ingredients.contains(ingredient))
        }
    }
}
