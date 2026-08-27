package ru.samates.gardenspa.domain

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue

enum class CultivationType(val displayName: String) {
    OPEN_GROUND("Открытый грунт"),
    GREENHOUSE("Теплица")
}

enum class CareAnchor {
    START_DATE,
    SAFE_SPRING_DATE
}

data class WeatherLimits(
    val minimumNightTemperatureC: Double? = null,
    val maximumPrecipitationMm: Double? = null,
    val maximumWindMetersPerSecond: Double? = null
)

data class CareRecurrence(
    val type: RepeatType,
    val interval: Int,
    val count: Int
)

data class CareStepTemplate(
    val id: String,
    val title: String,
    val anchor: CareAnchor = CareAnchor.START_DATE,
    val offsetDays: Int,
    val windowBeforeDays: Int = 0,
    val windowAfterDays: Int = 0,
    val weatherLimits: WeatherLimits = WeatherLimits(),
    val recurrence: CareRecurrence? = null,
    val productDescription: String? = null,
    val note: String
)

data class PlantCareTemplate(
    val id: String,
    val canonicalName: String,
    val aliases: Set<String>,
    val version: Int,
    val supportedCultivationTypes: Set<CultivationType>,
    val openGroundStartOffsetDays: Int = 3,
    val greenhouseStartOffsetDays: Int = -14,
    val steps: List<CareStepTemplate>
)

data class CareProgramContext(
    val startDate: LocalDate,
    val cultivationType: CultivationType,
    val climate: ClimateFingerprint,
    val forecast: List<ForecastWeatherDay> = emptyList()
)

data class GeneratedCareStep(
    val templateStepId: String,
    val title: String,
    val scheduledDate: LocalDate,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val recurrence: CareRecurrence?,
    val weatherAdjusted: Boolean,
    val needsWeatherConfirmation: Boolean,
    val explanation: String,
    val productDescription: String?,
    val note: String
)

data class GeneratedCareProgram(
    val instanceId: String,
    val templateId: String,
    val templateVersion: Int,
    val plantName: String,
    val cultivationType: CultivationType,
    val recommendedStartDate: LocalDate,
    val chosenStartDate: LocalDate,
    val climateSummary: String,
    val warning: String?,
    val steps: List<GeneratedCareStep>
)

enum class ProgramStartChoice {
    RECOMMENDED_DATE,
    NEXT_YEAR,
    USER_DATE
}

data class ProgramStartProposal(
    val recommendedDate: LocalDate,
    val selectedDate: LocalDate,
    val recommendationHasPassed: Boolean
) {
    fun resolve(choice: ProgramStartChoice): LocalDate = when (choice) {
        ProgramStartChoice.RECOMMENDED_DATE -> recommendedDate
        ProgramStartChoice.NEXT_YEAR -> recommendedDate.plusYears(1)
        ProgramStartChoice.USER_DATE -> selectedDate.plusDays(1)
    }
}

object ProgramStartPlanner {
    fun propose(
        template: PlantCareTemplate,
        cultivationType: CultivationType,
        climate: ClimateFingerprint,
        selectedDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): ProgramStartProposal {
        val recommendationYear = maxOf(today.year, selectedDate.year)
        val recommendedDate = recommendedStartDate(
            template = template,
            cultivationType = cultivationType,
            climate = climate,
            year = recommendationYear
        )
        return ProgramStartProposal(
            recommendedDate = recommendedDate,
            selectedDate = selectedDate,
            recommendationHasPassed = recommendedDate.isBefore(today)
        )
    }
}

fun recommendedStartDate(
    template: PlantCareTemplate,
    cultivationType: CultivationType,
    climate: ClimateFingerprint,
    year: Int
): LocalDate {
    val springBase = climate.safeSpringDate(year)
    return when (cultivationType) {
        CultivationType.OPEN_GROUND -> springBase.plusDays(template.openGroundStartOffsetDays.toLong())
        CultivationType.GREENHOUSE -> springBase.plusDays(template.greenhouseStartOffsetDays.toLong())
    }
}

