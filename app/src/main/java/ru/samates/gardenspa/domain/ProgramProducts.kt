package ru.samates.gardenspa.domain

import java.time.LocalDate
import java.util.UUID
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId

enum class ProductSection(val title: String) {
    TREATMENT("Средства для обработки"), FERTILIZER("Удобрения")
}

/** Product instructions are independent. Task-level alternatives are not dose equivalents. */
data class ProgramProduct(
    val id: String,
    val name: String,
    val manufacturer: String,
    val taskTitle: String,
    val purpose: String,
    val instruction: String,
    val sourceUrl: String,
    val crops: Set<String> = setOf("tomato", "cucumber"),
    val problems: Set<String> = emptySet(),
    val cultivationTypes: Set<CultivationType> = CultivationType.entries.toSet(),
    val unavailableReason: String? = null,
    val sections: Set<ProductSection> = setOf(ProductSection.TREATMENT)
) {
    val displayName: String get() = "$name — $manufacturer"
    val note: String get() = "$purpose\n$instruction\nИсточник: $sourceUrl\n$PRODUCT_LABEL_NOTICE\n$SINGLE_PRODUCT_WORK_NOTICE"
}

const val PRODUCT_LABEL_NOTICE = "Сверьте точную форму препарата, культуру, грунт, цель обработки, норму, срок ожидания и меры защиты с этикеткой. Не смешивайте выбранные альтернативы."
const val SINGLE_PRODUCT_WORK_NOTICE = "Назначается одна работа. Старые дозировка и повторы не переносятся. Следующие обработки добавляйте по инструкции выбранного средства, учитывая уже выполненные. Погодное окно для этого средства автоматически не рассчитывается: сравните прогноз с его инструкцией."

enum class PlantProblemKind(val label: String) { DISEASE("Болезнь"), PEST("Вредитель") }

data class PlantProblem(val id: String, val label: String, val kind: PlantProblemKind, val crops: Set<String>)

object ProgramProductCatalog {
    val supportedCrops = setOf("tomato", "cucumber", "peony", "hydrangea", "rose", "blackberry",
        "raspberry", "currant", "garden-strawberry", "blueberry", "apple", "pear", "potato", "lawn")

    val allowedManufacturers = setOf("Август", "ЩёлковоАгрохим", "ФМРус", "BonaForte", "Фаско", "Гера",
        "Ортон", "Агрикола", "HB-101", "БашИнком", "Фармбиомед", "Syngenta", "Нэст-М", "Петрович",
        "Green Belt", "Био-комплекс", "Аминосил", "Органик Микс", "5сезонов",
        "НПФ Собер") // Manufacturer of 30 Plus, explicitly requested in the spring-program update.

    fun supports(programId: String?): Boolean = programId in supportedCrops

