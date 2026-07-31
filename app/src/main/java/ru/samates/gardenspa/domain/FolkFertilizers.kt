package ru.samates.gardenspa.domain

data class FolkFertilizerRecipe(
    val id: String,
    val name: String,
    val purpose: String,
    val ingredients: String,
    val preparation: String,
    val consumptionRate: String,
    val warning: String,
    val sourceName: String,
    val sourceUrl: String
) {
    fun purposeForDrug(): String = buildString {
        append(purpose)
        append(" Приготовление: ")
        append(preparation)
        if (warning.isNotBlank()) {
            append(" Важно: ")
            append(warning)
        }
    }
}

object FolkFertilizers {
    private const val RHS_HOMEMADE = "https://www.rhs.org.uk/advice/profile?PID=1208"
    private const val OSU_COMPOST = "https://extension.oregonstate.edu/catalog/em-9308-how-use-compost-gardens-landscapes"
    private const val OSU_ASH = "https://extension.oregonstate.edu/catalog/ec-1503-fertilizing-your-garden-vegetables-fruits-ornamentals"
    private const val UMN_CLIPPINGS = "https://extension.umn.edu/lawns-and-landscapes-minnesota/what-do-lawn-clippings"

    val recipes = listOf(
        FolkFertilizerRecipe(
            id = "nettle_liquid",
            name = "Концентрат из крапивы",
            purpose = "Жидкая растительная подкормка для рассады, растений в контейнерах, овощных культур и декоративных однолетников.",
            ingredients = "Свежая крапива без семян, ёмкость с крышкой.",
            preparation = "Измельчить и плотно утрамбовать крапиву без добавления воды. Закрыть и проверять раз в неделю; слить тёмную жидкость после разложения массы.",
            consumptionRate = "1 часть концентрата на 20 частей воды; вносить под корень.",
            warning = "Собирать в перчатках. Не использовать как листовой спрей из-за риска вдыхания микроорганизмов.",
            sourceName = "Royal Horticultural Society",
            sourceUrl = RHS_HOMEMADE
        ),
        FolkFertilizerRecipe(
            id = "dried_green_feed",
            name = "Настой сухой зелёной массы",
            purpose = "Мягкая жидкая подкормка из высушенной травы или листьев.",
            ingredients = "Высушенная зелёная масса, вода, закрывающаяся ёмкость.",
            preparation = "Утрамбовать растительный материал и залить водой так, чтобы она полностью его покрыла. Держать под крышкой, пока жидкость не станет тёмной, затем процедить.",
            consumptionRate = "1 часть настоя на 10 частей воды; поливать под корень.",
            warning = "Не использовать растения с признаками болезни, семенами сорняков и одревесневшие побеги.",
            sourceName = "Royal Horticultural Society",
            sourceUrl = RHS_HOMEMADE
        ),
        FolkFertilizerRecipe(
            id = "finished_compost",
            name = "Внесение зрелого компоста",
            purpose = "Улучшение структуры почвы и постепенное пополнение органического вещества и питательных элементов.",
            ingredients = "Полностью созревший растительный компост.",
            preparation = "Разложить равномерным слоем по поверхности существующей грядки и аккуратно заделать в верхний слой почвы.",
            consumptionRate = "Слой 0,6–2,5 см один раз в год для существующих грядок.",
            warning = "Не использовать горячий или недозревший компост и не превышать норму: избыток тоже может повредить растениям.",
            sourceName = "Oregon State University Extension",
            sourceUrl = OSU_COMPOST
        ),
        FolkFertilizerRecipe(
            id = "wood_ash",
            name = "Древесная зола",
            purpose = "Источник калия и материал для снижения кислотности почвы.",
            ingredients = "Просеянная зола из чистой необработанной древесины.",
            preparation = "Равномерно рассыпать по влажной почве и слегка заделать, не оставляя куч.",
            consumptionRate = "Только по результатам анализа почвы: при дефиците калия — 50–75 г/м². Не применять при pH 7 и выше.",
            warning = "Не применять при pH 7 и выше, под голубику, рододендроны, азалии и картофель; не брать золу угля, мусора или окрашенной древесины.",
            sourceName = "Oregon State University Extension",
            sourceUrl = OSU_ASH
        ),
        FolkFertilizerRecipe(
            id = "grass_mulch",
            name = "Мульча из скошенной травы",
            purpose = "Постепенное питание почвы, удержание влаги и подавление сорняков.",
            ingredients = "Подсушенная трава с газона, который в последнее время не обрабатывали гербицидами.",
            preparation = "Подсушить траву и разложить тонким рыхлым слоем вокруг растений.",
            consumptionRate = "Слой 2,5–5 см за одно внесение.",
            warning = "Не использовать мокрую слежавшуюся траву или траву с газона, недавно обработанного гербицидами.",
            sourceName = "University of Minnesota Extension",
            sourceUrl = UMN_CLIPPINGS
        )
    )
}
