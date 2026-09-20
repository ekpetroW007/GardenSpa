package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.domain.*

class ExpandedCareProgramsTest {
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)
    private fun program(name: String, date: LocalDate = LocalDate.of(2026, 3, 1), south: Boolean = false) =
        CareProgramGenerator().generate(PlantCareCatalog.find(name)!!, CareProgramContext(date, CultivationType.OPEN_GROUND, climate, isSouthernRegion = south))

    @Test fun hydrangeaSpeciesShareCareButPruningAndShelterDependOnSpeciesAndRegion() {
        val pan = PlantCareCatalog.find("Гортензия метельчатая")!!
        val large = PlantCareCatalog.find("Гортензия крупнолистная")!!
        val tree = PlantCareCatalog.find("Гортензия древовидная")!!
        assertEquals(3, setOf(pan.id, large.id, tree.id).size)
        fun shared(template: PlantCareTemplate) = template.steps.filterNot { it.id in setOf("hydrangea_pruning", ExpandedCarePrograms.SHELTER_STEP) }
        assertEquals(shared(pan), shared(large))
        assertEquals(shared(pan), shared(tree))
        assertTrue(pan.steps.any { it.id == "hydrangea_pruning" })
        assertTrue(tree.steps.any { it.id == "hydrangea_pruning" })
        assertFalse(large.steps.any { it.id == "hydrangea_pruning" })
        assertTrue(program(large.canonicalName).steps.any { it.templateStepId == ExpandedCarePrograms.SHELTER_STEP })
        assertFalse(program(large.canonicalName, south = true).steps.any { it.templateStepId == ExpandedCarePrograms.SHELTER_STEP })
        assertTrue(ProgramProductCatalog.alternatives(pan.id, "nutrition").none { it.id == "bona_hydrangea" })
        assertTrue(ProgramProductCatalog.alternatives(large.id, "nutrition").any { it.id == "bona_hydrangea" })
    }

    @Test fun dependentLeafChecksKeepFiveDayGapsEvenWhenStartingBetweenThem() {
        val name = "Гортензия метельчатая"
        val template = PlantCareCatalog.find(name)!!
        val start = recommendedStartDate(template, CultivationType.OPEN_GROUND, climate, 2026)
        val original = program(name, start)
        val first = original.steps.single { it.templateStepId.startsWith("hydrangea_pale_acid") }
        val late = program(name, first.scheduledDate.plusDays(2))
        val checks = late.steps.filter { it.templateStepId.startsWith("hydrangea_pale") || it.templateStepId.startsWith("hydrangea_yellow") }
        assertEquals(3, checks.size)
        assertEquals(2027, checks.first().scheduledDate.year)
        assertEquals(5L, ChronoUnit.DAYS.between(checks[0].scheduledDate, checks[1].scheduledDate))
        assertEquals(5L, ChronoUnit.DAYS.between(checks[1].scheduledDate, checks[2].scheduledDate))
        assertTrue(checks[0].note.contains("Полив под корень"))
        val ironIndex = late.steps.indexOfFirst { it.templateStepId == "hydrangea_pale_iron" }
        val changed = late.withProduct(ironIndex, "hydrangea_antichlorozin", checks[1].scheduledDate)
        assertTrue(changed.steps[ironIndex].title.startsWith("Осмотр"))
        assertTrue(changed.steps[ironIndex].note.contains("бледные листья"))
        assertFalse(changed.steps[ironIndex].note.contains("2 столовые ложки"))
        assertEquals(setOf(ProductSection.FERTILIZER), ProgramProductCatalog.products.single { it.id == "hydrangea_citric_acid" }.sections)
    }

    @Test fun conifersHaveCareAndAtLeastFiveFeedingChoicesWithoutLeaflessIronTreatment() {
        ExpandedCarePrograms.conifers.forEach { (id, name) ->
            val generated = program(name)
            assertEquals(id, generated.templateId)
            assertTrue(generated.steps.any { it.templateStepId == "conifer_water" })
            assertFalse(generated.steps.any { it.templateStepId.startsWith(SpringCarePrograms.AUTUMN_STEP) })
            assertTrue(ProgramProductCatalog.alternatives(id, "nutrition").filter { it.unavailableReason == null }.size >= 5)
            assertTrue(ProgramProductCatalog.alternatives(id, "conifer_sanitation").isEmpty())
        }
        assertTrue(ProgramProductCatalog.alternatives("lawn", "nutrition").any { it.manufacturer == "Fertika" })
    }

    @Test fun everyProgramHasFiveSelectableProductsAndTwoRealManufacturers() {
        PlantCareCatalog.all().forEach { template ->
            val choices = template.steps.flatMap { ProgramProductCatalog.alternatives(template.id, it.id) }
                .filter { it.unavailableReason == null && it.manufacturer != "Рецепт пользователя" }.distinctBy { it.id }
            assertTrue("${template.id}: ${choices.size}", choices.size >= 5)
            assertTrue(template.id, choices.map { it.manufacturer }.distinct().size >= 2)
        }
    }

    @Test fun newRecipesKeepAmountsChoiceAndPollinationNotice() {
        assertEquals(7, FolkFertilizers.tankMixes.size)
        FolkFertilizers.tankMixes.forEach { assertTrue(it.warning.contains(FolkFertilizers.POLLINATION_NOTICE)) }
        val mixes = FolkFertilizers.tankMixes.filter { it.id !in setOf(FolkFertilizers.GREEN_CONE_ID, "magic_plant_drink_tank_mix") }
        assertEquals(5, mixes.size)
        assertTrue(mixes.all { it.ingredients.startsWith("На 10 л воды") })
        assertTrue(mixes.single { it.id == "biological_pests_tank_mix" }.ingredients.contains("Лепидоцид, жидкий — 20 мл"))
        assertTrue(mixes.single { it.id == "fitosporin_stock_tank_mix" }.preparation.contains("200 г развести в 400 мл"))
        assertTrue(FolkFertilizers.recipes.single { it.id == "pharmacy_cocktail" }.ingredients.contains("по 1 ампуле"))
        assertTrue(FolkFertilizers.recipes.single { it.id == "yeast_tincture" }.preparation.contains("до 10 л"))
    }

    @Test fun greenConeReplacementAndPostFloweringMixturesKeepPhaseAndSelectedRecipeOnly() {
        val original = program("Яблоня")
        val index = original.steps.indexOfFirst { it.templateStepId == SpringCarePrograms.GREEN_STEP }
        assertEquals(7, ProgramProductCatalog.alternatives("apple", SpringCarePrograms.GREEN_STEP).size)
        val changed = original.withProduct(index, "mix_fitolavin_tank_mix", original.steps[index].scheduledDate)
        assertEquals(original.steps[index].title, changed.steps[index].title)
        assertTrue(changed.steps[index].note.contains("Фитолавин — 20 мл"))
        assertFalse(changed.steps[index].note.contains("Микохелп"))
        assertTrue(changed.steps[index].note.contains("до или после периода цветения"))
        for (name in listOf("Яблоня", "Груша")) assertTrue(program(name).steps.any { it.templateStepId == SpringCarePrograms.AFTER_FLOWERING_STEP })
    }

    @Test fun requestedDiseasesAreExplicitAndTreatmentsNeverLeakAcrossTargets() {
        val expected = mapOf("rose" to setOf("black_spot"), "peony" to setOf("gray_mold"), "lily" to setOf("gray_mold"), "tulip" to setOf("gray_mold"),
            "tomato" to setOf("late_blight", "fusarium", "cladosporium"), "sweet-pepper" to setOf("late_blight", "fusarium", "cladosporium"),
            "eggplant" to setOf("late_blight", "fusarium", "cladosporium"), "apple" to setOf("moniliosis", "scab"), "pear" to setOf("moniliosis", "scab"),
            "cherry" to setOf("moniliosis", "scab"), "plum" to setOf("moniliosis", "scab"), "gladiolus" to setOf("fusarium"), "iris" to setOf("fusarium"), "cucumber" to setOf("alternaria"))
        expected.forEach { (crop, ids) -> assertTrue(crop, ProgramProductCatalog.problemsFor(crop).map { it.id }.containsAll(ids)) }
        assertTrue(ProgramProductCatalog.treatments("rose", "black_spot", CultivationType.OPEN_GROUND).any { it.id == "skor_rose" })
        assertTrue(ProgramProductCatalog.treatments("lily", "gray_mold", CultivationType.OPEN_GROUND).any { it.id == "chistotsvet_flowers" })
        assertTrue(ProgramProductCatalog.treatments("cherry", "moniliosis", CultivationType.OPEN_GROUND).any { it.id == "horus_stone" })
        assertFalse(ProgramProductCatalog.treatments("cherry", "scab", CultivationType.OPEN_GROUND).any { it.id == "horus_stone" })
        assertTrue(ProgramProductCatalog.treatments("cucumber", "alternaria", CultivationType.OPEN_GROUND).any { it.unavailableReason == null })
        assertTrue(ProgramProductCatalog.treatments("tomato", "cladosporium", CultivationType.GREENHOUSE).any { it.id == "reanimator_tomato_cladosporium" })
        assertTrue(ProgramProductCatalog.treatments("tomato", "cladosporium", CultivationType.OPEN_GROUND).isEmpty())
    }

    @Test fun referenceSearchFindsAllFiveCategoriesAndNormalizesYo() {
        val personal = listOf(DrugEntity(id = 42, name = "Мой настой", purpose = "Своя запись", consumptionRate = "По этикетке"))
        assertTrue(searchReference("настой", personal).any { it.category == "Мои средства" })
        assertTrue(searchReference("коктейль аптеки", personal).any { it.category == "Народные рецепты" })
        assertEquals("Баковые смеси", searchReference("зеленый конус", personal).first().category)
        assertTrue(searchReference("газонное Fertika", personal).any { it.category == ProductSection.FERTILIZER.title })
        assertTrue(searchReference("Феровит", personal).any { it.category == ProductSection.TREATMENT.title })
        assertTrue(searchReference("абракадабра", personal).isEmpty())
    }
}
