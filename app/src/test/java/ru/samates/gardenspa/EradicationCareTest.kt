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

    @Test fun allThreeDatesShiftWithClimateAndGreenConeStaysAfterTheSpringStage() {
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
        assertEquals(LocalDate.of(2026, 9, 30), cold[EradicationCare.AUTUMN_IRON_STEP])
    }

    @Test fun springChoiceDeterminesFollowupAndKeepsCopperAmountsInSeparateOptions() {
        assertEquals(3, EradicationCare.springVariants.size)
        val young = EradicationCare.springVariants.single { it.id == "urea_young" }
        val mature = EradicationCare.springVariants.single { it.id == "urea_mature" }
        val copper = EradicationCare.springVariants.single { it.id == "copper" }
        assertEquals(7, young.followupDays)
        assertNull(mature.followupDays)
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
    }

    @Test fun autumnComponentsKeepChoicesAndDoNotPresentTheOriginalCoragenGramsAsADose() {
        val variant = EradicationCare.variantsFor(EradicationCare.AUTUMN_MIX_STEP).single()
        assertEquals("autumn_main", variant.id)
        val groups = EradicationCare.componentsFor(variant.id)
        assertEquals(listOf("insecticide", "fungicide"), groups.map { it.id })
        assertEquals(listOf(2, 3), groups.map { it.options.size })
        val coragen = groups.flatMap { it.options }.single { it.id == "coragen" }
        assertTrue(coragen.product.contains("по этикетке выбранной культуры"))
        assertFalse(coragen.product.contains("4 г"))
        assertTrue(coragen.note.contains("жидкий концентрат"))
        assertTrue(variant.note.contains("Эпин-Экстра — 1 мл"))
        assertTrue(variant.note.contains("прилипатель — 10 мл"))
        assertTrue(variant.note.contains("не ниже +15 °C"))
        assertTrue(variant.note.contains("после периода цветения"))
        assertTrue(EradicationCare.IRON_NOTE.contains("300 г"))
        assertTrue(EradicationCare.IRON_NOTE.contains("в 1 л горячей воды"))
        assertTrue(EradicationCare.IRON_NOTE.contains("общего объёма 10 л"))
        assertTrue(EradicationCare.IRON_NOTE.contains("не подтверждённая схема лечения хлороза"))
        assertEquals(EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.autumnRecipe.id)
        assertEquals("Баковая смесь «Осенняя основная»", FolkFertilizers.tankMixes.single { it.id == EradicationCare.AUTUMN_RECIPE_ID }.name)
        assertTrue(EradicationCare.referenceProducts.all { product -> ProgramReferenceCatalog.allEntries().any { it.id == product.id } })
        assertTrue(SpringCarePrograms.mixtureProducts.none { it.id == "mix_${EradicationCare.AUTUMN_RECIPE_ID}" })
    }
}
