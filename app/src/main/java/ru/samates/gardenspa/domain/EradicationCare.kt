package ru.samates.gardenspa.domain

import java.time.LocalDate

data class EradicationVariant(
    val id: String,
    val title: String,
    val product: String,
    val note: String,
    val followupDays: Int? = null,
    val kind: String,
    val unavailableReason: String? = null
)

data class EradicationComponentGroup(val id: String, val title: String, val options: List<EradicationVariant>)

object EradicationCare {
    const val FIRST_VARIANT_VERSION = 20
    const val PROGRAM_VERSION = 22
    const val EARLY_STEP = "before_bud_break"
    const val PEST_STEP = "before_bud_break_followup"
    const val AUTUMN_MIX_STEP = "autumn_before_leaf_fall_mix"
    const val AUTUMN_IRON_STEP = "after_leaf_fall"
    const val WHITEWASH_STEP = "autumn_whitewash"
    const val AUTUMN_RECIPE_ID = "autumn_eradication_tank_mix"
    const val IRON_RECIPE_ID = "iron_eradication_tank_mix"
    val fruitTreeIds = setOf("apple", "pear", "cherry", "plum")
    val cropIds = fruitTreeIds + setOf("currant", "raspberry", "blackberry", "blueberry")

    const val SPRING_CONDITIONS = "До распускания почек, при устойчивой температуре +3…+5 °C и выше. " +
        "Не применять по зелёным листьям. Выберите один вариант; не соединяйте растворы карбамида и меди в одном баке."
    const val ROTATION_NOTE = "Чередуйте карбамид и медь по годам. В следующем сезоне выберите вариант с учётом предыдущей обработки."
    const val PROFILAKTIN_NOTE = "Профилактин БИО, ВЭ — от зимующих вредителей, 500 мл на 10 л воды. " +
        "Плодовые деревья: 1–5 л на дерево; смородина: 0,5–1,5 л на куст.\n\n" +
        "Применять до распускания почек при температуре от +4 °C, отдельно от предыдущей обработки. Одна обработка за сезон."
    const val PEST_NOTE = "Через 7 дней после весенней искореняющей обработки от болезней. Выберите одно средство от зимующих вредителей.\n\n" +
        "Масляные препараты применяют до распускания почек при температуре от +4 °C. " +
        "Средства для периода вегетации не заменяют обработку по спящим почкам. Не добавлять выбранное средство в предыдущий раствор."

    val springVariants = listOf(
        EradicationVariant("urea_young", "Карбамид — молодые растения, грибковые болезни", "Карбамид — 300 г на 10 л воды",
            "Для молодых растений против грибковых болезней: растворить 300 г карбамида в воде и довести объём до 10 л.\n\n" +
                "$SPRING_CONDITIONS\n\n$ROTATION_NOTE", followupDays = 7, kind = "urea"),
        EradicationVariant("urea_mature", "Карбамид — взрослые растения, грибковые болезни, вредители", "Карбамид — 500–700 г на 10 л воды",
            "Для взрослых растений при сильной заселённости и поражении грибковыми болезнями: 500–700 г карбамида на 10 л воды.\n\n" +
                "Концентрация относится к обработке спящих растений. От зимующих вредителей предусмотрена отдельная работа через 7 дней.\n\n" +
                "$SPRING_CONDITIONS\n\n$ROTATION_NOTE", followupDays = 7, kind = "urea"),
        EradicationVariant("copper", "Медный купорос или бордоская жидкость", "Медный купорос / бордоская жидкость — один вариант",
            "Против грибковых болезней. Выберите одно медьсодержащее средство по возрасту и состоянию растения. " +
                "Бордоская жидкость — отдельный вариант состава.\n\n$SPRING_CONDITIONS\n\n$ROTATION_NOTE",
            followupDays = 7, kind = "copper")
    )

