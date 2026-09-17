package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class SeasonalProgramsTest {
    private val names = listOf("пион", "гортензия", "роза", "ежевика", "малина", "смородина",
        "земляника", "голубика", "яблоня", "груша", "картофель", "газон")
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun requestedCropsHaveDistinctProgramsAndUsableChoicesFromDifferentManufacturers() {
        val templates = names.map { requireNotNull(PlantCareCatalog.find(it)) }
        assertEquals(12, templates.map { it.id }.toSet().size)
        for (template in templates) {
            assertEquals(if (template.id in SpringCarePrograms.woodyCrops) 7 else 6, template.version)
            assertTrue(template.steps.size >= 4)
            assertEquals(template.steps.size, template.steps.map { it.id }.toSet().size)
            val feeding = template.steps.first { it.id.startsWith("nutrition~") }
            val alternatives = ProgramProductCatalog.alternatives(template.id, feeding.id)
                .filter { it.unavailableReason == null }
            assertTrue(template.id, alternatives.map { it.manufacturer }.toSet().size >= 2)
            assertTrue(alternatives.all { template.id in it.crops })
            assertTrue(ProgramProductCatalog.supports(template.id))
            assertTrue(ProgramProductCatalog.problemsFor(template.id).isNotEmpty())
            assertTrue(ProgramProductCatalog.treatments(template.id, null, CultivationType.OPEN_GROUND).isNotEmpty())
            assertTrue(ProgramProductCatalog.treatments(template.id, null, CultivationType.GREENHOUSE).isEmpty())
            assertTrue(template.steps.none { it.id == "pest_treatment_if_needed" })
            val generated = CareProgramGenerator().generate(template,
                CareProgramContext(LocalDate.of(2026, 3, 1), CultivationType.OPEN_GROUND, climate))
            assertEquals(template.steps.map { it.id }, generated.steps.map { it.templateStepId })
            assertTrue(generated.steps.all { it.note.isNotBlank() })
        }
        assertTrue(PlantNameCatalog.namesStartingWith("газон").contains("Газон"))
    }

    @Test fun newProgramReplacementKeepsOtherWorkAndDoesNotCarryTheDefaultFertilizerCourse() {
        val template = requireNotNull(PlantCareCatalog.find("пион"))
        val start = LocalDate.of(2026, 3, 1)
        val generated = CareProgramGenerator().generate(template, CareProgramContext(start, CultivationType.OPEN_GROUND, climate))
        val changed = generated.withProduct(0, "bona_flowers", start.plusDays(2))
        assertNotNull(generated.steps[0].recurrence)
        assertNull(changed.steps[0].recurrence)
        assertTrue(changed.steps[0].note.contains("15–25 г/м²"))
        assertFalse(changed.steps[0].note.contains("100 г/куст"))
        assertEquals(generated.steps.drop(1), changed.steps.drop(1))
        assertEquals("bona_flowers", ProgramProductCatalog.productForStep(changed.steps[0].templateStepId)?.id)
        assertThrows(NoSuchElementException::class.java) { generated.withProduct(0, "bona_blueberry", start) }
    }

    @Test fun exactCropProblemAndGroundAreRequiredEvenWhenDiagnosisIsOptional() {
        fun ids(crop: String, problem: String?, ground: CultivationType = CultivationType.OPEN_GROUND) =
            ProgramProductCatalog.treatments(crop, problem, ground).map { it.id }
        assertTrue("rayek_fruit" in ids("pear", "scab"))
        assertFalse("rayek_fruit" in ids("pear", "rust"))
        assertFalse("rayek_vegetables" in ids("potato", "late_blight"))
        assertTrue("ordan_potato" in ids("potato", "late_blight"))
        assertTrue("fitoverm_potato" in ids("potato", "colorado_beetle"))
        assertFalse("fitoverm_apple" in ids("pear", "codling_moth"))
        assertTrue(ids("blueberry", "scab").isEmpty())
        assertTrue(ids("lawn", "snow_mold").isEmpty())
        assertTrue("reanimator_lawn" in ids("lawn", "leaf_spot"))
        assertTrue("ordan_cucumber_greenhouse" in ids("cucumber", "downy_mildew", CultivationType.GREENHOUSE))
        assertFalse("ordan_cucumber_open_ground" in ids("cucumber", "downy_mildew", CultivationType.GREENHOUSE))
        assertTrue(ids("peony", null).containsAll(ids("peony", "powdery_mildew")))
    }

    @Test fun referenceIncludesEveryProgramProductAndBothPartsOfCucumberMixture() {
        val reference = ProgramReferenceCatalog.products
        assertEquals(reference.size, reference.map { it.id }.toSet().size)
        assertTrue(reference.all { it.sections.isNotEmpty() && it.sourceUrl.startsWith("https://") })
        assertTrue(reference.all { it.manufacturer in ProgramProductCatalog.allowedManufacturers })
        for (template in PlantCareCatalog.all().filter { ProgramProductCatalog.supports(it.id) }) {
            template.steps.mapNotNull { it.productDescription }.forEach { description ->
                assertTrue("Missing reference: $description", reference.any { it.displayName == description } ||
                    FolkFertilizers.tankMixes.any { it.name == description })
            }
        }
        assertTrue(ProgramReferenceCatalog.entries(ProductSection.FERTILIZER, "Гуми-Оми").isNotEmpty())
        assertTrue(ProgramReferenceCatalog.entries(ProductSection.TREATMENT, "Фитоспорин-АС").isNotEmpty())
        for (name in listOf("Борогум-М", "Богатый Овощи", "Агрикола Аква")) {
            assertTrue(ProgramReferenceCatalog.entries(ProductSection.FERTILIZER, name).isNotEmpty())
            assertTrue(ProgramReferenceCatalog.entries(ProductSection.TREATMENT, name).isNotEmpty())
        }
        assertTrue(ProgramReferenceCatalog.entries(ProductSection.FERTILIZER, "BonaForte").all { it.manufacturer == "BonaForte" })
        assertTrue(ProgramReferenceCatalog.entries(ProductSection.FERTILIZER, "абракадабра").isEmpty())
    }

    @Test fun tankMixtureIsMovedWithoutDuplicatingOrChangingItsRecipe() {
        assertEquals(6, FolkFertilizers.recipes.size)
        assertTrue(FolkFertilizers.recipes.none { it.isTankMix || it.id == "magic_plant_drink_tank_mix" })
        val mixture = FolkFertilizers.tankMixes.single { it.id == "magic_plant_drink_tank_mix" }
        assertEquals("magic_plant_drink_tank_mix", mixture.id)
        assertTrue(mixture.isTankMix)
        assertTrue(mixture.ingredients.contains("Алирин-Б — 4 таблетки"))
        assertTrue(mixture.ingredients.contains("Фитоверм — 20 мл"))
    }
}
