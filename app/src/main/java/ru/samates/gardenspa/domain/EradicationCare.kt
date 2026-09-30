package ru.samates.gardenspa.domain

/** A user's care variant; label checks in [note] are not a universal crop authorization. */
data class EradicationVariant(
    val id: String,
    val title: String,
    val product: String,
    val note: String,
    val followupDays: Int? = null,
    val kind: String
)

data class EradicationComponentGroup(
    val id: String,
    val title: String,
    val options: List<EradicationVariant>
)

object EradicationCare {
    const val PROGRAM_VERSION = 20
    const val EARLY_STEP = "before_bud_break"
    const val AUTUMN_MIX_STEP = "autumn_before_leaf_fall_mix"
    const val AUTUMN_IRON_STEP = "after_leaf_fall"
    const val AUTUMN_RECIPE_ID = "autumn_eradication_tank_mix"
    const val IRON_RECIPE_ID = "iron_eradication_tank_mix"
    val cropIds = setOf("apple", "pear", "cherry", "plum", "currant", "raspberry", "blackberry", "blueberry")

    const val SPRING_CONDITIONS = "До распускания почек, при устойчивой температуре +3…+5 °C и выше. " +
        "Дата рассчитана по климату и служит ориентиром: фактическое состояние почек и требования этикетки важнее даты. " +
        "Выберите один вариант на сезон; не соединяйте растворы мочевины и меди в одном баке."
    const val ROTATION_NOTE = "По схеме пользователя чередуйте мочевину и медь по годам. При сохранении на следующий год заново выберите вариант с учётом предыдущей обработки."
    const val PROFILAKTIN_NOTE = "Отдельная обработка Профилактином через 7 дней, только пока почки не распустились и при температуре не ниже +4 °C. " +
        "Семь дней — интервал пользовательской схемы, не подтверждение совместимости. Классическая форма МКЭ, БИО и ЛАЙТ имеют разный состав: " +
        "выберите форму и норму по актуальной этикетке именно для своей культуры. Исторический регламент классического МКЭ — 500 мл/10 л; " +
        "он не подтверждает применение на малине, ежевике или голубике. Не добавлять Профилактин в предыдущий раствор. " +
        "Источник для классической формы: https://dacha.avgust.com/upload/Avgust_catalog_2022_1.pdf; " +
        "для БИО: https://dacha.avgust.com/catalog/profilaktin-bio/"
    private const val USER_NOTE = "Схема пользователя. Норму, культуру, фазу и ограничения конкретной фасовки сверяйте с её этикеткой."
    private const val UREA_NOTE = "Запрошенная концентрация относится к искореняющей схеме по спящим растениям, а не к подкормке по листьям. " +
        "Назначение схемы пользователя — обработка против грибковых и бактериальных заболеваний. Не переносите её на зелёные ткани. " +
        "Универсальное уничтожение вредителей и лечение бактериальных заболеваний этим рецептом не подтверждены."

    val springVariants = listOf(
        EradicationVariant("urea_young", "1. Мочевина — молодые растения", "Мочевина — 300 г на 10 л воды",
            "$USER_NOTE Молодые растения, небольшое количество вредителей по наблюдениям пользователя. " +
                "Растворить 300 г мочевины в воде, довести объём до 10 л. $UREA_NOTE\n$SPRING_CONDITIONS\n$PROFILAKTIN_NOTE\n$ROTATION_NOTE",
            followupDays = 7, kind = "urea"),
        EradicationVariant("urea_mature", "2. Мочевина — взрослые растения", "Мочевина — 500–700 г на 10 л воды",
            "$USER_NOTE Взрослые растения при сильном заражении по наблюдениям пользователя. " +
                "В исходной схеме указано 500–700 г мочевины на 10 л; выберите концентрацию по условиям растения и инструкции применяемого средства. " +
                "$UREA_NOTE\n$SPRING_CONDITIONS\n$ROTATION_NOTE",
            kind = "urea"),
        EradicationVariant("copper", "3. Медный купорос или бордоская жидкость", "Медный купорос / бордоская жидкость — один вариант",
            "$USER_NOTE Назначение схемы пользователя — обработка против грибковых и бактериальных заболеваний. " +
                "Выберите одно медьсодержащее средство по возрасту и состоянию растения. " +
                "Жидкую бордоскую форму не приравнивать к сухой смеси; фазу выбранной этикетки соблюдайте и при изменении календарной даты. " +
                "$SPRING_CONDITIONS\n$PROFILAKTIN_NOTE\n$ROTATION_NOTE",
            followupDays = 7, kind = "copper")
    )