    val copperComponents = listOf(EradicationComponentGroup("copper_source", "Выберите медьсодержащее средство", listOf(
        EradicationVariant("copper_mature", "Медный купорос — взрослые растения", "Медный купорос — 300 г на 10 л воды",
            "Для взрослых растений против грибковых болезней: растворить 300 г медного купороса и довести водой до 10 л.\n\n" +
                "Применять только до распускания почек. Не наносить на зелёные листья.", kind = "copper"),
        EradicationVariant("copper_young", "Медный купорос — молодые или неповреждённые растения", "Медный купорос — 100 г на 10 л воды",
            "Для молодых или неповреждённых растений против грибковых болезней: растворить 100 г медного купороса и довести водой до 10 л.\n\n" +
                "Применять только до распускания почек. Не наносить на зелёные листья.", kind = "copper"),
        EradicationVariant("bordeaux_liquid", "Бордоская жидкость", "Бордоская жидкость — 250 мл на 10 л воды",
            "Для плодовых деревьев и чёрной смородины против болезней: бордоская жидкость Август, ВСК — 250 мл на 10 л воды.\n\n" +
                "Эта норма предназначена для фазы зелёного конуса. Жидкий концентрат не заменяют равным количеством сухой смеси. " +
                "Применение на малине, ежевике и голубике этой схемой не предусмотрено.", kind = "copper")
    )))

    val autumnComponents = listOf(
        EradicationComponentGroup("insecticide", "От вредителей — выберите одно средство", listOf(
            EradicationVariant("actara", "Актара", "Актара, ВДГ — 4 г на 10 л в составе рецепта",
                "В рецепте осенней смеси — 4 г на 10 л. Для смородины против тли зарегистрированная норма отдельного применения — " +
                    "2 г на 10 л, 0,5–1 л на куст до цветения или после сбора урожая. Норма рецепта не является общей для всех культур.", kind = "insecticide"),
            EradicationVariant("coragen", "Кораген", "Кораген, КС — норма зависит от культуры",
                "Для яблони против плодожорки и листовёрток — 0,2 мл на 10 л. Кораген — жидкий концентрат суспензии, хлорантранилипрол 200 г/л. " +
                    "Норма 4 мл на 10 л предназначена для томата против совки; её не переносят на плодовые деревья.", kind = "insecticide")
        )),
        EradicationComponentGroup("fungicide", "От болезней — выберите одно средство", listOf(
            EradicationVariant("horus", "Хорус", "Хорус, ВДГ — 2 г на 10 л воды",
                "Для яблони и груши: Хорус, водно-диспергируемые гранулы — 2 г на 10 л. Норма для косточковых отличается: 1 г на 3 л. " +
                    "Не переносить эту дозу на ягодные кустарники.\n\nВысокая эффективность в прохладе: +5…+10 °C; действует от +3 °C.", kind = "fungicide"),
            EradicationVariant("skor", "Скор", "Скор, КЭ — 2 мл на 10 л воды",
                "Яблоня и груша — парша и мучнистая роса; слива — клястероспориоз; вишня — коккомикоз: 2 мл на 10 л. " +
                    "Не переносить эту норму на все болезни и ягодные кустарники.\n\n" +
                    "Температура максимальной эффективности: числовой диапазон в опубликованных рекомендациях производителя не указан.", kind = "fungicide"),
            EradicationVariant("raek", "Раёк", "Раёк, КЭ — 1,5–2 мл на 10 л воды",
                "Для яблони и груши от парши и мучнистой росы: 1,5–2 мл на 10 л. Применять в период вегетации.\n\n" +
                    "Температура: активное действие при среднесуточной температуре выше +14…+16 °C. Отдельный диапазон максимальной эффективности не указан.", kind = "fungicide"),
            EradicationVariant("topaz", "Топаз", "Топаз, КЭ — 2 мл на 10 л воды",
                "Для смородины против американской мучнистой росы при первых признаках: 2 мл на 10 л.\n\n" +
                    "Температура: эффективен при высоких температурах. Числовой диапазон максимальной эффективности производитель не указывает.", kind = "fungicide")
        ))
    )

