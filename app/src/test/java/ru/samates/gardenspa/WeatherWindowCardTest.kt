package ru.samates.gardenspa

import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.GardenEntity
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

        assertEquals(3, selectWeatherGarden(gardens, listOf(3, 2, 1))?.id)
    }

    @Test
    fun `weather falls back to the oldest located garden when there is no treatment`() {
        val gardens = listOf(
            garden(id = 7, latitude = 56.84, longitude = 60.61),
            garden(id = 4, latitude = 55.03, longitude = 82.92)
        )

        assertEquals(4, selectWeatherGarden(gardens, emptyList())?.id)
    }

    @Test
    fun `weather error explains a dns failure`() {
        val error = IllegalStateException("request failed", UnknownHostException("api.weatherapi.com"))

        assertEquals(
            "Телефон не может найти api.weatherapi.com. Проверьте Private DNS, VPN или другую сеть.",
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
}