    val copperComponents = listOf(EradicationComponentGroup("copper_source", "Выберите медьсодержащее средство", listOf(
        EradicationVariant("copper_mature", "Медный купорос — взрослые растения", "Медный купорос — 300 г на 10 л воды",
            "Рецепт пользователя для взрослых растений: 300 г медного купороса, водой довести до 10 л. " +
                "Применять только до распускания почек по этикетке садового препарата. Средство для деревянных конструкций не заменяет садовый препарат.",
            kind = "copper"),
        EradicationVariant("copper_young", "Медный купорос — молодые или неповреждённые растения", "Медный купорос — 100 г на 10 л воды",
            "Рецепт пользователя для молодых или неповреждённых растений: 100 г медного купороса, водой довести до 10 л. " +
                "Применять только до распускания почек по этикетке садового препарата. Средство для деревянных конструкций не заменяет садовый препарат.",
            kind = "copper"),
        EradicationVariant("bordeaux_liquid", "Бордоская жидкость", "Бордоская жидкость — 250 мл на 10 л воды",
            "Вариант пользовательского рецепта: бордоская жидкость 250 мл/10 л. У Август, ВСК эта норма указана для зелёного конуса на плодовых, " +
                "чёрной смородине и крыжовнике: применяйте в фазе своей этикетки, а не автоматически по спящим почкам. " +
                "Нормы малины, ежевики и голубики этим регламентом не подтверждены. Источник: https://dacha.avgust.com/catalog/bordoskaya-zhidkost/",
            kind = "copper")
    )))

    val autumnComponents = listOf(
        EradicationComponentGroup("insecticide", "От вредителей — выберите одно средство", listOf(
            EradicationVariant("actara", "Актара", "Актара, ВДГ — норма по этикетке культуры",
                "В исходном рецепте пользователя указано 4 г на 10 л; эта доза не подтверждена для всех плодовых и ягодных культур. " +
                    "Официальный регламент Syngenta для смородины в ЛПХ против тли — 2 г/10 л, 0,5–1 л/куст до цветения или после сбора урожая. " +
                    "Для другой культуры сначала проверьте её наличие и дозу на этикетке. " +
                    "Источник: https://www.syngenta.ru/sites/g/files/kgtney371/files/media/document/2024/11/11/catalog_2025.pdf",
                kind = "insecticide"),
            EradicationVariant("coragen", "Кораген", "Кораген, КС — доза по этикетке выбранной культуры",
                "По уточнению пользователя дозировку выбирают по этикетке для своей культуры. Кораген — жидкий концентрат суспензии, " +
                    "хлорантранилипрол 200 г/л; не отмеряйте его как 4 г порошка. Нормы разных культур не взаимозаменяемы. " +
                    "В опубликованном ЛПХ-регламенте FMC для яблони против плодожорки и листовёрток указано 0,2 мл/10 л; " +
                    "подтвердите значение по актуальной упаковке. Норма 4 мл/10 л относится к томату против совки, а не ко всему саду. " +
                    "Источник: https://fmcrussia.ru/products/insecticides/koragen",
                kind = "insecticide")
        )),
        EradicationComponentGroup("fungicide", "От болезней — выберите одно средство", listOf(
            EradicationVariant("skor", "Скор", "Скор, КЭ — 2 мл на 10 л по регламенту культуры",
                "Вариант пользовательского рецепта: 2 мл/10 л. Подтверждённые цели этой нормы: яблоня и груша — парша/мучнистая роса, " +
                    "слива — клястероспориоз, вишня — коккомикоз. Не переносите норму на все болезни и ягодные кустарники. " +
                    "Источник: https://dacha.avgust.com/catalog/skor/", kind = "fungicide"),
            EradicationVariant("raek", "Раёк", "Раёк, КЭ — 2 мл на 10 л по регламенту культуры",
                "Вариант пользовательского рецепта: 2 мл/10 л. Российская строка производителя для яблони и груши от парши и мучнистой росы — " +
                    "1,5–2 мл/10 л; косточковые на странице отдельно помечены регистрацией в Беларуси. Применимость к другой культуре подтвердите по этикетке. " +
                    "Источник: https://dacha.avgust.com/catalog/rayek/", kind = "fungicide"),
            EradicationVariant("topaz", "Топаз", "Топаз, КЭ — 2 мл на 10 л по регламенту культуры",
                "Вариант пользовательского рецепта: 2 мл/10 л. Производитель подтверждает эту норму для смородины против американской мучнистой росы " +
                    "при первых признаках. Это не универсальная замена Скору или Раёку для всех плодовых болезней. " +
                    "Источник: https://dacha.avgust.com/catalog/topaz/", kind = "fungicide")
        ))
    )

