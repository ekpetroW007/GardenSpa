package ru.samates.gardenspa.domain

fun ClimateFingerprint.naturalZone(): NaturalZone = when {
    frostFreeDays < 100 || growingDegreeDays5 < 1_100 -> NaturalZone.TUNDRA_AND_FOREST_TUNDRA
    frostFreeDays < 140 || winterMinimumP10 <= -30.0 -> NaturalZone.TAIGA
    frostFreeDays >= 220 && winterMinimumP10 > -8.0 && warmSeasonPrecipitationMm >= 480.0 -> NaturalZone.HUMID_SUBTROPICS
    frostFreeDays < 165 && warmSeasonPrecipitationMm >= 340.0 -> NaturalZone.MIXED_AND_BROADLEAF_FORESTS
    frostFreeDays < 190 && warmSeasonPrecipitationMm >= 270.0 -> NaturalZone.FOREST_STEPPE
    else -> NaturalZone.STEPPE_AND_SEMI_DESERT
}

fun popularAnnualCareTemplates(): List<PlantCareTemplate> = listOf(
    popularPlantTemplate(
        id = "tomato",
        name = "Томат",
        aliases = setOf("томат", "томаты", "помидор", "помидоры"),
        supportedCultivationTypes = CultivationType.entries.toSet(),
        supportedNaturalZones = NaturalZone.entries.toSet(),
        openGroundStartOffsetDays = 14,
        greenhouseStartOffsetDays = -21,
        pruningTitle = "Формирующая обрезка и пасынкование томата",
        pruningNote = "Удаляйте небольшие пасынки чистым инструментом по сухой листве; не обрезайте мокрые растения и сразу подвяжите сохраняемые побеги.",
        summerCareTitle = "Повторить пасынкование и проверить подвязку",
        summerCareNote = "Не удаляйте за один раз слишком много листьев; обеспечьте проветривание и не оставляйте срезанные части рядом с растением.",
        waterIntervalDays = 4,
        diseaseLabel = "томатов",
        autumnRootTitle = "Корневая калийная подкормка перед завершением плодоношения",
        winterTitle = "Завершить уход и убрать больные растительные остатки"
    ),
    popularPlantTemplate(
        id = "cucumber",
        name = "Огурец",
        aliases = setOf("огурец", "огурцы"),
        supportedCultivationTypes = CultivationType.entries.toSet(),
        supportedNaturalZones = NaturalZone.entries.toSet(),
        openGroundStartOffsetDays = 18,
        greenhouseStartOffsetDays = -18,
        pruningTitle = "Формирующая обрезка огурца",
        pruningNote = "Удалите повреждённые нижние листья и лишние боковые побеги по схеме выбранного сорта; режьте только сухую ткань чистым инструментом.",
        summerCareTitle = "Направить плети и повторить формировку",
        summerCareNote = "Не перегибайте плети и не удаляйте здоровые листья без необходимости; поддерживайте проветривание шпалеры.",
        waterIntervalDays = 3,
        diseaseLabel = "огурцов",
        autumnRootTitle = "Корневая калийная подкормка в конце плодоношения",
        winterTitle = "Завершить уход и удалить поражённые плети"
    ),
    popularPlantTemplate(
        id = "hydrangea",
        name = "Гортензия",
        aliases = setOf("гортензия", "гортензии"),
        supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
        supportedNaturalZones = NaturalZone.entries.toSet() - NaturalZone.TUNDRA_AND_FOREST_TUNDRA,
        openGroundStartOffsetDays = -7,
        pruningTitle = "Весенняя обрезка гортензии",
        pruningNote = "Санитарно удалите мёртвую древесину. Метельчатую и древовидную гортензию обрезают по прошлогоднему приросту весной; крупнолистную сильно не укорачивают, чтобы сохранить цветочные почки.",
        summerCareTitle = "Обрезка после цветения для видов на прошлогодних побегах",
        summerCareNote = "У крупнолистной и плетистой гортензии удаляйте отцветшие соцветия и чрезмерно длинные побеги после цветения; для метельчатой ограничьтесь санитарной коррекцией.",
        waterIntervalDays = 4,
        diseaseLabel = "гортензии",
        autumnRootTitle = "Корневая фосфорно-калийная подкормка без азота",
        winterTitle = "Замульчировать корни и подготовить побеги к зиме"
    ),
    popularPlantTemplate(
        id = "peony",
        name = "Пион",
        aliases = setOf("пион", "пионы", "пион травянистый"),
        supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
        supportedNaturalZones = NaturalZone.entries.toSet() - setOf(NaturalZone.TUNDRA_AND_FOREST_TUNDRA, NaturalZone.HUMID_SUBTROPICS),
        openGroundStartOffsetDays = -5,
        pruningTitle = "Весенняя санитарная обрезка остатков пиона",
        pruningNote = "Удалите только отмершие прошлогодние остатки, не повреждая молодые почки; основную обрезку здоровой листвы выполняют осенью после её отмирания.",
        summerCareTitle = "Обрезать отцветшие цветки пиона",
        summerCareNote = "Удалите цветок до первого полноценного листа, но сохраните зелёную листву для питания корневища.",
        waterIntervalDays = 7,
        diseaseLabel = "пиона",
        autumnRootTitle = "Корневая фосфорно-калийная подкормка пиона",
        winterTitle = "Замульчировать корневую зону после промерзания поверхности",
        autumnPruningTitle = "Осенняя обрезка пиона после отмирания листвы",
        autumnPruningNote = "Срежьте стебли до 3–5 см над почвой и удалите больную листву с участка, чтобы снизить запас инфекции."
    ),
    popularPlantTemplate(
        id = "rose",
        name = "Роза",
        aliases = setOf("роза", "розы", "роза садовая"),
        supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
        supportedNaturalZones = NaturalZone.entries.toSet() - NaturalZone.TUNDRA_AND_FOREST_TUNDRA,
        openGroundStartOffsetDays = -14,
        pruningTitle = "Весенняя обрезка розы",
        pruningNote = "После ослабления сильных морозов удалите мёртвые, больные, трущиеся и растущие внутрь побеги. Силу укорачивания выбирайте по группе розы.",
        summerCareTitle = "Летняя обрезка после цветения",
        summerCareNote = "У повторноцветущих роз удаляйте отцветшие цветки; однократно цветущие кустовые розы корректируйте после завершения цветения.",
        waterIntervalDays = 6,
        diseaseLabel = "розы",
        autumnRootTitle = "Корневая фосфорно-калийная подкормка розы без азота",
        winterTitle = "Окучить основание и подготовить сухое зимнее укрытие"
    ),
    popularPlantTemplate(
        id = "apple",
        name = "Яблоня",
        aliases = setOf("яблоня", "яблони", "яблоко"),
        supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
        supportedNaturalZones = NaturalZone.entries.toSet() - NaturalZone.TUNDRA_AND_FOREST_TUNDRA,
        openGroundStartOffsetDays = -35,
        pruningTitle = "Позднезимняя или ранневесенняя обрезка яблони",
        pruningNote = "До распускания почек удалите сухие, больные, пересекающиеся ветви и волчки; не проводите сильную обрезку в период экстремального мороза.",
        summerCareTitle = "Летняя санитарная обрезка и удаление волчков",
        summerCareNote = "Удаляйте только явно ненужные вертикальные побеги и больную древесину; крупные формирующие срезы оставьте на период покоя.",
        waterIntervalDays = 14,
        diseaseLabel = "яблони",
        autumnRootTitle = "Осенняя корневая фосфорно-калийная подкормка яблони",
        winterTitle = "Очистить приствольный круг и защитить ствол на зиму"
    ),
    popularPlantTemplate(
        id = "pear",
        name = "Груша",
        aliases = setOf("груша", "груши", "грушевое дерево"),
        supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
        supportedNaturalZones = NaturalZone.entries.toSet() - NaturalZone.TUNDRA_AND_FOREST_TUNDRA,
        openGroundStartOffsetDays = -32,
        pruningTitle = "Позднезимняя или ранневесенняя обрезка груши",
        pruningNote = "До распускания почек удалите сухие, больные и загущающие ветви, сохраняя выраженный центральный проводник; избегайте крупных срезов в мороз.",
        summerCareTitle = "Летняя санитарная обрезка груши",
        summerCareNote = "Удалите волчки и ветви с признаками бактериального ожога до здоровой ткани; инструмент дезинфицируйте между срезами.",
        waterIntervalDays = 14,
        diseaseLabel = "груши",
        autumnRootTitle = "Осенняя корневая фосфорно-калийная подкормка груши",
        winterTitle = "Убрать мумифицированные плоды и защитить ствол на зиму"
    )
)

