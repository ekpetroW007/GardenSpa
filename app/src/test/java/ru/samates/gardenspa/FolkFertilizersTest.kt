package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.domain.FolkFertilizers
import ru.samates.gardenspa.domain.EradicationCare

class FolkFertilizersTest {
    @Test
    fun catalogRetainsRussianSourcesAndTheUserRecipe() {
        assertEquals(
            setOf("milk_iodine_late_blight", "ash_spray_rust", "ash_soap_aphids",
                "mustard_caterpillars", "mustard_apple_scab", "ash_feed_fruit_set",
                "magic_plant_drink_tank_mix", FolkFertilizers.GREEN_CONE_ID, "pharmacy_cocktail", "yeast_tincture",
                "alirin_gamair_tank_mix", "fitolavin_tank_mix", "biological_pests_tank_mix", "btu_protection_tank_mix", "fitosporin_stock_tank_mix",
                EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.IRON_RECIPE_ID),
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
        assertEquals("Баковая смесь «Зелёный конус. Биологические препараты»", recipe.name)
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

    @Test fun eradicationMixtureReusesTheAgreedIronRecipeAndPreservesItInMyProducts() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == EradicationCare.IRON_RECIPE_ID }
        assertEquals("Баковая смесь «Осенняя искореняющая»", recipe.name)
        assertEquals("", recipe.ingredients)
        assertEquals(EradicationCare.IRON_NOTE, recipe.preparation)
        assertTrue(recipe.purposeForDrug().contains(EradicationCare.IRON_NOTE))
        assertTrue(recipe.warning.isEmpty())
        assertTrue(recipe.consumptionRate.isEmpty())
        assertEquals(EradicationCare.IRON_PURPOSE + "\n\n" + EradicationCare.IRON_NOTE, recipe.purposeForDrug())
        assertTrue(FolkFertilizers.recipes.none { it.id == recipe.id })
    }

    @Test fun autumnRecipeKeepsOnlyTheAgreedTextAndCorrectHorusUnit() {
        val recipe = FolkFertilizers.tankMixes.single { it.id == EradicationCare.AUTUMN_RECIPE_ID }
        assertEquals("Проводится после сбора плодов, до листопада\n\nТемпература воздуха — +5…+10", recipe.purpose)
        assertEquals("Баковая смесь «Осенняя основная»\nНа 10 литров воды:\nАктара 4 г\nХорус 2 г\nЭпин 1 мл\nПрилипатель 10 мл", recipe.ingredients)
        assertTrue(listOf(recipe.preparation, recipe.warning, recipe.consumptionRate).all(String::isEmpty))
        assertEquals(recipe.purpose + "\n\n" + recipe.ingredients, recipe.purposeForDrug())
        FolkFertilizers.tankMixes.filter { it.id !in setOf(EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.IRON_RECIPE_ID) }.forEach {
            assertTrue(it.warning.contains(FolkFertilizers.POLLINATION_NOTICE))
        }
        val template = ru.samates.gardenspa.domain.PlantCareCatalog.all().single { it.id == "apple" }
        assertEquals(EradicationCare.IRON_NOTE, template.steps.single { it.id == EradicationCare.AUTUMN_IRON_STEP }.note)
    }
}