    val problems = listOf(
        PlantProblem("late_blight", "Фитофтороз", PlantProblemKind.DISEASE, setOf("tomato")),
        PlantProblem("alternaria", "Альтернариоз", PlantProblemKind.DISEASE, setOf("tomato")),
        PlantProblem("powdery_mildew", "Настоящая мучнистая роса", PlantProblemKind.DISEASE, setOf("tomato", "cucumber")),
        PlantProblem("downy_mildew", "Пероноспороз (ложная мучнистая роса)", PlantProblemKind.DISEASE, setOf("cucumber")),
        PlantProblem("gray_mold", "Серая гниль", PlantProblemKind.DISEASE, setOf("tomato", "cucumber")),
        PlantProblem("root_rot", "Корневая гниль / увядание", PlantProblemKind.DISEASE, setOf("tomato", "cucumber")),
        PlantProblem("whitefly", "Тепличная белокрылка", PlantProblemKind.PEST, setOf("tomato", "cucumber")),
        PlantProblem("aphids", "Тля", PlantProblemKind.PEST, setOf("tomato", "cucumber")),
        PlantProblem("spider_mite", "Паутинный клещ", PlantProblemKind.PEST, setOf("tomato", "cucumber")),
        PlantProblem("thrips", "Трипсы", PlantProblemKind.PEST, setOf("tomato", "cucumber"))
    ).map { problem ->
        problem.copy(crops = problem.crops + when (problem.id) {
            "late_blight", "alternaria" -> setOf("potato")
            "powdery_mildew" -> supportedCrops - setOf("potato")
            "gray_mold" -> setOf("peony", "rose", "hydrangea", "blackberry", "raspberry", "currant", "garden-strawberry", "blueberry")
            "root_rot" -> supportedCrops
            "aphids" -> supportedCrops - "lawn"
            "spider_mite", "thrips" -> setOf("peony", "rose", "hydrangea", "blackberry", "raspberry", "currant", "garden-strawberry", "apple", "pear")
            else -> emptySet()
        })
    } + listOf(
        PlantProblem("scab", "Парша", PlantProblemKind.DISEASE, setOf("apple", "pear")),
        PlantProblem("leaf_spot", "Пятнистости листьев", PlantProblemKind.DISEASE,
            setOf("peony", "rose", "hydrangea", "blackberry", "raspberry", "currant", "garden-strawberry", "blueberry", "lawn")),
        PlantProblem("rust", "Ржавчина", PlantProblemKind.DISEASE, setOf("rose", "pear", "raspberry", "currant", "lawn")),
        PlantProblem("snow_mold", "Снежная плесень", PlantProblemKind.DISEASE, setOf("lawn")),
        PlantProblem("colorado_beetle", "Колорадский жук", PlantProblemKind.PEST, setOf("potato")),
        PlantProblem("weevil", "Долгоносик", PlantProblemKind.PEST, setOf("garden-strawberry", "raspberry", "blackberry", "apple")),
        PlantProblem("codling_moth", "Плодожорка", PlantProblemKind.PEST, setOf("apple", "pear"))
    )

    private const val SILVER_URL = "https://bio-kompleks.ru/catalog/vsya_produktsiya/bio_kompleks_serebromedin_/"
    private const val SILVER_CAUTION = "Не смешивать с другими средствами и не использовать для обеззараживания почвы. На сайте расходятся сведения о сроке ожидания: уточните его по своей упаковке. Производитель указывает, что продукт не является пестицидом. Это описание производителя, а не гарантия излечения."