object PlantCareCatalog {
    private val templates = listOf(
        PlantCareTemplate(
            id = "tomato",
            canonicalName = "Томат",
            aliases = setOf("томат", "томаты", "помидор", "помидоры"),
            version = 3,
            supportedCultivationTypes = CultivationType.entries.toSet(),
            steps = listOf(
                CareStepTemplate(
                    id = "fitosporin_roots",
                    title = "Обработать корни рассады перед высадкой",
                    offsetDays = 0,
                    productDescription = "Фитоспорин-М Томат, 10 г — БашИнком",
                    note = "Разведите 10 г в 5 л воды. Погрузите корни рассады на 1–2 часа; расход — 1 л на 100–150 растений. Источник: https://www.bashinkom.ru/products/ojz/FitosporinMTomat10g"
                ),
                CareStepTemplate(
                    id = "gumi_omi_planting",
                    title = "Внести удобрение при посадке",
                    offsetDays = 0,
                    productDescription = "Гуми-Оми Томат, Баклажан, Перец — БашИнком",
                    note = "Норма — 700 г на 10 м². Внесите в междурядье на глубину 5–10 см или в угол лунки и отделите от корней слоем земли 2–3 см. Источник: https://www.bashinkom.ru/products/ojz/GumiOMITomatBaklazhanPerets07kg"
                ),
                CareStepTemplate(
                    id = "fitosporin_spraying",
                    title = "Опрыскать томаты для профилактики болезней",
                    offsetDays = 8,
                    windowBeforeDays = 1,
                    windowAfterDays = 2,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 21, 2),
                    productDescription = "Фитоспорин-М Томат, 10 г — БашИнком",
                    note = "Разведите 5 г в 10 л воды; расход — 10 л на 100 м². Первая обработка — через 7–10 дней после высадки, повторная — через 2–3 недели. Работайте утром, вечером или в пасмурную погоду минимум за 2 часа до дождя. Источник: https://www.bashinkom.ru/products/ojz/FitosporinMTomat10g"
                ),
                CareStepTemplate(
                    id = "gumi_omi_feeding",
                    title = "Провести удобрительный полив томатов",
                    offsetDays = 10,
                    windowBeforeDays = 0,
                    windowAfterDays = 3,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 5.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 21, 6),
                    productDescription = "Гуми-Оми Томат, Баклажан, Перец — БашИнком",
                    note = "Разведите 70 г в 10 л воды и настаивайте 2 часа; расход — 10 л на 10 м². Начните через 10 дней после высадки и повторяйте каждые 2–3 недели. Источник: https://www.bashinkom.ru/products/ojz/GumiOMITomatBaklazhanPerets07kg"
                ),
                CareStepTemplate(
                    id = "borogum_flowering",
                    title = "При начале цветения опрыскать Борогумом-М",
                    offsetDays = 28,
                    windowBeforeDays = 0,
                    windowAfterDays = 14,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 28, 2),
                    productDescription = "Борогум-М — БашИнком",
                    note = "Выполните только после начала цветения: 2 ст. ложки на 1,5 л воды, расход раствора — на 50 м². Опрыскивайте вечером. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-tomata/"
                ),
                CareStepTemplate(
                    id = "bogaty_flowering",
                    title = "Подкормить цветущие томаты по листьям",
                    offsetDays = 42,
                    windowBeforeDays = 0,
                    windowAfterDays = 14,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 28, 2),
                    productDescription = "Богатый Овощи — БашИнком",
                    note = "Выполните только во время цветения, чередуя с Борогумом-М: 1 ст. ложка на 5 л воды, расход раствора — на 50 м². Интервал между подкормками — 2 недели. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-tomata/"
                ),
                CareStepTemplate("support", "Проверить подвязку и опору", offsetDays = 7, note = "Подвязка не должна пережимать стебель."),
                CareStepTemplate("leaf_inspection", "Осмотреть листья на признаки стресса", offsetDays = 7, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12), note = "Отметьте пятна, повреждения и вредителей; лечение выбирайте после определения причины.")
            )
        ),
        PlantCareTemplate(
            id = "cucumber",
            canonicalName = "Огурец",
            aliases = setOf("огурец", "огурцы"),
            version = 3,
            supportedCultivationTypes = CultivationType.entries.toSet(),
            steps = listOf(
                CareStepTemplate(
                    id = "gumi_omi_planting",
                    title = "Внести удобрение в лунку перед высадкой",
                    offsetDays = 0,
                    productDescription = "Гуми-Оми Огурец, Кабачок, Бахчевые — БашИнком",
                    note = "Внесите 1 ст. ложку в лунку и тщательно перемешайте с землёй. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-ogurtsa/"
                ),
                CareStepTemplate(
                    id = "kornesil_planting",
                    title = "Полить рассаду после высадки",
                    offsetDays = 0,
                    productDescription = "КорнеСил — БашИнком",
                    note = "Разведите 100 мл препарата в 10 л воды и полейте под корень по 3–4 л на растение. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-ogurtsa/"
                ),
                CareStepTemplate(
                    id = "fitosporin_spraying",
                    title = "Опрыскать огурцы для профилактики болезней",
                    offsetDays = 7,
                    windowBeforeDays = 0,
                    windowAfterDays = 3,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 3),
                    productDescription = "Фитоспорин-М Огурцы, 10 г — БашИнком",
                    note = "Разведите 10 г в 5 л воды; расход — 5 л на 50 м². Проведите 3 опрыскивания: первое профилактическое, следующие с интервалом 10–15 дней. Раствор готовьте за 1–2 часа до применения и используйте в течение суток; обрабатывайте минимум за 2 часа до дождя. Источник: https://www.bashinkom.ru/products/ojz/FitosporinMOGURTSY10g/1"
                ),
                CareStepTemplate(
                    id = "gumi_omi_7_8_leaves",
                    title = "При 7–8 листьях провести корневую подкормку",
                    offsetDays = 14,
                    windowBeforeDays = 0,
                    windowAfterDays = 10,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 5.0),
                    productDescription = "Гуми-Оми Огурец, Кабачок, Бахчевые + Фитоспорин-АС — БашИнком",
                    note = "Выполните только в фазе 7–8 настоящих листьев: растворите 70 г Гуми-Оми и 50 мл Фитоспорина-АС в 10 л воды; полейте под корень по 0,5 л на растение. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-ogurtsa/"
                ),
                CareStepTemplate(
                    id = "borogum_flowering",
                    title = "При начале цветения опрыскать Борогумом-М",
                    offsetDays = 28,
                    windowBeforeDays = 0,
                    windowAfterDays = 14,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 28, 2),
                    productDescription = "Борогум-М — БашИнком",
                    note = "Выполните только после начала цветения: 2 ст. ложки на 1,5 л воды, расход раствора — на 50 м². Опрыскивайте вечером. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-ogurtsa/"
                ),
                CareStepTemplate(
                    id = "bogaty_flowering",
                    title = "Подкормить цветущие огурцы по листьям",
                    offsetDays = 42,
                    windowBeforeDays = 0,
                    windowAfterDays = 14,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 2.0, maximumWindMetersPerSecond = 6.0),
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 28, 2),
                    productDescription = "Богатый Овощи — БашИнком",
                    note = "Выполните только во время цветения, чередуя с Борогумом-М: 1 ст. ложка на 5 л воды, расход раствора — на 50 м². Интервал между подкормками — 2 недели. Источник: https://www.bashinkom.ru/ojz/vyrashchivanie-kultur/tekhnologiya-vyrashchivaniya-ogurtsa/"
                ),
                CareStepTemplate("guide_shoots", "Проверить опору для побегов", offsetDays = 10, note = "Направляйте побеги без резких перегибов."),
                CareStepTemplate("moisture_check", "Проверить влажность почвы", offsetDays = 2, recurrence = CareRecurrence(RepeatType.CUSTOM, 3, 20), note = "Поливайте только при необходимости с учётом осадков и состояния почвы."),
                CareStepTemplate("leaf_inspection", "Осмотреть листья", offsetDays = 7, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12), note = "Ищите изменение окраски, пятна и следы вредителей.")
            )
        ),
        PlantCareTemplate(
            id = "garden-strawberry",
            canonicalName = "Земляника",
            aliases = setOf(
                "земляника", "земляника садовая", "клубника",
                "земляника садовая короткого дня", "земляника садовая ремонтантная",
                "земляника садовая нейтрального дня", "клубника одноразовая",
                "клубника июньская", "клубника ремонтантная", "клубника нейтрального дня"
            ),
            version = 2,
            supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
            steps = listOf(
                CareStepTemplate("moisture_check", "Проверить влажность почвы", offsetDays = 1, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12), note = "Оценивайте почву под мульчей, если она используется."),
                CareStepTemplate("mulch_check", "Проверить мульчу", offsetDays = 5, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 4), note = "Мульча не должна закрывать центр розетки."),
                CareStepTemplate("leaf_inspection", "Осмотреть листья и ягоды", offsetDays = 7, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 10), note = "Удаляйте только явно повреждённые части чистым инструментом.")
            ) + standardTreatmentSteps("садовой земляники")
        ),
        PlantCareTemplate(
            id = "apple",
            canonicalName = "Яблоня",
            aliases = setOf(
                "яблоня", "яблони", "яблоко",
                "яблоня на сильнорослом подвое", "яблоня на карликовом подвое",
                "яблоня стандартная", "яблоня семенной подвой",
                "яблоня карликовая", "яблоня полукарликовая"
            ),
            version = 2,
            supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
            steps = listOf(
                CareStepTemplate("crown_inspection", "Осмотреть крону и ствол", offsetDays = 0, note = "Зафиксируйте повреждения коры, сухие ветви и необычные пятна."),
                CareStepTemplate("trunk_circle", "Проверить приствольный круг", offsetDays = 7, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 6), note = "Не повреждайте поверхностные корни при рыхлении."),
                CareStepTemplate("moisture_check", "Проверить необходимость полива", offsetDays = 10, recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 12), note = "Учитывайте возраст дерева, осадки и влажность почвы.")
            ) + standardTreatmentSteps("яблони")
        ),
        PlantCareTemplate(
            id = "pear",
            canonicalName = "Груша",
            aliases = setOf(
                "груша", "груши", "грушевое дерево",
                "груша на сильнорослом подвое", "груша на слаборослом подвое",
                "груша на карликовом подвое", "груша стандартная",
                "груша карликовая", "груша полукарликовая"
            ),
            version = 2,
            supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
            steps = listOf(
                CareStepTemplate("crown_inspection", "Осмотреть крону и ствол", offsetDays = 0, note = "Зафиксируйте повреждения коры, сухие ветви и необычные пятна."),
                CareStepTemplate("trunk_circle", "Проверить приствольный круг", offsetDays = 7, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 6), note = "Не повреждайте поверхностные корни при рыхлении."),
                CareStepTemplate("moisture_check", "Проверить необходимость полива", offsetDays = 10, recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 12), note = "Учитывайте возраст дерева, осадки и влажность почвы.")
            ) + standardTreatmentSteps("груши")
        ),
        PlantCareTemplate(
            id = "hydrangea",
            canonicalName = "Гортензия",
            aliases = setOf("гортензия", "гортензии"),
            version = 2,
            supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
            steps = listOf(
                CareStepTemplate("moisture_check", "Проверить влажность почвы", offsetDays = 0, recurrence = CareRecurrence(RepeatType.CUSTOM, 3, 24), note = "Ориентируйтесь на фактическую влажность, а не только на календарь."),
                CareStepTemplate("mulch_check", "Проверить слой мульчи", offsetDays = 5, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 5), note = "Не укладывайте мульчу вплотную к основанию побегов."),
                CareStepTemplate("leaf_inspection", "Осмотреть листья и побеги", offsetDays = 7, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12), note = "Отмечайте увядание, пятна и повреждения, прежде чем выбирать обработку.")
            ) + standardTreatmentSteps("гортензии")
        ),
        seasonalVegetable(
            id = "potato",
            name = "Картофель",
            aliases = setOf("картофель", "картошка"),
            openGroundStartOffsetDays = -7,
            moistureIntervalDays = 7,
            cropSpecificTask = "Проверить окучивание",
            cropSpecificNote = "Подсыпайте почву только при необходимости, не засыпая листья."
        ),
        seasonalVegetable(
            id = "sweet-pepper",
            name = "Перец сладкий",
            aliases = setOf("перец", "перец сладкий", "болгарский перец"),
            openGroundStartOffsetDays = 14,
            greenhouseStartOffsetDays = -10,
            moistureIntervalDays = 4,
            cropSpecificTask = "Проверить опоры и завязи",
            cropSpecificNote = "Подвязывайте побеги свободно и не удаляйте здоровые завязи без причины."
        ),
        seasonalVegetable(
            id = "eggplant",
            name = "Баклажан",
            aliases = setOf("баклажан", "баклажаны"),
            openGroundStartOffsetDays = 16,
            greenhouseStartOffsetDays = -8,
            moistureIntervalDays = 4,
            cropSpecificTask = "Проверить опору побегов",
            cropSpecificNote = "Не допускайте пережимания стеблей подвязкой."
        ),
        seasonalVegetable(
            id = "zucchini",
            name = "Кабачок",
            aliases = setOf(
                "кабачок", "кабачки", "цуккини",
                "кабачок кустовой", "кабачок плетистый",
                "цуккини кустовой", "цуккини плетистый"
            ),
            openGroundStartOffsetDays = 12,
            moistureIntervalDays = 5,
            cropSpecificTask = "Осмотреть центр куста",
            cropSpecificNote = "Удаляйте только явно повреждённые части чистым инструментом."
        ),
        seasonalVegetable(
            id = "pumpkin",
            name = "Тыква",
            aliases = setOf("тыква", "тыквы"),
            openGroundStartOffsetDays = 14,
            moistureIntervalDays = 6,
            cropSpecificTask = "Проверить плети и свободное место",
            cropSpecificNote = "Направляйте плети без резких перегибов и повреждения узлов."
        ),
        seasonalVegetable(
            id = "cabbage",
            name = "Капуста белокочанная",
            aliases = setOf("капуста", "капуста белокочанная", "белокочанная капуста"),
            openGroundStartOffsetDays = -10,
            moistureIntervalDays = 5,
            cropSpecificTask = "Осмотреть кочан и нижнюю сторону листьев",
            cropSpecificNote = "Отмечайте кладки и повреждения, не применяя средство до определения причины."
        ),
        seasonalVegetable(
            id = "carrot",
            name = "Морковь",
            aliases = setOf("морковь", "морковка"),
            openGroundStartOffsetDays = -14,
            moistureIntervalDays = 6,
            cropSpecificTask = "Проверить густоту всходов",
            cropSpecificNote = "Прореживайте только после появления устойчивых всходов и увлажнения почвы."
        ),
        seasonalVegetable(
            id = "beet",
            name = "Свёкла",
            aliases = setOf("свекла", "свёкла", "свекла столовая", "свёкла столовая"),
            openGroundStartOffsetDays = -7,
            moistureIntervalDays = 7,
            cropSpecificTask = "Проверить густоту всходов",
            cropSpecificNote = "Оставляйте более сильные растения, не повреждая корни соседних."
        ),
        seasonalVegetable(
            id = "onion",
            name = "Лук репчатый",
            aliases = setOf("лук", "лук репчатый", "репчатый лук"),
            openGroundStartOffsetDays = -18,
            moistureIntervalDays = 7,
            cropSpecificTask = "Осмотреть перо и шейку",
            cropSpecificNote = "Отмечайте пожелтение и размягчение; не увлажняйте посадки автоматически."
        ),
        seasonalVegetable(
            id = "garlic",
            name = "Чеснок",
            aliases = setOf(
                "чеснок", "чеснока", "чеснок стрелкующийся", "чеснок нестрелкующийся",
                "чеснок озимый стрелкующийся", "чеснок hardneck", "чеснок softneck"
            ),
            openGroundStartOffsetDays = -21,
            moistureIntervalDays = 8,
            cropSpecificTask = "Осмотреть листья и основание",
            cropSpecificNote = "Проверяйте посадки на пожелтение, повреждения и переувлажнение."
        )
    ).map(PlantCareTemplate::withoutSeasonLabels)

    private fun standardTreatmentSteps(cropLabel: String): List<CareStepTemplate> = listOf(
        CareStepTemplate(
            id = "preventive_disease_treatment",
            title = "Профилактическая обработка от болезней",
            offsetDays = 18,
            windowBeforeDays = 2,
            windowAfterDays = 3,
            weatherLimits = WeatherLimits(maximumPrecipitationMm = 3.0, maximumWindMetersPerSecond = 6.0),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 5),
            productDescription = "Средство профилактического действия против грибных и бактериальных болезней, разрешённое для $cropLabel.",
            note = "Конкретный препарат и дозировку выбирайте по актуальной инструкции с учётом культуры и стадии развития."
        ),
        CareStepTemplate(
            id = "pest_treatment_if_needed",
            title = "Обработка от вредителей при необходимости",
            offsetDays = 21,
            windowBeforeDays = 1,
            windowAfterDays = 3,
            weatherLimits = WeatherLimits(maximumPrecipitationMm = 3.0, maximumWindMetersPerSecond = 6.0),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 10, 8),
            productDescription = "Средство против сосущих и листогрызущих вредителей, разрешённое для $cropLabel.",
            note = "Проводите обработку только после подтверждения вредителя; соблюдайте срок ожидания и инструкцию выбранного средства."
        )
    )

    private fun seasonalVegetable(
        id: String,
        name: String,
        aliases: Set<String>,
        openGroundStartOffsetDays: Int,
        greenhouseStartOffsetDays: Int = -14,
        moistureIntervalDays: Int,
        cropSpecificTask: String,
        cropSpecificNote: String
    ): PlantCareTemplate = PlantCareTemplate(
        id = id,
        canonicalName = name,
        aliases = aliases,
        version = 2,
        supportedCultivationTypes = if (greenhouseStartOffsetDays < 0 && id in setOf("sweet-pepper", "eggplant")) {
            CultivationType.entries.toSet()
        } else {
            setOf(CultivationType.OPEN_GROUND)
        },
        openGroundStartOffsetDays = openGroundStartOffsetDays,
        greenhouseStartOffsetDays = greenhouseStartOffsetDays,
        steps = listOf(
            CareStepTemplate(
                id = "adaptation_check",
                title = "Проверить состояние растения",
                offsetDays = 4,
                note = "Оцените рост, окраску и упругость листьев; зафиксируйте необычные изменения."
            ),
            CareStepTemplate(
                id = "moisture_check",
                title = "Проверить влажность почвы",
                offsetDays = 2,
                recurrence = CareRecurrence(RepeatType.CUSTOM, moistureIntervalDays, 16),
                note = "Решение о поливе принимайте по фактической влажности почвы и недавним осадкам."
            ),
            CareStepTemplate(
                id = "crop_specific_check",
                title = cropSpecificTask,
                offsetDays = 9,
                recurrence = CareRecurrence(RepeatType.CUSTOM, 10, 8),
                note = cropSpecificNote
            ),
            CareStepTemplate(
                id = "leaf_inspection",
                title = "Осмотреть на признаки стресса и вредителей",
                offsetDays = 7,
                recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12),
                note = "Сначала определите возможную причину; препарат выбирайте отдельно и применяйте только по инструкции."
            )
        ) + standardTreatmentSteps(name.lowercase(Locale.forLanguageTag("ru")))
    )

    fun find(userInput: String): PlantCareTemplate? {
        val normalized = normalizePlantName(userInput)
        return templates.firstOrNull { template ->
            normalized == normalizePlantName(template.canonicalName) ||
                template.aliases.any { normalizePlantName(it) == normalized }
        }
    }

    fun all(): List<PlantCareTemplate> = templates
}