private fun popularPlantTemplate(
    id: String,
    name: String,
    aliases: Set<String>,
    supportedCultivationTypes: Set<CultivationType>,
    supportedNaturalZones: Set<NaturalZone>,
    openGroundStartOffsetDays: Int,
    greenhouseStartOffsetDays: Int = -14,
    pruningTitle: String,
    pruningNote: String,
    summerCareTitle: String,
    summerCareNote: String,
    waterIntervalDays: Int,
    diseaseLabel: String,
    autumnRootTitle: String,
    winterTitle: String,
    autumnPruningTitle: String? = null,
    autumnPruningNote: String? = null
): PlantCareTemplate {
    val steps = mutableListOf(
        CareStepTemplate("pruning", pruningTitle, offsetDays = 0, windowBeforeDays = 3, windowAfterDays = 5, note = pruningNote),
        CareStepTemplate(
            id = "post_pruning_disease_treatment",
            title = "Опрыскивание после обрезки для профилактики болезней",
            offsetDays = 3,
            windowBeforeDays = 1,
            windowAfterDays = 4,
            weatherLimits = sprayWeatherLimits(),
            productDescription = "Средство профилактического действия против болезней, разрешённое для $diseaseLabel.",
            note = "Обрабатывайте только при наличии показаний и в разрешённую для культуры фазу; не опрыскивайте по открытым цветкам, соблюдайте инструкцию и срок ожидания."
        ),
        CareStepTemplate(
            id = "spring_root_feeding",
            title = "Весенняя корневая подкормка",
            offsetDays = 8,
            windowBeforeDays = 2,
            windowAfterDays = 5,
            productDescription = "Корневое удобрение, разрешённое для $diseaseLabel и выбранной фазы развития.",
            weatherLimits = WeatherLimits(maximumPrecipitationMm = 5.0),
            note = "Вносите по влажной почве и только с учётом анализа грунта, возраста растения и инструкции удобрения."
        ),
        CareStepTemplate(
            id = "moisture_and_health_check",
            title = "Проверить влажность, листья и побеги",
            offsetDays = 7,
            recurrence = CareRecurrence(RepeatType.CUSTOM, waterIntervalDays, 18),
            note = "Поливайте только по фактической влажности. Одновременно отмечайте пятна, налёт, деформации и вредителей."
        ),
        CareStepTemplate(
            id = "spring_foliar_feeding",
            title = "Весенняя внекорневая подкормка микроэлементами",
            offsetDays = 21,
            windowBeforeDays = 2,
            windowAfterDays = 4,
            weatherLimits = sprayWeatherLimits(),
            productDescription = "Внекорневое удобрение с микроэлементами, разрешённое для $diseaseLabel.",
            note = "Проводите только при подтверждённой потребности или признаках дефицита, утром либо вечером по сухим листьям; концентрацию не повышайте."
        ),
        CareStepTemplate(
            id = "season_disease_spraying",
            title = "Плановое опрыскивание от болезней по результатам осмотра",
            offsetDays = 28,
            windowBeforeDays = 2,
            windowAfterDays = 4,
            weatherLimits = sprayWeatherLimits(),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 6),
            productDescription = "Фунгицидное или биологическое средство, разрешённое для $diseaseLabel и выявленной болезни.",
            note = "Сначала определите проблему. Чередуйте только разрешённые механизмы действия, не обрабатывайте во время цветения и соблюдайте срок ожидания до сбора урожая."
        ),
        CareStepTemplate(
            id = "pest_spraying_if_needed",
            title = "Опрыскивание от вредителей при подтверждённом заселении",
            offsetDays = 32,
            windowBeforeDays = 2,
            windowAfterDays = 4,
            weatherLimits = sprayWeatherLimits(),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 6),
            productDescription = "Средство против выявленного вредителя, разрешённое для $diseaseLabel.",
            note = "Обрабатывайте только после обнаружения вредителя и достижения порога вредоносности; защищайте опылителей и строго соблюдайте инструкцию."
        ),
        CareStepTemplate("summer_pruning", summerCareTitle, offsetDays = 45, windowBeforeDays = 5, windowAfterDays = 10, note = summerCareNote),
        CareStepTemplate(
            id = "summer_root_feeding",
            title = "Летняя корневая подкормка",
            offsetDays = 52,
            windowBeforeDays = 4,
            windowAfterDays = 7,
            productDescription = "Корневое удобрение, разрешённое для $diseaseLabel в период роста или плодоношения.",
            note = "Подбирайте состав по фазе развития и анализу почвы; не превышайте дозировку и не вносите в пересохший грунт."
        ),
        CareStepTemplate(
            id = "summer_foliar_feeding",
            title = "Летняя внекорневая подкормка по диагностике",
            offsetDays = 67,
            windowBeforeDays = 3,
            windowAfterDays = 5,
            weatherLimits = sprayWeatherLimits(),
            productDescription = "Внекорневое удобрение для коррекции подтверждённого дефицита, разрешённое для $diseaseLabel.",
            note = "Внекорневая подкормка дополняет, но не заменяет корневое питание. Не смешивайте средства без проверки совместимости."
        )
    )
    if (autumnPruningTitle != null && autumnPruningNote != null) {
        steps += CareStepTemplate("autumn_pruning", autumnPruningTitle, anchor = CareAnchor.SAFE_AUTUMN_DATE, offsetDays = -21, windowBeforeDays = 7, windowAfterDays = 5, note = autumnPruningNote)
    }
    steps += CareStepTemplate(
        id = "autumn_root_feeding",
        title = autumnRootTitle,
        anchor = CareAnchor.SAFE_AUTUMN_DATE,
        offsetDays = -35,
        windowBeforeDays = 7,
        windowAfterDays = 5,
        productDescription = "Осеннее корневое удобрение без избытка азота, разрешённое для $diseaseLabel.",
        note = "Срок уточняйте по состоянию растения и почвы; поздний азот может задержать вызревание тканей."
    )
    steps += CareStepTemplate("winter_preparation", winterTitle, anchor = CareAnchor.SAFE_AUTUMN_DATE, offsetDays = -7, windowBeforeDays = 5, windowAfterDays = 7, note = "Удалите больные остатки, проверьте мульчу и защиту от морозов, ветра, грызунов и солнечных ожогов с учётом местного климата.")

    return PlantCareTemplate(
        id = id,
        canonicalName = name,
        aliases = aliases,
        version = 3,
        supportedCultivationTypes = supportedCultivationTypes,
        supportedNaturalZones = supportedNaturalZones,
        openGroundStartOffsetDays = openGroundStartOffsetDays,
        greenhouseStartOffsetDays = greenhouseStartOffsetDays,
        zoneStartOffsetDays = zoneStartAdjustments(),
        openGroundEndOffsetDays = -10,
        greenhouseEndOffsetDays = 14,
        zoneEndOffsetDays = zoneEndAdjustments(),
        steps = steps
    )
}

private fun sprayWeatherLimits(): WeatherLimits = WeatherLimits(
    minimumNightTemperatureC = 5.0,
    maximumPrecipitationMm = 2.0,
    maximumWindMetersPerSecond = 5.0
)

private fun zoneStartAdjustments(): Map<NaturalZone, Int> = mapOf(
    NaturalZone.TUNDRA_AND_FOREST_TUNDRA to 14,
    NaturalZone.TAIGA to 7,
    NaturalZone.MIXED_AND_BROADLEAF_FORESTS to 0,
    NaturalZone.FOREST_STEPPE to -3,
    NaturalZone.STEPPE_AND_SEMI_DESERT to -7,
    NaturalZone.HUMID_SUBTROPICS to -18
)

private fun zoneEndAdjustments(): Map<NaturalZone, Int> = mapOf(
    NaturalZone.TUNDRA_AND_FOREST_TUNDRA to -14,
    NaturalZone.TAIGA to -7,
    NaturalZone.MIXED_AND_BROADLEAF_FORESTS to 0,
    NaturalZone.FOREST_STEPPE to 3,
    NaturalZone.STEPPE_AND_SEMI_DESERT to 7,
    NaturalZone.HUMID_SUBTROPICS to 21
)
