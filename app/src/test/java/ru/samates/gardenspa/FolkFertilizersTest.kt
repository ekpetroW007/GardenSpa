package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.domain.FolkFertilizers

class FolkFertilizersTest {
    @Test
    fun catalogRetainsRussianSourcesAndTheUserRecipe() {
        assertEquals(
            setOf("milk_iodine_late_blight", "ash_spray_rust", "ash_soap_aphids",
                "mustard_caterpillars", "mustard_apple_scab", "ash_feed_fruit_set",
                "magic_plant_drink_tank_mix", FolkFertilizers.GREEN_CONE_ID),
            (FolkFertilizers.recipes + FolkFertilizers.tankMixes).map { it.id }.toSet()
        )
        val allowedHosts = setOf("old.rosselhoscenter.ru", "rosselhoscenter.ru", "lenta.ru")
        FolkFertilizers.recipes.filter { it.sourceUrl.isNotBlank() }.forEach {
            assertTrue("Unexpected source for ${it.id}", java.net.URI(it.sourceUrl).host in allowedHosts)
        }
    }

    @Test
    fun magicPlantDrinkContainsTheRequestedIngredients() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == "magic_plant_drink_tank_mix" }

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

    @Test fun greenConeContainsLiquidLepidocideAndKeepsIngredientsWhenAddedToMyProducts() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        assertEquals("Зелёный конус. Биологические препараты", recipe.name)
        listOf("На 10 л воды", "Микохелп — 20 мл", "Фитохелп — 20 мл",
            "Лепидоцид, жидкий препарат — 20 мл", "Битоксибациллин — 40 г",
            "Фитоверм — 20 мл", "Липосам — 8 мл (1 пакет)", "Циркон — 1 мл").forEach {
            assertTrue(recipe.ingredients.contains(it))
            assertTrue(recipe.purposeForDrug().contains(it))
        }
        assertTrue(recipe.sourceUrl.isEmpty())
        assertTrue(recipe.warning.contains("совместимость полного состава отдельно не подтверждена"))
        assertTrue(FolkFertilizers.recipes.none { it.id == recipe.id })
    }
}