    val products = listOf(
        ProgramProduct("silver_prevention", "Серебромедин, концентрат", "Био-комплекс",
            "Провести листовую профилактику болезней", "Альтернатива по задаче профилактики, другой состав и регламент.",
            "По карточке: 40 мл на 1 л воды, смачивание листьев с обеих сторон, в сухую погоду выше +15 °C. Профилактическая частота — 1–2 раза в месяц. $SILVER_CAUTION", SILVER_URL),
        ProgramProduct("organic_tomato", "Удобрение для томатов, гранулы", "Органик Микс",
            "Внести гранулированное удобрение для томатов", "Частичная альтернатива по питанию. Отличается составом и способом внесения.",
            "20–40 г на растение. Распределить по поверхности, заделать на 5 см и полить. По карточке интервал 30–45 дней, сезон с апреля по октябрь. Это не раствор для опрыскивания.",
            "https://organic-mix.ru/catalog/udobrenie-dlya-tomatov-organik-miks-50-gr/", crops = setOf("tomato")),
        ProgramProduct("organic_cucumber", "Удобрение для огурцов, гранулы", "Органик Микс",
            "Подкормить взрослый огурец гранулами", "Частичная альтернатива по питанию взрослого растения. Не заменяет защиту от болезней.",
            "Для взрослого огурца 50 г на куст. Распределить по поверхности, заделать на 5 см и полить. По карточке интервал 30–45 дней, с апреля по октябрь. Не переносить эту норму на рассаду.",
            "https://organic-mix.ru/catalog/udobrenie-dlya-ogurtsov-organik-miks-50-gr/", crops = setOf("cucumber")),
        ProgramProduct("amino_tomato", "Витамины для томатов, концентрат", "Аминосил",
            "Подкормить томаты до цветения", "Частичная альтернатива по питанию. Не является заменой фунгицида или инсектицида.",
            "Для взрослых растений до цветения: 20 мл на 1 л воды, корневой полив 1–4 л на растение до увлажнения почвы. Частота по карточке — раз в 2 недели до цветения. Не продолжать этот режим автоматически после начала цветения.",
            "https://aminosil.ru/vitamini-dlya-tomatov-v-forme-kontsentrata", crops = setOf("tomato")),
        ProgramProduct("amino_cucumber_planting", "Витамины для огурцов, концентрат", "Аминосил",
            "Полить рассаду огурцов после высадки", "Частичная альтернатива по поддержке рассады после высадки, другой состав.",
            "20 мл на 1 л воды. После высадки рассады полить по 0,5 л на растение. Используется собственная норма Аминосила, а не расход КорнеСила.",
            "https://aminosil.ru/vitamini-dlya-ogurtsov-v-forme-kontsentrata", crops = setOf("cucumber")),
        ProgramProduct("amino_cucumber", "Витамины для огурцов, концентрат", "Аминосил",
            "Подкормить огурцы до цветения", "Частичная альтернатива только по питанию. Фитоспорин в состав не входит, защитную часть исходной смеси не заменяет.",
            "Для взрослых растений до цветения: 20 мл на 1 л воды, полив 1–4 л на растение до равномерного увлажнения. По карточке — раз в 2 недели до цветения. Защитную обработку согласуйте отдельно, совместимость смеси не предполагается.",
            "https://aminosil.ru/vitamini-dlya-ogurtsov-v-forme-kontsentrata", crops = setOf("cucumber")),
        ProgramProduct("rostobion_seedling", "Ростобион", "Био-комплекс",
            "Полить рассаду Ростобионом", "Частичная альтернатива по поддержке рассады, не по лечению инфекции.",
            "Карточка для полива рассады: 50 мл на 1 л воды, раз в неделю. Расход на растение уточнить по упаковке и объёму корневого кома. Не переносить норму для взрослой овощной культуры.",
            "https://bio-kompleks.ru/catalog/vsya_produktsiya/bio_kompleks_rostobion/", crops = setOf("cucumber")),
        ProgramProduct("maxi_nutrition", "MAXI Гумус", "5сезонов",
            "Подкормить растение MAXI Гумусом", "Кандидат по задаче органического питания.",
            "Официальный каталог подтверждает назначение, но не даёт достаточного регламента для этой операции.",
            "https://5-sezonov.ru/", unavailableReason = "Нужна инструкция производителя с нормой для культуры. Автоматическое назначение пока недоступно."),
        ProgramProduct("silver_disease", "Серебромедин, концентрат", "Био-комплекс",
            "Обработать растение при подтверждённой болезни", "Частичная альтернатива по заявленной производителем задаче. Только для совпадающей цели на упаковке.",
            "По карточке при заболевании: 80 мл на 1 л воды, опрыскивание в сухую погоду выше +15 °C, интервал 7 дней. При быстром развитии болезни требуется агроном. $SILVER_CAUTION", SILVER_URL,
            problems = setOf("late_blight", "powdery_mildew", "downy_mildew")),
        ProgramProduct("muchnistop", "Мучнистоп, готовый спрей 1 л", "Органик Микс",
            "Обработать листья от настоящей мучнистой росы", "Альтернатива по задаче при настоящей мучнистой росе. Не путать с пероноспорозом.",
            "Не разводить. Нанести на обе стороны листьев и молодые побеги, не на цветы и плоды. Температура +10…+25 °C, без ожидаемого дождя 8–10 часов. По карточке повтор 8–12 дней (не раньше 7), ожидание до сбора 1 день. Сверить этикетку.",
            "https://organic-mix.ru/catalog/muchnistop-sprey-ot-muchnistoy-rosy-1-l-organik-miks/", problems = setOf("powdery_mildew")),
        ProgramProduct("reanimator", "Фитоспорин-М РеаниматоР, 0,2 л", "БашИнком",
            "Обработать растение Реаниматором", "Кандидат для подтверждённой болезни, не профилактический порошок Фитоспорина.",
            "У производителя есть разные концентрации по степени поражения. Нельзя выбрать их автоматически по одному названию болезни. Нужна точная инструкция для культуры, цели, степени поражения и срока ожидания.",
            "https://new.bashinkom.ru/products/ojz/FitosporinMReanimatoR02l", problems = setOf("late_blight", "alternaria", "powdery_mildew", "downy_mildew", "gray_mold"),
            unavailableReason = "Точный регламент для выбранного случая требует проверки этикетки / консультации производителя."),
        ProgramProduct("boverix", "Боверикс, Ж, 200/500 мл", "БашИнком",
            "Обработать растение от тепличной белокрылки", "Альтернатива по задаче против тепличной белокрылки. Только защищённый грунт.",
            "Розничная таблица: 100 мл на 10 л воды, расход 0,5–1 л на 10 м². Курс 4 обработки через 10–14 дней. В магазине производителя указан срок ожидания 5 суток: https://bashinkom-v-dom.ru/product/boveriks-zh-500-ml . Сверьте этикетку формы, допуск для ЛПХ и ограничения для пчёл. Курс не назначается автоматически.",
            "https://new.bashinkom.ru/products/ojz/BoveriksZH500ml200ml", problems = setOf("whitefly"), cultivationTypes = setOf(CultivationType.GREENHOUSE)),
        ProgramProduct("biozashchitin", "Биозащитин, концентрат 5 мл", "Органик Микс",
            "Обработать растение от обнаруженного вредителя", "Частичная контактная альтернатива при начальном заселении. Состав и эффективность отличаются от Боверикса.",
            "5 мл на 1 л воды. Смачивать обе стороны листьев, не под прямым солнцем. По карточке при вредителях интервал 7 дней, отсутствие ожидания заявляет производитель. Проверьте переносимость на небольшом участке. Предельная кратность и дождестойкость требуют сверки этикетки.",
            "https://organic-mix.ru/catalog/biozashchitin-v-ampule-organicheskoe-sredstvo-zashchity-rasteniy-3-v-1-5-ml/", problems = setOf("whitefly", "aphids", "spider_mite", "thrips")),
        ProgramProduct("mag_whitefly", "МАГ от белокрылки, концентрат", "Био-комплекс",
            "Обработать растение от белокрылки", "Кандидат по задаче против белокрылки.",
            "На странице расходятся нормы разведения. Не выбирать одну самостоятельно. Есть предупреждение не опрыскивать рассаду.",
            "https://bio-kompleks.ru/catalog/vsya_produktsiya/bio_kompleks_mag_sredstvo_ot_belokrylki/", problems = setOf("whitefly"),
            unavailableReason = "Нужна актуальная этикетка без противоречий в дозировке."),
        ProgramProduct("mag_mite", "МАГ от паутинного клеща, концентрат", "Био-комплекс",
            "Обработать растение от паутинного клеща", "Кандидат по задаче против паутинного клеща.",
            "На странице расходятся нормы разведения. Не выбирать одну самостоятельно. Есть предупреждение не опрыскивать рассаду.",
            "https://bio-kompleks.ru/catalog/vsya_produktsiya/bio_kompleks_mag_sredstvo_ot_pautinnogo_kleshcha/", problems = setOf("spider_mite"),
            unavailableReason = "Нужна актуальная этикетка без противоречий в дозировке."),
        ProgramProduct("maxi_mite", "MAXI Гумат противоклещ с метаризином", "5сезонов",
            "Обработать растение против клеща", "Кандидат по названию задачи, не подтверждённая замена против паутинного клеща.",
            "Не подтверждены вид клеща, способ обработки, штамм, титр, норма и срок ожидания. Органическое удобрение не считается зарегистрированным акарицидом по названию.",
            "https://5-sezonov.ru/", problems = setOf("spider_mite"),
            unavailableReason = "Назначение недоступно до подтверждения регламента производителем.")
    ).map { product ->
        when (product.id) {
            "organic_tomato", "organic_cucumber", "amino_tomato", "amino_cucumber_planting",
            "amino_cucumber", "rostobion_seedling", "maxi_nutrition" ->
                product.copy(sections = setOf(ProductSection.FERTILIZER))
            "biozashchitin" -> product.copy(crops = supportedCrops - "lawn")
            "muchnistop" -> product.copy(crops = supportedCrops - setOf("potato", "lawn"))
            else -> product
        }
    } + SeasonalProgramProducts.products + SpringCarePrograms.products

