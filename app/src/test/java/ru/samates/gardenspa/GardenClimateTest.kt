package ru.samates.gardenspa

import java.time.MonthDay
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.domain.ClimateConfidence
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.GardenClimate
import ru.samates.gardenspa.domain.GardenLocation
import ru.samates.gardenspa.domain.LocationSource
import ru.samates.gardenspa.domain.decodeGardenClimate
import ru.samates.gardenspa.domain.encode

class GardenClimateTest {
    @Test
    fun climateRoundTripKeepsGardenSpecificLocation() {
        val climate = GardenClimate(
            GardenLocation(55.75, 37.62, 156, "Москва | Север", LocationSource.PLACE_SEARCH, 2.0),
            ClimateFingerprint(MonthDay.of(5, 10), MonthDay.of(9, 20), 133, 2_100.0, 1_300.0, 420.0, -24.0, ClimateConfidence.HIGH, 20)
        )

        assertEquals(climate, climate.encode().decodeGardenClimate())
    }
}
