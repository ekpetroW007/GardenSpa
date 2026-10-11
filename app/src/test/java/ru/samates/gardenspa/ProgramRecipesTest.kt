package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class ProgramRecipesTest {
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun recipeIdsWinOverOldNamesAndReplacementsWinOverTheOriginalPhase() {
        val iron = EradicationCare.ironRecipe
        assertEquals(iron, ProgramRecipes.recipeFor("tank_mix:${iron.id}", "Прежнее название"))
        assertEquals(iron, ProgramRecipes.recipeFor(EradicationCare.AUTUMN_IRON_STEP, "Железный купорос — 300 г на 10 л воды"))
        assertEquals(iron, ProgramRecipes.recipeFor(null, "Баковая смесь «Искореняющая»"))
        val replacement = FolkFertilizers.tankMixes.single { it.id == "fitolavin_tank_mix" }
        assertEquals(replacement, ProgramRecipes.recipeFor("${SpringCarePrograms.GREEN_STEP}~mix_${replacement.id}:remaining:2"))
        assertEquals(replacement, ProgramRecipes.recipeFor(null, "${replacement.name} — Рецепт пользователя"))
        assertNull(ProgramRecipes.recipeFor("${SpringCarePrograms.GREEN_STEP}~personal_drug_1", "Мой состав"))
        assertNull(ProgramRecipes.recipeFor("tank_mix:unknown", "Неизвестный рецепт"))
        FolkFertilizers.recipes.forEach { recipe ->
            assertEquals(recipe, ProgramRecipes.recipeFor("folk_recipe:${recipe.id}"))
        }
    }

    @Test fun compositionKeepsReferenceAmountsAndParagraphsWithoutRepeatedHeading() {
        val autumn = EradicationCare.autumnRecipe
        assertEquals("На 10 литров воды:\nАктара 4 г\nХорус 2 г\nЭпин 1 мл\nПрилипатель 10 мл", ProgramRecipes.composition(autumn))
        assertEquals(EradicationCare.IRON_NOTE.substringAfter("\n\n"), ProgramRecipes.composition(EradicationCare.ironRecipe))
        (FolkFertilizers.tankMixes + FolkFertilizers.recipes).filter {
            it.id !in setOf(EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.IRON_RECIPE_ID)
        }.forEach { recipe -> assertEquals(recipe.ingredients, ProgramRecipes.composition(recipe)) }
    }

    @Test fun everySelectableMixtureUsesTheReferenceNameExactly() {
        SpringCarePrograms.mixtureProducts.forEach { product ->
            val recipe = requireNotNull(ProgramRecipes.recipeFor("any~${product.id}"))
            assertEquals(recipe.name, product.displayName)
        }
        val program = CareProgramGenerator().generate(PlantCareCatalog.find("Яблоня")!!,
            CareProgramContext(LocalDate.of(2026, 1, 1), CultivationType.OPEN_GROUND, climate))
        val index = program.steps.indexOfFirst { it.templateStepId == SpringCarePrograms.GREEN_STEP }
        val updated = program.withProduct(index, "mix_fitolavin_tank_mix", program.steps[index].scheduledDate)
        val recipe = FolkFertilizers.tankMixes.single { it.id == "fitolavin_tank_mix" }
        assertEquals(recipe.name, updated.steps[index].productDescription)
        assertEquals(recipe.name, updated.installedRows(null, "Сад")[index].drugName)
    }

    @Test fun fruitProgramsGenerateAndInstallOnlyOneMainAutumnProcedureEvenAfterSelection() {
        EradicationCare.fruitTreeIds.forEach { crop ->
            val template = PlantCareCatalog.all().single { it.id == crop }
            listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 11)).forEach { start ->
                val program = CareProgramGenerator().generate(template,
                    CareProgramContext(start, CultivationType.OPEN_GROUND, climate))
                val autumnSteps = program.steps.filter {
                    ProgramRecipes.recipeFor(it.templateStepId, it.productDescription)?.id == EradicationCare.AUTUMN_RECIPE_ID
                }
                assertEquals(crop, 1, autumnSteps.size)
                assertNull(autumnSteps.single().recurrence)
                val rows = program.installedRows(null, "Сад")
                val autumn = rows.filter {
                    ProgramRecipes.recipeFor(it.programStepId, it.drugName)?.id == EradicationCare.AUTUMN_RECIPE_ID
                }.single()
                assertEquals(EradicationCare.autumnRecipe.name, autumn.taskName)
                assertEquals(EradicationCare.autumnRecipe.name, autumn.drugName)
                val selected = autumn.withProgramVariant("autumn_main", "insecticide=coragen|fungicide=raek")
                assertEquals(EradicationCare.autumnRecipe.name, selected.taskName)
                assertTrue(selected.drugName.contains("Кораген"))
                assertFalse(selected.drugName.contains("Актара"))
                assertEquals("NONE", selected.repeatType)
            }
        }
    }

    @Test fun whitewashFollowsTheNamedIronMixtureByFourteenDaysInEveryRegionAndStartSeason() {
        EradicationCare.fruitTreeIds.forEach { crop ->
            for (autumnMonth in listOf(9, 10, 11)) {
                for (start in listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, autumnMonth, 26))) {
                    val program = CareProgramGenerator().generate(PlantCareCatalog.all().single { it.id == crop },
                        CareProgramContext(start, CultivationType.OPEN_GROUND, climate.copy(safeAutumnDay = MonthDay.of(autumnMonth, 10))))
                    val ironIndex = program.steps.indexOfFirst { it.templateStepId == EradicationCare.AUTUMN_IRON_STEP }
                    val whitewashIndex = program.steps.indexOfFirst { ProgramProductCatalog.baseStepId(it.templateStepId) == EradicationCare.WHITEWASH_STEP }
                    val iron = program.steps[ironIndex]
                    assertEquals(EradicationCare.ironRecipe.name, iron.title)
                    assertEquals(EradicationCare.ironRecipe.name, iron.productDescription)
                    assertTrue("$crop $start: chronological order", whitewashIndex > ironIndex)
                    assertEquals(iron.scheduledDate.plusDays(14), program.steps[whitewashIndex].scheduledDate)
                }
            }
        }
    }

    @Test fun removedProductIsAbsentFromAllActiveCatalogSurfaces() {
        val labels = EradicationCare.autumnComponents.flatMap { group -> group.options.flatMap { listOf(it.id, it.title, it.product, it.note) } } +
            ProgramProductCatalog.products.flatMap { listOf(it.id, it.name, it.instruction) } +
            ProgramReferenceCatalog.allEntries().flatMap { listOf(it.id, it.name, it.instruction) } +
            (FolkFertilizers.recipes + FolkFertilizers.tankMixes).flatMap { listOf(it.name, it.ingredients, it.preparation) } +
            PlantCareCatalog.all().flatMap { template -> template.steps.flatMap { listOf(it.title, it.productDescription.orEmpty(), it.note) } }
        assertTrue(labels.none { it.contains("тилт", true) || it.contains("tilt", true) })
    }
}