    const val AUTUMN_CONDITIONS = "Проводится после сбора плодов, до листопада\n\nТемпература воздуха — +5…+10"
    const val AUTUMN_FIXED_INGREDIENTS = "На 10 л воды: Эпин — 1 мл; прилипатель — 10 мл."
    const val AUTUMN_ADJUVANT_NOTICE = ""
    const val AUTUMN_ADDITIONS = AUTUMN_FIXED_INGREDIENTS
    const val MIX_COMPATIBILITY = "Выберите по одному средству от вредителей и болезней."
    const val AUTUMN_INGREDIENTS = "Баковая смесь «Осенняя основная»\nНа 10 литров воды:\nАктара 4 г\nХорус 2 г\nЭпин 1 мл\nПрилипатель 10 мл"
    const val IRON_PURPOSE = "Против грибковых болезней, мхов, лишайников\nПрофилактика хлороза (раннего пожелтения листьев)"
    const val IRON_NOTE = "Осенняя искореняющая обработка\n\n" +
        "Срок: Строго после окончания листопада, с первыми \"белыми мухами\", температура воздуха 0...+5\n\n" +
        "Время: Утро\n\n" +
        "Важно: Избегать вечерней обработки — из-за риска замерзания раствора на деревьях.\n\n" +
        "Не применять на хвойных деревьях и кустарниках\n\n" +
        "Препарат \"Железный купорос\"\n\n" +
        "Приготовление: 300 грамм железного купороса развести в 1 литре горячей воды, процедить, довести объем до 10 литров обычной водой.\n\n" +
        "Назначение: грибковые заболевания, мхи, лишайники, хлороз (дефицит железа, признак - пожелтение листьев летом).\n\n" +
        "Опрыскать сад."
    const val WHITEWASH_NOTE = "Назначение: защита от болезней, вредителей, грызунов, солнечных ожогов.\n\n" +
        "Через 14 дней после последней осенней обработки. Выберите один состав: Доктор Сад — БашИнком или известь с медным купоросом.\n\n" +
        "Известь + медный купорос: 2,5 кг гашёной извести и 300–500 г медного купороса на 10 л воды для усиленной дезинфекции."

    val autumnVariant = EradicationVariant("autumn_main", autumnRecipe.name, autumnRecipe.name,
        "$AUTUMN_CONDITIONS\n\n$AUTUMN_ADDITIONS", kind = "autumn_mix")

    fun componentsFor(variantId: String): List<EradicationComponentGroup> = when (variantId) {
        "copper" -> copperComponents
        "autumn_main", "autumn_mix" -> autumnComponents
        else -> emptyList()
    }

    val autumnRecipe: FolkFertilizerRecipe get() = FolkFertilizerRecipe(
        AUTUMN_RECIPE_ID, "Баковая смесь «Осенняя основная»", AUTUMN_CONDITIONS, AUTUMN_INGREDIENTS,
        "", "", "", "", "", isTankMix = true
    )

    val ironRecipe: FolkFertilizerRecipe get() = FolkFertilizerRecipe(
        IRON_RECIPE_ID, "Баковая смесь «Осенняя искореняющая»", IRON_PURPOSE,
        "", IRON_NOTE, "", "", "", "", isTankMix = true
    )