    fun baseStepId(stepId: String): String = stepId.substringBefore("~").substringBefore(":remaining:")
    fun productForStep(stepId: String?): ProgramProduct? = stepId?.substringAfter("~", "")
        ?.substringBefore(":remaining:")?.takeIf(String::isNotBlank)?.let { id -> products.firstOrNull { it.id == id } }

    fun alternatives(programId: String?, stepId: String?): List<ProgramProduct> {
        if (!supports(programId) || stepId == null) return emptyList()
        val ids = when (baseStepId(stepId)) {
            SpringCarePrograms.EARLY_STEP -> SpringCarePrograms.products.map { it.id }.toSet()
            "nutrition", "nutrition_review" -> SeasonalProgramProducts.feedingIds(programId!!)
            "lawn_autumn_nutrition" -> setOf("organic_lawn_autumn", "bona_lawn_autumn")
            "gumi_omi_planting" -> if (programId == "tomato") setOf("organic_tomato", "maxi_nutrition") else setOf("maxi_nutrition")
            "fitosporin_spraying" -> setOf("silver_prevention")
            "gumi_omi_feeding" -> setOf("amino_tomato", "organic_tomato", "maxi_nutrition", "bona_vegetables")
            "gumi_omi_7_8_leaves" -> setOf("amino_cucumber", "organic_cucumber", "maxi_nutrition", "bona_vegetables")
            "kornesil_planting" -> setOf("amino_cucumber_planting", "rostobion_seedling")
            else -> emptySet()
        }
        return products.filter { it.id in ids && programId in it.crops }
    }

