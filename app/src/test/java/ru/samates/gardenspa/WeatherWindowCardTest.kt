package ru.samates.gardenspa

import java.net.UnknownHostException
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.nearestIncompleteTreatment
import ru.samates.gardenspa.presentation.selectWeatherGarden
import ru.samates.gardenspa.presentation.weatherFailureMessage

class WeatherWindowCardTest {
    @Test
    fun `weather follows the garden of the nearest treatment`() {
        val gardens = listOf(
            garden(id = 1, latitude = 55.75, longitude = 37.62),
            garden(id = 2, latitude = 59.94, longitude = 30.32),
            garden(id = 3)
        )

        val plant = work(3, LocalDate.of(2026, 9, 30))
        assertEquals(3, selectWeatherGarden(gardens,
            ScheduledTreatment(plant, LocalDate.parse(plant.creationDate), LocalDate.parse(plant.creationDate), false, false))?.id)
    }

    @Test
    fun `weather falls back to the oldest located garden when there is no treatment`() {
        val gardens = listOf(
            garden(id = 7, latitude = 56.84, longitude = 60.61),
            garden(id = 4, latitude = 55.03, longitude = 82.92)
        )

        assertEquals(4, selectWeatherGarden(gardens, null)?.id)
    }

    @Test
    fun `an unassigned or deleted garden never uses another gardens weather for its treatment`() {
        val date = LocalDate.of(2026, 9, 30)
        val located = listOf(garden(1, 55.75, 37.62))
        val unassigned = work(3, date).copy(gardenId = null)
        assertNull(selectWeatherGarden(located, ScheduledTreatment(unassigned, date, date, false, false)))
        assertNull(selectWeatherGarden(located, ScheduledTreatment(unassigned.copy(gardenId = 99), date, date, false, false)))
    }

    @Test
    fun `garden follows effective dates and switches after completion postponement and undo`() {
        val today = LocalDate.of(2026, 9, 30)
        val gardens = listOf(garden(1, 55.75, 37.62), garden(2, 59.94, 30.32), garden(3, 56.84, 60.61))
        val works = listOf(work(1, today), work(2, today.plusDays(1)), work(3, today.plusDays(2)))
        val firstDone = ProcedureEntity(plantId = 1, procedureName = "Полив", scheduledDate = today.toString(),
            completedDate = today.toString(), status = "COMPLETED")
        val secondMoved = ProcedureEntity(plantId = 2, procedureName = "Полив", scheduledDate = today.plusDays(1).toString(),
            rescheduledDate = today.plusDays(5).toString())
        val nearest = nearestIncompleteTreatment(works, listOf(firstDone, secondMoved), today)
        assertEquals(today.plusDays(2), nearest?.scheduledDate)
        assertEquals(3, selectWeatherGarden(gardens, nearest)?.id)

        val thirdDone = firstDone.copy(plantId = 3, scheduledDate = today.plusDays(2).toString())
        val afterCompletion = nearestIncompleteTreatment(works, listOf(firstDone, secondMoved, thirdDone), today)
        assertEquals(today.plusDays(5), afterCompletion?.scheduledDate)
        assertEquals(2, selectWeatherGarden(gardens, afterCompletion)?.id)

        val afterUndo = nearestIncompleteTreatment(works, listOf(secondMoved, thirdDone), today)
        assertEquals(today, afterUndo?.scheduledDate)
        assertEquals(1, selectWeatherGarden(gardens, afterUndo)?.id)
    }

    @Test
    fun `weather error explains a dns failure`() {
        val error = IllegalStateException("request failed", UnknownHostException("api.open-meteo.com"))

        assertEquals(
            "Телефон не может подключиться к погодному сервису. Проверьте Private DNS, VPN или другую сеть.",
            weatherFailureMessage(error)
        )
    }

    private fun garden(
        id: Int,
        latitude: Double? = null,
        longitude: Double? = null
    ) = GardenEntity(
        id = id,
        name = "Сад $id",
        latitude = latitude,
        longitude = longitude
    )

    private fun work(gardenId: Int, date: LocalDate) = PlantEntity(
        id = gardenId, plantName = "Яблоня", taskName = "Полив", wateringInterval = 1,
        creationDate = date.toString(), drugId = null, gardenId = gardenId,
        drugName = "Вода", gardenName = "Сад $gardenId", repeatType = "NONE"
    )
}