    val pestProducts = listOf(
        ProgramProduct("eradication_pest_profilaktin", "Профилактин БИО, ВЭ", "Август",
            "Весенняя искореняющая обработка от зимующих вредителей", "От зимующих вредителей.", PROFILAKTIN_NOTE,
            "https://dacha.avgust.com/catalog/profilaktin-bio/", crops = fruitTreeIds + "currant"),
        ProgramProduct("eradication_pest_30_plus", "Препарат 30 Плюс", "НПФ Собер",
            "Весенняя искореняющая обработка от зимующих вредителей", "От зимующих вредителей.",
            "500 мл на 10 л воды. Применять ранней весной до распускания почек, при температуре выше +4 °C. Наносить отдельно от других средств.",
            "https://grepharm.ru/products/ot-vrediteley/preparat-30-plyus/", crops = fruitTreeIds + setOf("currant", "raspberry")),
        ProgramProduct("eradication_pest_agrozelin", "Агрозелин", "Green Belt",
            "Весенняя искореняющая обработка от зимующих вредителей", "Вазелиновое масло, водная эмульсия, против зимующих вредителей.",
            "250 мл на 5 л воды (500 мл на 10 л). До распускания почек при температуре от +4 °C, одна обработка. Не смешивать с другими средствами защиты растений.",
            "https://greenbelt.ru/products/agrozelin/", crops = fruitTreeIds + setOf("currant", "raspberry")),
        ProgramProduct("eradication_pest_fufanon", "Фуфанон-Нова", "Кеминова",
            "Обработать при появлении вредителей", "Малатион 440 г/л, водная эмульсия.",
            "6,5 мл на 5 л воды. Применяют при появлении вредителей; это не масляная обработка по спящим почкам.",
            "https://grepharm.ru/products/ot-vrediteley/fufanon-nova/", crops = cropIds,
            unavailableReason = "Применение при появлении вредителей; для этого этапа по спящим почкам не подтверждено."),
        ProgramProduct("eradication_pest_alatar", "Алатар, КЭ", "Ваше хозяйство",
            "Обработать при появлении вредителей", "Малатион 22,5% и циперметрин 5%.",
            "По вегетирующим растениям при +12…+25 °C: яблоня — 5 мл на 10 л, смородина — 5 мл на 5 л.",
            "https://www.vhoz.ru/catalog/tovary_dlya_sada_i_ogoroda/sredstva_zashchity_rasteniy_ot_vrediteley_/alatar-universalnyy-preparat-dlya-zashchity-ot-vrediteley/",
            crops = cropIds, unavailableReason = "Применяется по вегетирующим растениям при +12…+25 °C, не по спящим почкам."),
        ProgramProduct("eradication_pest_karbofos", "Карбофос", "Green Belt",
            "Бытовое применение", "Малатион 100 г/кг, порошок.", "У этой формы подтверждено бытовое применение от домовых муравьёв.",
            "https://greenbelt.ru/products/karbofos/", crops = cropIds,
            unavailableReason = "У этой формы подтверждено бытовое применение, садовая обработка не подтверждена."),
        ProgramProduct("eradication_pest_aliot", "Алиот, КЭ", "Август",
            "Обработать при появлении вредителей", "Малатион 570 г/л.",
            "Яблоня — 10 мл на 10 л по вегетации. Смородина против почкового клеща — в бутонизацию; другие вредители — по вегетации.",
            "https://www.avgust.com/products/rf/aliot/", crops = cropIds,
            unavailableReason = "Применение по вегетации или в бутонизацию; для этапа по спящим почкам не подтверждено.")
    )

    fun pestAlternatives(programId: String): List<ProgramProduct> = pestProducts.map { product ->
        if (programId in product.crops) product else product.copy(unavailableReason = "Применение для выбранной культуры не подтверждено.")
    }

    val whitewashProducts = listOf(
        ProgramProduct("eradication_whitewash_doctor", "Доктор Сад — лечебная садовая побелка", "БашИнком",
            "Побелка деревьев", "Защита от болезней, вредителей, грызунов, солнечных ожогов.",
            "1 кг пасты на 1,5–2 л тёплой воды. Перед побелкой очистить отмершую кору. Нанести на ствол и основания скелетных ветвей.",
            "https://www.bashinkom.ru/products/ojz/DoktorSadLechebnayapobelkasadovaya1kg", crops = fruitTreeIds),
        ProgramProduct("eradication_whitewash_lime_copper", "Известь + медный купорос", "Рецепт пользователя",
            "Побелка деревьев", "Защита от болезней, вредителей, грызунов, солнечных ожогов.",
            "2,5 кг гашёной извести и 300–500 г медного купороса на 10 л воды для усиленной дезинфекции.", "", crops = fruitTreeIds)
    )

