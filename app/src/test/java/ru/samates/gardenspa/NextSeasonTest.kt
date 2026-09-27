package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.nextSeasonDate
import java.time.LocalDate

class NextSeasonTest {
    @Test fun nextSeasonUsesPostponedDateAndClampsLeapDay() {
        val archive = ProcedureEntity(plantId = 1, procedureName = "Полив", scheduledDate = "2024-02-20",
            rescheduledDate = "2024-02-29", completedDate = "2024-03-02", status = "COMPLETED")
        assertEquals(LocalDate.of(2025, 2, 28), archive.nextSeasonDate(LocalDate.of(2024, 3, 2)))
        // Calculate from the original leap day each time, so a later leap season keeps 29 February.
        assertEquals(LocalDate.of(2028, 2, 29), archive.nextSeasonDate(LocalDate.of(2027, 3, 1)))
    }

    @Test fun olderArchiveUsesFirstFutureAnniversaryRatherThanPastSeason() {
        val archive = ProcedureEntity(plantId = 1, procedureName = "Полив", scheduledDate = "2024-05-10")
        assertEquals(LocalDate.of(2027, 5, 10), archive.nextSeasonDate(LocalDate.of(2026, 9, 27)))
        assertEquals(LocalDate.of(2026, 5, 10), archive.nextSeasonDate(LocalDate.of(2026, 3, 1)))
        assertEquals(LocalDate.of(2027, 5, 10), archive.nextSeasonDate(LocalDate.of(2026, 5, 10)))
    }
}
