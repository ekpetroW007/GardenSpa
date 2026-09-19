package ru.samates.gardenspa

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.*

class PersonalProductsTest {
    @Test fun ownProductRetainsCardAndNextYearSuffixButClearsOldCourseAndInstructions() {
        val row = PlantEntity(id = 3, plantName = "Томат", taskName = "Подкормить: Старое средство", wateringInterval = 14,
            creationDate = "2027-05-01", drugId = null, gardenId = 1, drugName = "Старое средство", gardenName = "Сад",
            plantCardId = "card", programId = "tomato", programStepId = "gumi_omi_feeding~old:remaining:2",
            programNote = "Старая норма", repeatType = "CUSTOM", repeatCount = 5)
        val own = DrugEntity(id = 9, name = "Моя подкормка", purpose = "Питание", consumptionRate = "По этикетке")
        val changed = row.withPersonalProduct(own, LocalDate.of(2027, 5, 3), 5)
        assertEquals("card", changed.plantCardId)
        assertEquals("gumi_omi_feeding~personal:9:remaining:2", changed.programStepId)
        assertEquals("NONE", changed.repeatType)
        assertNull(changed.repeatCount)
        assertFalse(changed.programNote.contains("Старая"))
        assertEquals(own.careNote, changed.programNote)
        assertFalse(changed.taskName.contains("Старое средство"))
        assertTrue(changed.taskName.contains(own.name))
        assertEquals(9, changed.drugId)
        val extra = row.withPersonalProduct(own, LocalDate.now(), 0, afterInspection = true)
        assertEquals(0, extra.id)
        assertEquals("card", extra.plantCardId)
        assertTrue(extra.programNote.contains("не указаны"))
        assertNotEquals(extra.programStepId, row.withPersonalProduct(own, LocalDate.now(), 0, true).programStepId)
    }

    @Test fun manualCatalogIncludesEveryRootAndFoliarEntryAndPepperHasAlternatives() {
        val all = ProgramReferenceCatalog.allEntries()
        ProductSection.entries.forEach { section ->
            ProgramReferenceCatalog.entries(section).forEach { assertTrue(it.displayName, all.any { entry -> entry.id == it.id }) }
        }
        val pepper = ProgramProductCatalog.alternatives("sweet-pepper", "preventive_disease_treatment")
        assertTrue(pepper.map { it.id }.containsAll(listOf("silver_prevention", "sporobacterin_vegetables", "biozashchitin_prevention")))
        assertTrue(pepper.filter { it.unavailableReason == null }.map { it.manufacturer }.distinct().size >= 3)
        PlantCareCatalog.all().forEach { template -> assertTrue(template.id, template.steps.none { it.title.contains("осмотреть", true) }) }
    }

    @Test fun retailTreatmentsUseCropAndGroundAndManufacturerCountsDoNotCountVariantsTwice() {
        fun options(crop: String, problem: String, ground: CultivationType = CultivationType.OPEN_GROUND) =
            ProgramProductCatalog.treatments(crop, problem, ground).filter { it.unavailableReason == null && it.retailUrl != null }
        for (flower in listOf("rose", "peony")) for (problem in listOf("aphids", "thrips")) {
            assertTrue(options(flower, problem).map { it.manufacturer }.distinct().size >= 5)
        }
        assertTrue(options("potato", "colorado_beetle").map { it.manufacturer }.distinct().size >= 5)
        assertFalse(options("tomato", "whitefly").any { it.id == "biotlin_greenhouse" })
        assertTrue(options("tomato", "whitefly", CultivationType.GREENHOUSE).any { it.id == "biotlin_greenhouse" })
        assertTrue(options("lawn", "snow_mold").isEmpty())
        assertFalse(options("pear", "rust").any { it.id.startsWith("skor_") })
        // Machine-readable evidence for the accompanying coverage report, generated from the shipped filters.
        println("COVERAGE_BEGIN")
        println("Растение;Условия;Проблема;Вариантов всего;Производителей всего;Вариантов Лемана;Производителей Лемана;Производители Лемана")
        PlantCareCatalog.all().forEach { template ->
            template.supportedCultivationTypes.forEach { ground ->
                ProgramProductCatalog.problemsFor(template.id).forEach { problem ->
                    val all = ProgramProductCatalog.treatments(template.id, problem.id, ground).filter { it.unavailableReason == null }
                    val retail = all.filter { it.retailUrl != null }
                    println(listOf(template.canonicalName, ground.displayName, problem.label, all.size,
                        all.map { it.manufacturer }.distinct().size, retail.size, retail.map { it.manufacturer }.distinct().size,
                        retail.map { it.manufacturer }.distinct().sorted().joinToString(", ")).joinToString(";"))
                }
            }
        }
        println("COVERAGE_END")
    }
}
