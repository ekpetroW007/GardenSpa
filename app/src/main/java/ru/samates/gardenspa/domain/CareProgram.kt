package ru.samates.gardenspa.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
    val maximumWindMetersPerSecond: Double? = null,
    val requiredDryHoursAfter: Int = 0
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
    val note: String,
    val reminderDaysBefore: Int? = null
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

const val NO_REMAINING_CARE_MESSAGE =
    "Эту программу нельзя продолжить с выбранной даты: при старте в рекомендуемую дату все предусмотренные процедуры уже должны были быть выполнены. Начните программу в следующем году или добавьте обработки вручную."

data class ProgramStartProposal(
    val recommendedDate: LocalDate,
    val selectedDate: LocalDate,
    val recommendationHasPassed: Boolean
) {
    fun resolve(choice: ProgramStartChoice): LocalDate = when (choice) {
        ProgramStartChoice.RECOMMENDED_DATE -> recommendedDate
        ProgramStartChoice.NEXT_YEAR -> recommendedDate.plusYears(1)
        ProgramStartChoice.USER_DATE -> selectedDate
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
    private val templates = (listOf(
        PlantCareTemplate(
            id = "tomato",
            canonicalName = "Томат",
            aliases = setOf("томат", "томаты", "помидор", "помидоры"),
            version = 5,
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
                    weatherLimits = WeatherLimits(
                        maximumPrecipitationMm = 2.0,
                        maximumWindMetersPerSecond = 6.0,
                        requiredDryHoursAfter = 2
                    ),
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
            )
        ),
        PlantCareTemplate(
            id = "cucumber",
            canonicalName = "Огурец",
            aliases = setOf("огурец", "огурцы"),
            version = 5,
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
                    weatherLimits = WeatherLimits(
                        maximumPrecipitationMm = 2.0,
                        maximumWindMetersPerSecond = 6.0,
                        requiredDryHoursAfter = 2
                    ),
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
            )
        ),
        seasonalVegetable(
            id = "sweet-pepper",
            name = "Перец сладкий",
            aliases = setOf("перец", "перец сладкий", "болгарский перец"),
            openGroundStartOffsetDays = 14,
            greenhouseStartOffsetDays = -10
        ),
        seasonalVegetable(
            id = "eggplant",
            name = "Баклажан",
            aliases = setOf("баклажан", "баклажаны"),
            openGroundStartOffsetDays = 16,
            greenhouseStartOffsetDays = -8
        ),
        seasonalVegetable(
            id = "zucchini",
            name = "Кабачок",
            aliases = setOf(
                "кабачок", "кабачки", "цуккини",
                "кабачок кустовой", "кабачок плетистый",
                "цуккини кустовой", "цуккини плетистый"
            ),
            openGroundStartOffsetDays = 12
        ),
        seasonalVegetable(
            id = "pumpkin",
            name = "Тыква",
            aliases = setOf("тыква", "тыквы"),
            openGroundStartOffsetDays = 14
        ),
        seasonalVegetable(
            id = "cabbage",
            name = "Капуста белокочанная",
            aliases = setOf("капуста", "капуста белокочанная", "белокочанная капуста"),
            openGroundStartOffsetDays = -10
        ),
        seasonalVegetable(
            id = "carrot",
            name = "Морковь",
            aliases = setOf("морковь", "морковка"),
            openGroundStartOffsetDays = -14
        ),
        seasonalVegetable(
            id = "beet",
            name = "Свёкла",
            aliases = setOf("свекла", "свёкла", "свекла столовая", "свёкла столовая"),
            openGroundStartOffsetDays = -7
        ),
        seasonalVegetable(
            id = "onion",
            name = "Лук репчатый",
            aliases = setOf("лук", "лук репчатый", "репчатый лук"),
            openGroundStartOffsetDays = -18
        ),
        seasonalVegetable(
            id = "garlic",
            name = "Чеснок",
            aliases = setOf(
                "чеснок", "чеснока", "чеснок стрелкующийся", "чеснок нестрелкующийся",
                "чеснок озимый стрелкующийся", "чеснок hardneck", "чеснок softneck"
            ),
            openGroundStartOffsetDays = -21
        )
    ) + SeasonalCarePrograms.templates.map(SpringCarePrograms::withSpringStages)).map(PlantCareTemplate::withoutSeasonLabels)

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
        greenhouseStartOffsetDays: Int = -14
    ): PlantCareTemplate = PlantCareTemplate(
        id = id,
        canonicalName = name,
        aliases = aliases,
        version = 3,
        supportedCultivationTypes = if (greenhouseStartOffsetDays < 0 && id in setOf("sweet-pepper", "eggplant")) {
            CultivationType.entries.toSet()
        } else {
            setOf(CultivationType.OPEN_GROUND)
        },
        openGroundStartOffsetDays = openGroundStartOffsetDays,
        greenhouseStartOffsetDays = greenhouseStartOffsetDays,
        steps = standardTreatmentSteps(name.lowercase(Locale.forLanguageTag("ru")))
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
        val continuesStartedSeason = context.startDate.isAfter(recommendedStart)
        val warning = when {
            context.startDate.isBefore(recommendedStart) ->
                "Выбранная дата раньше рекомендуемого начала работ ${recommendedStart}. Проверьте местные условия."
            continuesStartedSeason ->
                "Программа продолжена с ${context.startDate}: работы и повторы, срок которых уже прошёл, исключены."
            else -> null
        }
        val forecastByDate = context.forecast.associateBy { it.date }

        val generatedSteps = template.steps.map { step ->
            val anchorDate = when (step.anchor) {
                CareAnchor.START_DATE -> if (continuesStartedSeason) recommendedStart else context.startDate
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
        }.let { steps ->
            if (continuesStartedSeason) {
                steps.flatMap { it.remainingFrom(context.startDate) }
            } else {
                steps
            }
        }

        require(generatedSteps.isNotEmpty()) {
            NO_REMAINING_CARE_MESSAGE
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

    private fun GeneratedCareStep.remainingFrom(date: LocalDate): List<GeneratedCareStep> {
        if (!scheduledDate.isBefore(date)) return listOf(this)

        val repeat = recurrence
        if (repeat == null || repeat.type == RepeatType.NONE) {
            if (windowEnd.isBefore(date)) return emptyList()
            return listOf(
                copy(
                    scheduledDate = date,
                    windowStart = maxOf(windowStart, date),
                    recurrence = null,
                    explanation = "Срок работы ещё не завершился; при продолжении программы она назначена на $date."
                )
            )
        }

        val occurrences = (0 until repeat.count).map { index ->
            occurrenceDate(scheduledDate, repeat, index)
        }
        val remaining = occurrences.dropWhile { it.isBefore(date) }
        if (remaining.isEmpty()) return emptyList()

        val nextDate = remaining.first()
        val skipped = occurrences.size - remaining.size
        if (repeat.type == RepeatType.MONTHLY || repeat.type == RepeatType.YEARLY) {
            return remaining.mapIndexed { index, occurrence ->
                val shiftDays = ChronoUnit.DAYS.between(scheduledDate, occurrence)
                copy(
                    templateStepId = "$templateStepId:remaining:${skipped + index + 1}",
                    scheduledDate = occurrence,
                    windowStart = windowStart.plusDays(shiftDays),
                    windowEnd = windowEnd.plusDays(shiftDays),
                    recurrence = null,
                    explanation = "Программа продолжена с $date: сохранена оставшаяся работа ${index + 1} из ${remaining.size}."
                )
            }
        }

        val shiftDays = ChronoUnit.DAYS.between(scheduledDate, nextDate)
        return listOf(
            copy(
                scheduledDate = nextDate,
                windowStart = windowStart.plusDays(shiftDays),
                windowEnd = windowEnd.plusDays(shiftDays),
                recurrence = repeat.copy(count = remaining.size),
                explanation = "Программа продолжена с $date: пропущено прошедших повторов — $skipped, осталось — ${remaining.size}."
            )
        )
    }

    private fun occurrenceDate(start: LocalDate, recurrence: CareRecurrence, index: Int): LocalDate {
        val amount = recurrence.interval.coerceAtLeast(1).toLong() * index
        return when (recurrence.type) {
            RepeatType.NONE -> start
            RepeatType.DAILY, RepeatType.CUSTOM -> start.plusDays(amount)
            RepeatType.WEEKLY -> start.plusWeeks(amount)
            RepeatType.MONTHLY -> start.plusMonths(amount)
            RepeatType.YEARLY -> start.plusYears(amount)
        }
    }

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