    const val AUTUMN_CONDITIONS = "После полного сбора урожая, до листопада, при температуре не ниже +15 °C. " +
        "Ориентир исходной схемы — 10 сентября; в программе дата сдвигается по климату сада. Если урожай ещё не собран или условия не подходят, перенесите работу."
    const val AUTUMN_FIXED_INGREDIENTS = "На 10 л воды: Эпин-Экстра — 1 мл; прилипатель — 10 мл."
    const val AUTUMN_ADJUVANT_NOTICE = "Название прилипателя не указано: 10 мл не является универсальной нормой, сверяйте инструкцию выбранного средства."
    const val AUTUMN_ADDITIONS = "К выбранным компонентам исходной схемы: Эпин-Экстра — 1 мл; прилипатель — 10 мл; водой довести до 10 л. " +
        "Осенняя норма Эпина-Экстра 1 мл/10 л подтверждена НЭСТ-М: https://nest-m.ru/epin-extra/. " +
        AUTUMN_ADJUVANT_NOTICE
    const val MIX_COMPATIBILITY = "Совместимость полного состава отдельно не подтверждена. Выбирайте по одному инсектициду и фунгициду, " +
        "проверяйте разрешённую культуру, форму препарата, норму и совместимость всех компонентов. Не смешивайте одновременно оба инсектицида или все три фунгицида."
    const val IRON_NOTE = "Осенняя схема пользователя против грибковых болезней, мхов и лишайников. " +
        "После полного листопада: 300 г железного купороса растворить в 1 л горячей воды, " +
        "процедить, добавить воду до общего объёма 10 л и дать раствору остыть. Ориентир — 25 октября; дата сдвигается по климату сада. " +
        "Проводить утром, чтобы раствор высох до вечернего похолодания. Возможный первый снег не отменяет требования этикетки: " +
        "не опрыскивать замёрзшие растения, по снегу или при ожидаемом замерзании раствора. После листопада, отдельно от других средств. " +
        "Не применять эту концентрацию по листьям или хвое. Это осенняя обработка, а не подтверждённая схема лечения хлороза. " +
        "Рецепт 300 г/10 л предоставлен пользователем; у конкретного производителя состав и концентрация могут отличаться."

    val autumnVariant = EradicationVariant("autumn_main", "Осенняя основная обработка", "Баковая смесь «Осенняя основная»",
        "$USER_NOTE $AUTUMN_CONDITIONS\n$AUTUMN_ADDITIONS\n$MIX_COMPATIBILITY\n${FolkFertilizers.POLLINATION_NOTICE}", kind = "autumn_mix")

    fun componentsFor(variantId: String): List<EradicationComponentGroup> = when (variantId) {
        "copper" -> copperComponents
        "autumn_main", "autumn_mix" -> autumnComponents
        else -> emptyList()
    }

    val autumnRecipe: FolkFertilizerRecipe get() = FolkFertilizerRecipe(
        AUTUMN_RECIPE_ID, "Баковая смесь «Осенняя основная»", AUTUMN_CONDITIONS,
        "На 10 л воды: одно средство от вредителей — Актара (в исходной записи 4 г, фактическая норма по этикетке культуры) " +
            "ИЛИ Кораген (доза по этикетке выбранной культуры); одно средство от болезней — Скор 2 мл ИЛИ Раёк 2 мл ИЛИ Топаз 2 мл; " +
            "Эпин-Экстра 1 мл; прилипатель 10 мл только при соответствии его инструкции.",
        "Компоненты разводить в порядке и условиях их этикеток. Готовить раствор непосредственно перед использованием. " +
            "Выбор конкретных препаратов зависит от культуры и цели обработки.",
        "Общий объём смеси — 10 л. Норма рабочего раствора на растение, кратность и сроки ожидания — по этикеткам выбранных средств.",
        MIX_COMPATIBILITY,
        "Схема пользователя от 29.09.2026; Кораген — по этикетке культуры", "", isTankMix = true
    )

