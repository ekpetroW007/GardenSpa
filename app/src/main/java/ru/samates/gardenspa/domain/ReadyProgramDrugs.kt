package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.DrugEntity

private enum class ProgramDrugCategory {
    DISEASE,
    PEST,
    ROOT_FEEDING,
    FOLIAR_FEEDING,
    AUTUMN_FEEDING
}

private data class ProgramDrugOffer(
    val drug: DrugEntity,
    val categories: Set<ProgramDrugCategory>,
    val plantPrefixes: Set<String> = emptySet()
)

object ReadyProgramDrugCatalog {
    private val offers = listOf(
        ProgramDrugOffer(
            DrugEntity(
                name = "Фитоспорин-М Универсальный — БашИнком",
                purpose = "Биофунгицид для профилактики и защиты овощных, ягодных, плодовых и декоративных культур от грибных и бактериальных болезней.",
                consumptionRate = "По инструкции конкретной формы выпуска и выбранной культуры."
            ),
            setOf(ProgramDrugCategory.DISEASE)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Алирин-Б — АгроБиоТехнология",
                purpose = "Биологический фунгицид против корневых гнилей и листовых инфекций овощных, плодовых и цветочных культур.",
                consumptionRate = "Обычно 1–2 таблетки на 10 л воды; точную норму и способ применения сверить с инструкцией для культуры."
            ),
            setOf(ProgramDrugCategory.DISEASE)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Ордан — Август",
                purpose = "Фунгицид от фитофтороза, альтернариоза и пероноспороза картофеля, томатов, огурцов и лука.",
                consumptionRate = "По регламенту на упаковке для выбранной культуры; нормы и сроки ожидания различаются."
            ),
            setOf(ProgramDrugCategory.DISEASE),
            setOf("tomato", "cucumber", "potato", "onion")
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "ХОМ — Техноэкспорт",
                purpose = "Контактный фунгицид для защиты картофеля и томатов от фитофтороза и альтернариоза, огурцов от пероноспороза.",
                consumptionRate = "По регламенту на упаковке для выбранной культуры; рабочий раствор использовать в день приготовления."
            ),
            setOf(ProgramDrugCategory.DISEASE),
            setOf("tomato", "cucumber", "potato")
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Искра BIO — Техноэкспорт",
                purpose = "Биологический инсектоакарицид против тли, клещей и других вредителей овощных, плодовых, ягодных и цветочных культур.",
                consumptionRate = "По инструкции для выявленного вредителя и выбранной культуры."
            ),
            setOf(ProgramDrugCategory.PEST)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Батрайдер — Август",
                purpose = "Инсектицид от комплекса вредителей плодовых, овощных и цветочно-декоративных культур.",
                consumptionRate = "По инструкции для выявленного вредителя и выбранной культуры; соблюдать срок ожидания."
            ),
            setOf(ProgramDrugCategory.PEST)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "LEAF POWER Универсальное — Fertika",
                purpose = "Водорастворимое комплексное удобрение с микроэлементами для корневых и внекорневых подкормок овощных, цветочных и плодово-ягодных культур.",
                consumptionRate = "Корневая подкормка: 10–15 г на 10 л воды; внекорневая: 15 г на 10 л воды."
            ),
            setOf(ProgramDrugCategory.ROOT_FEEDING, ProgramDrugCategory.FOLIAR_FEEDING)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Аминозол — Август",
                purpose = "Жидкое органическое удобрение с аминокислотами для корневых и внекорневых подкормок овощных, плодовых, ягодных и декоративных культур.",
                consumptionRate = "По инструкции для выбранной культуры и способа подкормки."
            ),
            setOf(ProgramDrugCategory.ROOT_FEEDING, ProgramDrugCategory.FOLIAR_FEEDING)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "FERTIKA Осеннее — Fertika",
                purpose = "Комплексное осеннее удобрение с повышенным содержанием фосфора и калия для подготовки многолетних растений к зиме.",
                consumptionRate = "30–40 г на 1 м² приствольной зоны; точную норму сверить с инструкцией и культурой."
            ),
            setOf(ProgramDrugCategory.AUTUMN_FEEDING)
        ),
        ProgramDrugOffer(
            DrugEntity(
                name = "Агрикола Осеннее — Техноэкспорт",
                purpose = "Комплексное удобрение для осеннего внесения в почву под садовые и огородные культуры.",
                consumptionRate = "По инструкции на упаковке с учётом культуры, возраста растения и площади питания."
            ),
            setOf(ProgramDrugCategory.AUTUMN_FEEDING)
        )
    )

    val defaultDrugs: List<DrugEntity> = offers.map(ProgramDrugOffer::drug)

    fun recommendedFor(templateId: String, stepId: String, availableDrugs: List<DrugEntity>): List<DrugEntity> {
        val category = categoryFor(stepId) ?: return emptyList()
        return offers.asSequence()
            .filter { category in it.categories && (it.plantPrefixes.isEmpty() || it.plantPrefixes.any(templateId::startsWith)) }
            .mapNotNull { offer -> availableDrugs.firstOrNull { it.name.equals(offer.drug.name, ignoreCase = true) } }
            .toList()
    }

    private fun categoryFor(stepId: String): ProgramDrugCategory? = when {
        "disease" in stepId -> ProgramDrugCategory.DISEASE
        "pest" in stepId -> ProgramDrugCategory.PEST
        "autumn_root_feeding" in stepId -> ProgramDrugCategory.AUTUMN_FEEDING
        "foliar_feeding" in stepId -> ProgramDrugCategory.FOLIAR_FEEDING
        "root_feeding" in stepId -> ProgramDrugCategory.ROOT_FEEDING
        else -> null
    }
}