    fun problemsFor(programId: String): List<PlantProblem> = problems.filter { programId in it.crops }

    fun treatments(programId: String, problemId: String?, cultivationType: CultivationType): List<ProgramProduct> {
        if (!supports(programId)) return emptyList()
        if (programId !in setOf("tomato", "cucumber") && cultivationType != CultivationType.OPEN_GROUND) return emptyList()
        if (problemId != null && problemsFor(programId).none { it.id == problemId }) return emptyList()
        return products.filter {
            programId in it.crops && cultivationType in it.cultivationTypes && it.problems.isNotEmpty() &&
                (problemId == null || problemId in it.problems)
        }
    }
}

fun GeneratedCareProgram.withProduct(index: Int, productId: String, date: LocalDate, reminder: Int = 1): GeneratedCareProgram {
    val previous = steps[index]
    val product = ProgramProductCatalog.alternatives(templateId, previous.templateStepId)
        .first { it.id == productId && it.unavailableReason == null }
    require(!date.isBefore(chosenStartDate)) { "Дата работы не может быть раньше начала выбранной программы" }
    require(reminder in setOf(0, 1, 5))
    return copy(steps = steps.toMutableList().also {
        it[index] = previous.copy(
            templateStepId = "${ProgramProductCatalog.baseStepId(previous.templateStepId)}~${product.id}",
            title = product.taskTitle, scheduledDate = date, windowStart = date, windowEnd = date,
            recurrence = null, productDescription = product.displayName, note = product.note,
            reminderDaysBefore = reminder,
            weatherAdjusted = false, needsWeatherConfirmation = true,
            explanation = "Вы выбрали другую схему. Дату и условия применения проверьте по инструкции этого средства."
        )
    })
}

