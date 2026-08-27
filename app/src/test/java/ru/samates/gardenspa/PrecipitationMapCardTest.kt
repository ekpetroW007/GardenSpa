package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.presentation.openWeatherPrecipitationTileUrl
import ru.samates.gardenspa.presentation.selectPrecipitationGarden

class PrecipitationMapCardTest {
    @Test
    fun `map follows the garden of the nearest treatment even before coordinates are added`() {
        val gardens = listOf(
            garden(id = 1, latitude = 55.75, longitude = 37.62),
            garden(id = 2, latitude = 59.94, longitude = 30.32),
            garden(id = 3)
        )

        assertEquals(3, selectPrecipitationGarden(gardens, listOf(3, 2, 1))?.id)
    }

    @Test
    fun `map never silently switches from the nearest treatment to another garden`() {
        val gardens = listOf(
            garden(id = 1),
            garden(id = 2),
            garden(id = 3),
            garden(id = 4, latitude = 54.71, longitude = 20.51),
            garden(id = 5, latitude = 55.75, longitude = 37.62)
        )

        assertEquals(1, selectPrecipitationGarden(gardens, listOf(1, 2, 3, 4, 5))?.id)
    }

    @Test
    fun `map falls back to the oldest located garden`() {
        val gardens = listOf(
            garden(id = 7, latitude = 56.84, longitude = 60.61),
            garden(id = 4, latitude = 55.03, longitude = 82.92)
        )

        assertEquals(4, selectPrecipitationGarden(gardens, emptyList())?.id)
    }

    @Test
    fun `blank weather key disables only the precipitation layer`() {
        assertNull(openWeatherPrecipitationTileUrl("   "))
        assertEquals(
            "https://tile.openweathermap.org/map/precipitation_new/{z}/{x}/{y}.png?appid=test-key",
            openWeatherPrecipitationTileUrl(" test-key ")
        )
    }

    @Test
    fun `dry pixels stay invisible and precipitation becomes translucent dark blue`() {
        assertEquals(0, stylePrecipitationPixel(0x006D6DCD))
        assertEquals(0xC0082B4C.toInt(), stylePrecipitationPixel(0x206D6DCD))
        assertEquals(0xE0082B4C.toInt(), stylePrecipitationPixel(0xFF6D6DCD.toInt()))
    }

    @Test
    fun `cloud pattern is drawn only inside precipitation`() {
        val width = 64
        val height = 64
        val sourceAlpha = IntArray(width * height) { 32 }
        val pixels = IntArray(width * height) { stylePrecipitationPixel(0x206D6DCD) }

        addPrecipitationCloudPattern(pixels, sourceAlpha, width, height)

        assertEquals(0xF8FFFFFF.toInt(), pixels[32 * width + 32])
        assertEquals(0xC0082B4C.toInt(), pixels[0])

        val dryPixels = IntArray(width * height)
        addPrecipitationCloudPattern(dryPixels, IntArray(width * height), width, height)
        assertEquals(true, dryPixels.all { it == 0 })
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