    val referenceProducts: List<ProgramProduct> get() =
        (springVariants.filter { it.kind == "urea" } + copperComponents.flatMap { it.options } + autumnComponents.flatMap { it.options } +
            EradicationVariant("autumn_iron", "Железный купорос после листопада", "Железный купорос — 300 г на 10 л воды", IRON_NOTE, kind = "iron"))
            .map { variant -> ProgramProduct("eradication_${variant.id}", variant.product, "Рецепт пользователя",
                "Применить выбранную схему", variant.title, variant.note, "", crops = cropIds,
                cultivationTypes = setOf(CultivationType.OPEN_GROUND), unavailableReason = variant.unavailableReason) } + pestProducts + whitewashProducts

    fun applies(programId: String?): Boolean = programId in cropIds
    fun isVariantStep(stepId: String?): Boolean = stepId?.substringBefore("~") in setOf(EARLY_STEP, AUTUMN_MIX_STEP)
    fun variantsFor(stepId: String?): List<EradicationVariant> = when (stepId?.substringBefore("~")) {
        EARLY_STEP -> springVariants
        AUTUMN_MIX_STEP -> listOf(autumnVariant)
        else -> emptyList()
    }

    fun autumnDate(context: CareProgramContext): LocalDate {
        val location = context.location
        // Coordinates give a coarse regional fallback, not a prediction of actual leaf fall.
        val month = when {
            location != null && (location.longitude >= 65.0 || location.latitude >= 60.0) -> 9
            context.isSouthernRegion || location != null && location.latitude < 50.0 -> 11
            location != null -> 10
            context.climate.safeAutumnDay.monthValue <= 9 -> 9
            context.climate.safeAutumnDay.monthValue >= 11 -> 11
            else -> 10
        }
        return LocalDate.of(context.startDate.year, month, 25)
    }

    fun apply(template: PlantCareTemplate): PlantCareTemplate {
        if (!applies(template.id)) return template
        val replacedIds = setOf(EARLY_STEP, PEST_STEP, WHITEWASH_STEP, SpringCarePrograms.PEST_STEP,
            SpringCarePrograms.AUTUMN_STEP, AUTUMN_MIX_STEP, AUTUMN_IRON_STEP)
        val retained = template.steps.filterNot { it.id.substringBefore("~") in replacedIds }.map { step ->
            if (step.id.substringBefore("~") == SpringCarePrograms.GREEN_STEP) step.copy(anchor = CareAnchor.DORMANT_SPRING_DATE, offsetDays = 14) else step
        }
        val pest = pestAlternatives(template.id).firstOrNull { it.unavailableReason == null }
        return template.copy(version = maxOf(template.version + 1, PROGRAM_VERSION), steps = listOf(
            CareStepTemplate(EARLY_STEP, "Весенняя искореняющая обработка от болезней",
                CareAnchor.DORMANT_SPRING_DATE, 0, productDescription = "Выберите один из трёх вариантов", note = "$SPRING_CONDITIONS\n\n$ROTATION_NOTE"),
            CareStepTemplate(PEST_STEP + (pest?.let { "~${it.id}" } ?: ""), "Весенняя искореняющая обработка от зимующих вредителей",
                CareAnchor.DORMANT_SPRING_DATE, 7, productDescription = pest?.displayName ?: "Выберите средство",
                note = listOf(PEST_NOTE, pest?.instruction).filterNotNull().joinToString("\n\n"))
        ) + retained + listOf(
            CareStepTemplate(AUTUMN_MIX_STEP, autumnRecipe.name,
                CareAnchor.SAFE_AUTUMN_DATE, -30, productDescription = autumnRecipe.name, note = "$AUTUMN_CONDITIONS\n\n$AUTUMN_INGREDIENTS"),
            CareStepTemplate(AUTUMN_IRON_STEP, ironRecipe.name,
                CareAnchor.AUTUMN_ERADICATION_DATE, 0, productDescription = ironRecipe.name, note = IRON_NOTE)
        ) + if (template.id in fruitTreeIds) listOf(
            CareStepTemplate("$WHITEWASH_STEP~eradication_whitewash_doctor", "Побелка деревьев",
                CareAnchor.AUTUMN_ERADICATION_DATE, 14, productDescription = whitewashProducts.first().displayName, note = WHITEWASH_NOTE)
        ) else emptyList())
    }
}