class CareProgramGenerator {
    fun generate(
        template: PlantCareTemplate,
        context: CareProgramContext,
        instanceId: String = UUID.randomUUID().toString()
    ): GeneratedCareProgram {
        require(context.cultivationType in template.supportedCultivationTypes) {
            "Для выбранного способа выращивания программа пока не подготовлена"
        }

        val recommendedStart = recommendedStartDate(
            template = template,
            cultivationType = context.cultivationType,
            climate = context.climate,
            year = context.startDate.year
        )
        val warning = if (context.startDate.isBefore(recommendedStart)) {
            "Выбранная дата раньше рекомендуемого начала работ ${recommendedStart}. Проверьте местные условия."
        } else {
            null
        }
        val forecastByDate = context.forecast.associateBy { it.date }

        val generatedSteps = template.steps.map { step ->
            val anchorDate = when (step.anchor) {
                CareAnchor.START_DATE -> context.startDate
                CareAnchor.SAFE_SPRING_DATE -> recommendedStart
            }
            val initialDate = anchorDate.plusDays(step.offsetDays.toLong())
            val windowStart = initialDate.minusDays(step.windowBeforeDays.toLong())
            val windowEnd = initialDate.plusDays(step.windowAfterDays.toLong())
            val forecastCoversWindow = forecastByDate.keys.any { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
            val suitableDate = candidateOffsets(step.windowBeforeDays, step.windowAfterDays)
                .map { initialDate.plusDays(it.toLong()) }
                .filter { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
                .firstOrNull { date ->
                    val weather = forecastByDate[date] ?: return@firstOrNull false
                    weather.satisfies(step.weatherLimits)
                }
            val hasLimits = step.weatherLimits != WeatherLimits()
            val scheduledDate = when {
                !hasLimits -> initialDate
                suitableDate != null -> suitableDate
                else -> initialDate
            }
            val needsWeatherConfirmation = hasLimits && suitableDate == null
            val explanation = when {
                hasLimits && suitableDate != null && suitableDate != initialDate ->
                    "Дата сдвинута с $initialDate на $suitableDate по прогнозу погоды."
                hasLimits && suitableDate != null ->
                    "Прогноз на $scheduledDate соответствует условиям выполнения."
                hasLimits && forecastCoversWindow ->
                    "В доступном прогнозе нет полностью подходящего дня; проверьте погоду перед выполнением."
                hasLimits ->
                    "Дата рассчитана по программе; прогноз будет полезно проверить ближе к сроку."
                else ->
                    "Дата рассчитана относительно ${if (step.anchor == CareAnchor.START_DATE) "начала ухода" else "безопасного начала сезона"}."
            }

            GeneratedCareStep(
                templateStepId = step.id,
                title = step.title,
                scheduledDate = scheduledDate,
                windowStart = windowStart,
                windowEnd = windowEnd,
                recurrence = step.recurrence,
                weatherAdjusted = suitableDate != null && suitableDate != initialDate,
                needsWeatherConfirmation = needsWeatherConfirmation,
                explanation = explanation,
                productDescription = step.productDescription,
                note = step.note
            )
        }

        return GeneratedCareProgram(
            instanceId = instanceId,
            templateId = template.id,
            templateVersion = template.version,
            plantName = template.canonicalName,
            cultivationType = context.cultivationType,
            recommendedStartDate = recommendedStart,
            chosenStartDate = context.startDate,
            climateSummary = context.climate.displayName(),
            warning = warning,
            steps = generatedSteps
        )
    }

    private fun candidateOffsets(beforeDays: Int, afterDays: Int): List<Int> =
        (-beforeDays..afterDays).sortedWith(compareBy<Int> { it.absoluteValue }.thenBy { it })

    private fun ForecastWeatherDay.satisfies(limits: WeatherLimits): Boolean =
        (limits.minimumNightTemperatureC == null || minimumTemperatureC >= limits.minimumNightTemperatureC) &&
            (limits.maximumPrecipitationMm == null || precipitationMm <= limits.maximumPrecipitationMm) &&
            (limits.maximumWindMetersPerSecond == null || maximumWindMetersPerSecond <= limits.maximumWindMetersPerSecond)
}

private val seasonLabel = Regex(
    "(?iu)(?<!\\p{L})(летняя|осенняя|весенняя|предзимняя|ранневесенняя|позднезимняя)(?!\\p{L})"
)

internal fun careTitleWithoutSeasonLabel(title: String): String {
    val cleaned = title
        .replace(seasonLabel, "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .removePrefix("или ")
        .trim()
    return cleaned.replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru")) }
}

private fun PlantCareTemplate.withoutSeasonLabels(): PlantCareTemplate = copy(
    steps = steps.map { step -> step.copy(title = careTitleWithoutSeasonLabel(step.title)) }
)

fun normalizePlantName(value: String): String = Normalizer
    .normalize(value.trim().lowercase(Locale.forLanguageTag("ru")), Normalizer.Form.NFD)
    .replace("ё", "е")
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("[^а-яa-z0-9\\s-]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
