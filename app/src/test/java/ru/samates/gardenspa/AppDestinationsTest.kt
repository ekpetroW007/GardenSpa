package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.presentation.navigation.AppDestinations

class AppDestinationsTest {
    @Test
    fun plantRouteKeepsGardenWherePlantWasCreated() {
        assertEquals(
            "plantAddScreen/2026-08-15?gardenId=42",
            AppDestinations.plantAdd("2026-08-15", gardenId = 42)
        )
        assertEquals(
            "plantAddScreen/2026-08-15",
            AppDestinations.plantAdd("2026-08-15")
        )
    }
}
