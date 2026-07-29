package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId

data class PlantCard(
    val cardId: String,
    val primary: PlantEntity,
    val procedures: List<PlantEntity>
)

fun Iterable<PlantEntity>.toPlantCards(): List<PlantCard> =
    groupBy(PlantEntity::resolvedCardId)
        .map { (cardId, procedures) ->
            val sortedProcedures = procedures.sortedBy(PlantEntity::id)
            PlantCard(
                cardId = cardId,
                primary = sortedProcedures.first(),
                procedures = sortedProcedures
            )
        }
        .sortedWith(compareBy({ it.primary.gardenName }, { it.primary.plantName }))
