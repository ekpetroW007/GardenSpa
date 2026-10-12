package ru.samates.gardenspa

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.*

class VariantFollowupScheduleTest {
    private val today = LocalDate.of(2027, 3, 1)
    private fun row(id: Int, step: String, date: String, crop: String = "apple") = PlantEntity(
        id = id, plantName = "Растение", taskName = step, wateringInterval = 1, creationDate = date,
        drugId = null, gardenId = null, drugName = "Средство", gardenName = "Сад", plantCardId = "card",
        programId = crop, programVersion = RootCareSchedule.VERSION, programStepId = step,
        programImportKey = "card:$crop:v23:$step", programNote = "Исходная инструкция"
    ).let { it.copy(programBaseline = it.programFingerprint()) }

    @Test fun postponedSpringMovesOnlyForwardFromTheExistingClimateFloorAndRetainsRepeatSettings() {
        // Selected v22 parents can remain unchanged while their standard children have already migrated to v23.
        val parent = row(1, "before_bud_break", "2027-04-04").copy(programVersion = 22)
        val feeding = row(2, "nutrition", "2027-04-25").copy(repeatType = "CUSTOM", repeatInterval = 30,
            repeatEndType = "COUNT", repeatCount = 3).let { it.copy(programBaseline = it.programFingerprint()) }
        val moved = variantFollowupDateUpdates(parent, listOf(parent, feeding), emptyList(), LocalDate.of(2027, 5, 10), today).single()
        assertEquals("2027-05-11", moved.creationDate)
        assertEquals(feeding.copy(creationDate = moved.creationDate, programBaseline = moved.programBaseline), moved)
        assertEquals(moved.programFingerprint(), moved.programBaseline)
        assertTrue(variantFollowupDateUpdates(parent, listOf(parent, moved), emptyList(), LocalDate.of(2027, 4, 1), today).isEmpty())
        assertTrue(variantFollowupDateUpdates(parent, listOf(parent, moved), emptyList(), LocalDate.of(2027, 5, 10), today).isEmpty())
        val laterFloor = feeding.copy(creationDate = "2027-06-01").let { it.copy(programBaseline = it.programFingerprint()) }
        assertTrue(variantFollowupDateUpdates(parent, listOf(parent, laterFloor), emptyList(), LocalDate.of(2027, 5, 10), today).isEmpty())
    }

    @Test fun liveRootUpdatesPreserveManualPersonalHistoricalPastAndUnrelatedWorks() {
        val parent = row(1, "before_bud_break", "2027-04-04")
        val feeding = row(2, "nutrition", "2027-04-25")
        val protected = listOf(feeding.copy(userEdited = true), feeding.copy(userLockedDate = true),
            feeding.copy(drugId = 3), feeding.copy(programNote = "Мой состав"), feeding.copy(programBaseline = null),
            feeding.copy(programVersion = 22), feeding.copy(plantCardId = "other"), feeding.copy(programId = "pear"),
            feeding.copy(programImportKey = "archive-next-season:100"), feeding.copy(creationDate = "2027-02-01")
                .let { it.copy(programBaseline = it.programFingerprint()) },
            row(2, "nutrition~personal:3", "2027-04-25"))
        protected.forEach { child ->
            assertTrue(child.toString(), variantFollowupDateUpdates(parent, listOf(parent, child), emptyList(),
                LocalDate.of(2027, 5, 10), today).isEmpty())
        }
        for (status in listOf("PLANNED", "COMPLETED")) {
            val record = ProcedureEntity(plantId = feeding.id, procedureName = feeding.taskName,
                scheduledDate = feeding.creationDate, status = status)
            assertTrue(variantFollowupDateUpdates(parent, listOf(parent, feeding), listOf(record), LocalDate.of(2027, 5, 10), today).isEmpty())
        }
        assertTrue(variantFollowupDateUpdates(parent.copy(programImportKey = null), listOf(parent, feeding), emptyList(),
            LocalDate.of(2027, 5, 10), today).isEmpty())
        val planting = row(3, "fitosporin_roots", "2027-04-25", "tomato")
        val spray = row(4, "fitosporin_spraying", "2027-05-03", "tomato")
        assertTrue(variantFollowupDateUpdates(spray, listOf(spray, planting), emptyList(), LocalDate.of(2027, 5, 10), today).isEmpty())
    }

    @Test fun movingHydrangeaRootCheckKeepsBothFiveDayIntervals() {
        val crop = "hydrangea-macrophylla"
        val parent = row(1, "before_bud_break", "2027-04-04", crop)
        val acid = row(2, "hydrangea_pale_acid", "2027-04-25", crop)
        val iron = row(3, "hydrangea_pale_iron", "2027-04-30", crop)
        val magnesium = row(4, "hydrangea_yellow_magnesium", "2027-05-05", crop)
        val updates = variantFollowupDateUpdates(parent, listOf(parent, acid, iron, magnesium), emptyList(),
            LocalDate.of(2027, 5, 10), today).associateBy { it.id }
        assertEquals("2027-05-10", updates[acid.id]?.creationDate)
        assertEquals("2027-05-15", updates[iron.id]?.creationDate)
        assertEquals("2027-05-20", updates[magnesium.id]?.creationDate)
        assertTrue(updates.values.all { it.programBaseline == it.programFingerprint() })
    }

    @Test fun separateSeasonAndMaterializedRepeatsKeepTheirDatesAndIntervals() {
        val parent = row(1, "before_bud_break", "2028-04-04")
        val previousSeason = row(2, "nutrition~standard:remaining:1", "2027-04-25")
        val series = listOf("2028-04-25", "2028-05-09", "2028-05-23").mapIndexed { index, date ->
            row(3 + index, "nutrition~standard:remaining:${index + 2}", date)
        }
        val updates = variantFollowupDateUpdates(parent, listOf(parent, previousSeason) + series, emptyList(),
            LocalDate.of(2028, 5, 10), today)
        assertEquals(listOf("2028-05-11", "2028-05-25", "2028-06-08"), updates.map { it.creationDate })
        assertFalse(updates.any { it.id == previousSeason.id })
        assertTrue(updates.all { it.repeatType == "NONE" && it.programBaseline == it.programFingerprint() })
        assertTrue(variantFollowupDateUpdates(parent, listOf(parent, previousSeason) + series, emptyList(),
            LocalDate.of(2029, 5, 10), today).isEmpty())
    }
}
