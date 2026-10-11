package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class EradicationCareTest {
    private fun original(id: String) = PlantCareTemplate(id, "Проверка", emptySet(), 10,
        setOf(CultivationType.OPEN_GROUND), openGroundStartOffsetDays = -51, steps = listOf(
            CareStepTemplate(SpringCarePrograms.EARLY_STEP, "Старая обработка от болезней", offsetDays = 0, note = "Старая норма"),
            CareStepTemplate(SpringCarePrograms.PEST_STEP, "Старый Профилактин", offsetDays = 7, note = "Старая норма"),
            CareStepTemplate(SpringCarePrograms.GREEN_STEP, "Зелёный конус", offsetDays = 14, note = "Сохраняем состав"),
            CareStepTemplate("nutrition", "Подкормка", offsetDays = 35, note = "Сохраняем питание"),
            CareStepTemplate("${SpringCarePrograms.AUTUMN_STEP}~autumn_iron", "Старая осенняя обработка", offsetDays = 160, note = "Старая норма")
        ))
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun onlyEdibleWoodyProgramsReplaceTheOldStagesAndRetainOtherWork() {
        EradicationCare.cropIds.forEach { id ->
            val source = original(id)
            val changed = EradicationCare.apply(source)
            assertTrue(changed.version >= EradicationCare.PROGRAM_VERSION)
            assertEquals(1, changed.steps.count { it.id == EradicationCare.EARLY_STEP })
            assertEquals(1, changed.steps.count { it.id == EradicationCare.AUTUMN_MIX_STEP })
            assertEquals(1, changed.steps.count { it.id == EradicationCare.AUTUMN_IRON_STEP })
            assertEquals(1, changed.steps.count { ProgramProductCatalog.baseStepId(it.id) == EradicationCare.PEST_STEP })
            assertEquals(if (id in EradicationCare.fruitTreeIds) 1 else 0,
                changed.steps.count { ProgramProductCatalog.baseStepId(it.id) == EradicationCare.WHITEWASH_STEP })
            assertTrue(changed.steps.none { it.id.startsWith(SpringCarePrograms.PEST_STEP) || it.id.startsWith(SpringCarePrograms.AUTUMN_STEP) })
            assertEquals(source.steps.single { it.id == "nutrition" }, changed.steps.single { it.id == "nutrition" })
            assertEquals("Сохраняем состав", changed.steps.single { it.id == SpringCarePrograms.GREEN_STEP }.note)
        }
        listOf("rose", "hydrangea", "hydrangea-macrophylla", "thuja", "spruce", "pine", "juniper", "tomato", "garden-strawberry").forEach { id ->
            val source = original(id)
            assertSame(source, EradicationCare.apply(source))
        }
        assertFalse(EradicationCare.applies(null))
    }

    @Test fun springDatesShiftWithClimateAndAutumnUsesTheRegionalDay() {
        val template = EradicationCare.apply(original("apple"))
        fun dates(value: ClimateFingerprint) = CareProgramGenerator().generate(template,
            CareProgramContext(LocalDate.of(2026, 1, 1), CultivationType.OPEN_GROUND, value)).steps.associate { it.templateStepId to it.scheduledDate }
        val moderate = dates(climate)
        assertEquals(LocalDate.of(2026, 4, 4), moderate[EradicationCare.EARLY_STEP])
        assertEquals(LocalDate.of(2026, 4, 18), moderate[SpringCarePrograms.GREEN_STEP])
        assertEquals(LocalDate.of(2026, 9, 10), moderate[EradicationCare.AUTUMN_MIX_STEP])
        assertEquals(LocalDate.of(2026, 10, 25), moderate[EradicationCare.AUTUMN_IRON_STEP])
        val cold = dates(climate.copy(safeSpringDay = MonthDay.of(5, 20), safeAutumnDay = MonthDay.of(9, 15)))
        assertEquals(LocalDate.of(2026, 4, 29), cold[EradicationCare.EARLY_STEP])
        assertEquals(LocalDate.of(2026, 8, 16), cold[EradicationCare.AUTUMN_MIX_STEP])
        assertEquals(LocalDate.of(2026, 9, 25), cold[EradicationCare.AUTUMN_IRON_STEP])
    }

    @Test fun eachSpringChoiceKeepsTheSeparatePestStageAndCopperAmountsStayIndependent() {
        assertEquals(3, EradicationCare.springVariants.size)
        val young = EradicationCare.springVariants.single { it.id == "urea_young" }
        val mature = EradicationCare.springVariants.single { it.id == "urea_mature" }
        val copper = EradicationCare.springVariants.single { it.id == "copper" }
        assertEquals(7, young.followupDays)
        assertEquals(7, mature.followupDays)
        assertEquals(7, copper.followupDays)
        assertTrue(young.product.contains("300 г"))
        assertTrue(mature.product.contains("500–700 г"))
        assertTrue(EradicationCare.componentsFor(young.id).isEmpty())
        val options = EradicationCare.componentsFor(copper.id).single().options.associateBy { it.id }
        assertEquals(setOf("copper_mature", "copper_young", "bordeaux_liquid"), options.keys)
        assertTrue(options.getValue("copper_mature").product.contains("300 г"))
        assertFalse(options.getValue("copper_mature").note.contains("100 г"))
        assertTrue(options.getValue("copper_young").product.contains("100 г"))
        assertFalse(options.getValue("copper_young").note.contains("300 г"))
        assertTrue(options.getValue("bordeaux_liquid").product.contains("250 мл"))
        assertTrue(options.getValue("bordeaux_liquid").note.contains("зелёного конуса"))
        assertEquals(EradicationCare.springVariants, EradicationCare.variantsFor("before_bud_break~saved-choice"))
        (EradicationCare.springVariants + options.values).forEach { variant ->
            assertFalse(variant.note.contains("этикет", true))
            assertFalse(variant.note.contains("Профилактин", true))
            assertFalse(variant.note.contains("деревянных конструкций"))
            assertFalse(variant.note.contains("мочевин", true))
            assertTrue(variant.note.contains("\n\n"))
        }
        assertEquals("Карбамид — молодые растения, грибковые болезни", young.title)
        assertEquals("Карбамид — взрослые растения, грибковые болезни, вредители", mature.title)
    }

    @Test fun autumnComponentsKeepChoicesAndDoNotPresentTheOriginalCoragenGramsAsADose() {
        val variant = EradicationCare.variantsFor(EradicationCare.AUTUMN_MIX_STEP).single()
        assertEquals("autumn_main", variant.id)
        val groups = EradicationCare.componentsFor(variant.id)
        assertEquals(listOf("insecticide", "fungicide"), groups.map { it.id })
        assertEquals(listOf(2, 4), groups.map { it.options.size })
        val coragen = groups.flatMap { it.options }.single { it.id == "coragen" }
        assertTrue(coragen.product.contains("норма зависит от культуры"))
        assertFalse(coragen.product.contains("4 г"))
        assertTrue(coragen.note.contains("жидкий концентрат"))
        assertTrue(variant.note.contains("Эпин — 1 мл"))
        assertTrue(variant.note.contains("прилипатель — 10 мл"))
        assertTrue(variant.note.contains("+5…+10"))
        val fungicides = groups.last().options.associateBy { it.id }
        assertTrue(fungicides.getValue("horus").product.contains("2 г"))
        assertTrue(fungicides.getValue("horus").note.contains("+5…+10 °C"))
        assertTrue(fungicides.getValue("raek").note.contains("+14…+16 °C"))
        assertEquals(setOf("horus", "skor", "raek", "topaz"), fungicides.keys)
        assertTrue(EradicationCare.IRON_NOTE.contains("300 грамм"))
        assertTrue(EradicationCare.IRON_NOTE.contains("в 1 литре горячей воды"))
        assertTrue(EradicationCare.IRON_NOTE.contains("довести объем до 10 литров"))
        assertEquals(EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.autumnRecipe.id)
        assertEquals("Баковая смесь «Осенняя основная»", FolkFertilizers.tankMixes.single { it.id == EradicationCare.AUTUMN_RECIPE_ID }.name)
        val entries = ProgramReferenceCatalog.allEntries()
        EradicationCare.referenceProducts.forEach { product ->
            assertTrue("Missing source product: ${product.id}", ProgramReferenceCatalog.products.any { it.id == product.id })
            // Reference cards combine different program uses of the same named product.
            assertTrue("Missing reference content: ${product.displayName}", entries.any {
                it.displayName == product.displayName && it.instruction.contains(product.instruction) && it.crops.containsAll(product.crops)
            })
        }
        assertEquals(1, entries.count { it.displayName == "Профилактин БИО, ВЭ — Август" })
        assertTrue(SpringCarePrograms.mixtureProducts.none { it.id == "mix_${EradicationCare.AUTUMN_RECIPE_ID}" })
        assertTrue(SpringCarePrograms.mixtureProducts.none { it.id == "mix_${EradicationCare.IRON_RECIPE_ID}" })
    }

    @Test fun coordinatesSelectSeptemberOctoberOrNovemberEvenWithTheSameClimateFixture() {
        val template = EradicationCare.apply(original("apple"))
        listOf(
            Triple("Москва", 55.75 to 37.62, 10), Triple("Краснодар", 45.04 to 38.98, 11),
            Triple("Новосибирск", 55.03 to 82.92, 9), Triple("Омск", 54.99 to 73.37, 9),
            Triple("Владивосток", 43.12 to 131.89, 9), Triple("Мурманск", 68.97 to 33.07, 9)
        ).forEach { (name, coordinates, month) ->
            val context = CareProgramContext(LocalDate.of(2026, 1, 1), CultivationType.OPEN_GROUND, climate,
                location = GardenLocation(coordinates.first, coordinates.second, localityName = name,
                    source = LocationSource.PLACE_SEARCH, accuracyKm = 1.0))
            val steps = CareProgramGenerator().generate(template, context).steps.associateBy { ProgramProductCatalog.baseStepId(it.templateStepId) }
            val autumnDate = LocalDate.of(2026, month, 25)
            assertEquals(name, autumnDate, steps.getValue(EradicationCare.AUTUMN_IRON_STEP).scheduledDate)
            assertEquals(name, autumnDate.plusDays(14), steps.getValue(EradicationCare.WHITEWASH_STEP).scheduledDate)
        }
        assertEquals(LocalDate.of(2026, 11, 25), EradicationCare.autumnDate(
            CareProgramContext(LocalDate.of(2026, 1, 1), CultivationType.OPEN_GROUND, climate, isSouthernRegion = true)))
    }

    @Test fun beginningBetweenStagesMovesTheWholeDependentChainToNextYear() {
        val template = EradicationCare.apply(original("apple"))
        fun steps(start: LocalDate) = CareProgramGenerator().generate(template,
            CareProgramContext(start, CultivationType.OPEN_GROUND, climate)).steps.associateBy { ProgramProductCatalog.baseStepId(it.templateStepId) }
        val spring = steps(LocalDate.of(2026, 4, 5))
        val disease = spring.getValue(EradicationCare.EARLY_STEP).scheduledDate
        assertEquals(LocalDate.of(2027, 4, 4), disease)
        assertEquals(disease.plusDays(7), spring.getValue(EradicationCare.PEST_STEP).scheduledDate)
        assertEquals(disease.plusDays(14), spring.getValue(SpringCarePrograms.GREEN_STEP).scheduledDate)
        val autumn = steps(LocalDate.of(2026, 10, 26))
        val iron = autumn.getValue(EradicationCare.AUTUMN_IRON_STEP).scheduledDate
        assertEquals(LocalDate.of(2027, 10, 25), iron)
        assertEquals(iron.plusDays(14), autumn.getValue(EradicationCare.WHITEWASH_STEP).scheduledDate)
    }

    @Test fun pestAlternativesShowEveryRequestedProductWithoutTreatingWrongPhasesAsEquivalent() {
        val alternatives = ProgramProductCatalog.alternatives("apple", EradicationCare.PEST_STEP)
        assertEquals(7, alternatives.size)
        assertEquals(setOf("eradication_pest_profilaktin", "eradication_pest_30_plus", "eradication_pest_agrozelin"),
            alternatives.filter { it.unavailableReason == null }.map { it.id }.toSet())
        assertTrue(ProgramProductCatalog.alternatives("blueberry", EradicationCare.PEST_STEP).all { it.unavailableReason != null })
        val template = PlantCareCatalog.all().single { it.id == "apple" }
        val program = CareProgramGenerator().generate(template, CareProgramContext(LocalDate.of(2026, 1, 1), CultivationType.OPEN_GROUND, climate))
        val index = program.steps.indexOfFirst { ProgramProductCatalog.baseStepId(it.templateStepId) == EradicationCare.PEST_STEP }
        val selected = program.withProduct(index, "eradication_pest_agrozelin", program.steps[index].scheduledDate)
        assertEquals("Весенняя искореняющая обработка от зимующих вредителей", selected.steps[index].title)
        assertTrue(selected.steps[index].note.contains("500 мл на 10 л"))
        assertFalse(selected.steps[index].note.contains("этикет", true))
        assertThrows(NoSuchElementException::class.java) {
            program.withProduct(index, "eradication_pest_alatar", program.steps[index].scheduledDate)
        }
    }

    @Test fun whitewashHasTwoSeparateChoicesOnlyForFruitTrees() {
        EradicationCare.cropIds.forEach { crop ->
            val products = ProgramProductCatalog.alternatives(crop, EradicationCare.WHITEWASH_STEP)
            assertEquals(if (crop in EradicationCare.fruitTreeIds) 2 else 0, products.size)
        }
        assertTrue(EradicationCare.whitewashProducts.last().instruction.contains("2,5 кг"))
        assertTrue(EradicationCare.whitewashProducts.last().instruction.contains("300–500 г"))
    }
}
