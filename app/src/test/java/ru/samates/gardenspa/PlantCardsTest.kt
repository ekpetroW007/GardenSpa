package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.toPlantCards

class PlantCardsTest {
    @Test
    fun rowsWithTheSameCardIdAreShownAsOnePlantCard() {
        val plants = listOf(
            plant(id = 1, taskName = "Полив", cardId = "rose-card"),
            plant(id = 2, taskName = "Подкормка", cardId = "rose-card"),
            plant(id = 3, taskName = "Обрезка", cardId = "")
        )

        val cards = plants.toPlantCards()

        assertEquals(2, cards.size)
        assertEquals(listOf("Полив", "Подкормка"), cards.first { it.cardId == "rose-card" }.procedures.map { it.taskName })
        assertEquals(1, cards.first { it.cardId == "legacy-3" }.procedures.size)
    }

    private fun plant(id: Int, taskName: String, cardId: String) = PlantEntity(
        id = id,
        plantName = if (cardId.isBlank()) "Фикус" else "Роза",
        taskName = taskName,
        wateringInterval = 1,
        creationDate = "2026-07-30",
        drugId = null,
        gardenId = null,
        drugName = "Не выбран",
        gardenName = "Не выбран",
        plantCardId = cardId
    )
}