fun GeneratedCareProgram.withProblemTreatment(
    problemId: String?, productId: String, date: LocalDate, reminder: Int
): GeneratedCareProgram {
    require(!date.isBefore(chosenStartDate))
    require(reminder in setOf(0, 1, 5))
    val product = ProgramProductCatalog.treatments(templateId, problemId, cultivationType)
        .first { it.id == productId && it.unavailableReason == null }
    val problem = ProgramProductCatalog.problemsFor(templateId).firstOrNull { it.id == problemId }?.label
        ?: "Болезнь / вредитель не указаны пользователем"
    return copy(steps = steps + GeneratedCareStep(
        templateStepId = "problem:${problemId ?: "unspecified"}:${UUID.randomUUID()}~${product.id}",
        title = product.taskTitle, scheduledDate = date, windowStart = date, windowEnd = date,
        recurrence = null, weatherAdjusted = false, needsWeatherConfirmation = true,
        explanation = "Дополнительная обработка после осмотра. Приложение не ставит диагноз.",
        productDescription = product.displayName,
        note = "После осмотра: $problem. ${cultivationType.displayName}.\n${product.note}", reminderDaysBefore = reminder
    ))
}

fun PlantEntity.withProgramProduct(productId: String, date: LocalDate, reminder: Int): PlantEntity {
    val product = ProgramProductCatalog.alternatives(programId, programStepId)
        .first { it.id == productId && it.unavailableReason == null }
    require(reminder in setOf(0, 1, 5))
    return copy(
        taskName = product.taskTitle, drugId = null, drugName = product.displayName,
        creationDate = date.toString(), programStepId = "${ProgramProductCatalog.baseStepId(requireNotNull(programStepId))}~${product.id}",
        programNote = product.note, repeatType = RepeatType.NONE.name, repeatInterval = 1,
        wateringInterval = 1, repeatDaysOfWeek = "", repeatEndType = "NEVER", repeatEndDate = null,
        repeatCount = null, reminderDaysBefore = reminder, userLockedDate = true
    )
}

fun PlantEntity.problemTreatment(
    problemId: String?, productId: String, cultivationType: CultivationType,
    date: LocalDate, reminder: Int, requestId: String = UUID.randomUUID().toString()
): PlantEntity {
    val crop = requireNotNull(programId)
    val product = ProgramProductCatalog.treatments(crop, problemId, cultivationType)
        .first { it.id == productId && it.unavailableReason == null }
    require(reminder in setOf(0, 1, 5))
    val problem = ProgramProductCatalog.problemsFor(crop).firstOrNull { it.id == problemId }?.label
        ?: "Болезнь / вредитель не указаны пользователем"
    return copy(
        id = 0, plantCardId = resolvedCardId, taskName = product.taskTitle,
        creationDate = date.toString(), drugId = null, drugName = product.displayName,
        programStepId = "problem:${problemId ?: "unspecified"}~${product.id}",
        programImportKey = "$resolvedCardId:problem:$requestId",
        programNote = "После осмотра: $problem. ${cultivationType.displayName}.\n${product.note}",
        repeatType = RepeatType.NONE.name, repeatInterval = 1, wateringInterval = 1,
        repeatDaysOfWeek = "", repeatEndType = "NEVER", repeatEndDate = null, repeatCount = null,
        reminderDaysBefore = reminder, userLockedDate = true
    )
}