    val ironRecipe: FolkFertilizerRecipe get() = FolkFertilizerRecipe(
        IRON_RECIPE_ID, "Баковая смесь «Искореняющая»",
        "Осенняя схема пользователя против грибковых болезней, мхов и лишайников после полного листопада.",
        "На 10 л воды: железный купорос — 300 г.",
        IRON_NOTE,
        "Общий объём раствора — 10 л. Расход на растение — по этикетке выбранного средства.",
        "", "Рецепт пользователя, подтверждён 30.09.2026", "", isTankMix = true
    )

    /** Reference-only recipe records. They do not claim a verified generic pest/disease prescription. */
    val referenceProducts: List<ProgramProduct> get() =
        (springVariants.filter { it.kind == "urea" } + copperComponents.flatMap { it.options } + autumnComponents.flatMap { it.options } +
            EradicationVariant("autumn_iron", "Железный купорос после листопада", "Железный купорос — 300 г на 10 л воды", IRON_NOTE, kind = "iron"))
            .map { variant -> ProgramProduct("eradication_${variant.id}", variant.product, "Рецепт пользователя",
                "Применить выбранную схему", variant.title, variant.note, "", crops = cropIds,
                cultivationTypes = setOf(CultivationType.OPEN_GROUND)) }

    fun applies(programId: String?): Boolean = programId in cropIds
    fun isVariantStep(stepId: String?): Boolean = stepId?.substringBefore("~") in setOf(EARLY_STEP, AUTUMN_MIX_STEP)
    fun variantsFor(stepId: String?): List<EradicationVariant> = when (stepId?.substringBefore("~")) {
        EARLY_STEP -> springVariants
        AUTUMN_MIX_STEP -> listOf(autumnVariant)
        else -> emptyList()
    }

    /** Climate offsets use the existing moderate fixture (safe autumn: October 10): September 10 / October 25. */
    fun apply(template: PlantCareTemplate): PlantCareTemplate {
        if (!applies(template.id)) return template
        val replacedIds = setOf(EARLY_STEP, SpringCarePrograms.PEST_STEP, SpringCarePrograms.AUTUMN_STEP, AUTUMN_MIX_STEP, AUTUMN_IRON_STEP)
        val retained = template.steps.filterNot { it.id.substringBefore("~") in replacedIds }.map { step ->
            if (step.id.substringBefore("~") == SpringCarePrograms.GREEN_STEP) {
                step.copy(anchor = CareAnchor.DORMANT_SPRING_DATE, offsetDays = 14)
            } else step
        }
        return template.copy(version = maxOf(template.version + 1, PROGRAM_VERSION), steps = listOf(
            CareStepTemplate(EARLY_STEP, "Весенняя искореняющая обработка — выбрать вариант",
                CareAnchor.DORMANT_SPRING_DATE, 0, productDescription = "Выберите один из трёх вариантов",
                note = "$SPRING_CONDITIONS\n$ROTATION_NOTE\nВыбор определяет состав и необходимость отдельного Профилактина через 7 дней.")
        ) + retained + listOf(
            CareStepTemplate(AUTUMN_MIX_STEP, "Осенняя основная обработка — выбрать смесь",
                CareAnchor.SAFE_AUTUMN_DATE, -30, productDescription = autumnRecipe.name,
                note = "$AUTUMN_CONDITIONS\n${autumnRecipe.ingredients}\n$AUTUMN_ADDITIONS\n$MIX_COMPATIBILITY"),
            CareStepTemplate(AUTUMN_IRON_STEP, "После листопада: обработать железным купоросом",
                CareAnchor.SAFE_AUTUMN_DATE, 15, productDescription = "Железный купорос — 300 г на 10 л воды",
                note = IRON_NOTE)
        ))
    }
}
