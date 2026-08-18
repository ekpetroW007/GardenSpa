package ru.samates.gardenspa.domain

private data class StepText(val title: String, val note: String)

private data class ProgramVariant(
    val baseId: String,
    val id: String,
    val name: String,
    val aliases: Set<String> = emptySet(),
    val stepText: Map<String, StepText> = emptyMap(),
    val removeSteps: Set<String> = emptySet(),
    val extraSteps: List<CareStepTemplate> = emptyList(),
    val cultivationTypes: Set<CultivationType>? = null,
    val startShiftDays: Int = 0,
    val endShiftDays: Int = 0
)

private val programVariants = listOf(
    ProgramVariant("tomato", "tomato-determinate", "Томат детерминантный", setOf("томат кустовой", "помидор детерминантный", "помидор кустовой"), removeSteps = setOf("pruning", "summer_pruning"), endShiftDays = -14),
    ProgramVariant("tomato", "tomato-indeterminate", "Томат индетерминантный", setOf("томат высокорослый", "помидор индетерминантный"), stepText = mapOf(
        "pruning" to StepText("Формировка и пасынкование индетерминантного томата", "Ведите растение в один или два стебля, регулярно удаляйте небольшие пасынки по сухой листве и подвязывайте сохраняемые побеги."),
        "summer_pruning" to StepText("Повторить пасынкование и проверить подвязку", "Не удаляйте за один раз много здоровых листьев; сохраняйте листья над наливающимися кистями и обеспечьте проветривание.")
    ), endShiftDays = 7),

    ProgramVariant("cucumber", "cucumber-bush", "Огурец кустовой", setOf("короткоплетистый огурец"), removeSteps = setOf("pruning", "summer_pruning"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
    ProgramVariant("cucumber", "cucumber-vining", "Огурец плетистый", setOf("длинноплетистый огурец", "огурец для шпалеры"), stepText = mapOf(
        "pruning" to StepText("Формировка плетистого огурца", "Подвяжите главную плеть к опоре и формируйте боковые побеги по схеме сорта; не удаляйте здоровые листья без необходимости."),
        "summer_pruning" to StepText("Направить плети и повторить формировку", "Регулярно направляйте рост по шпалере, не перегибая плети, и удаляйте только повреждённые части.")
    )),

    ProgramVariant("hydrangea", "hydrangea-paniculata", "Гортензия метельчатая", setOf("hydrangea paniculata"), stepText = mapOf(
        "pruning" to StepText("Весенняя обрезка гортензии метельчатой", "До активного роста укоротите прошлогодние побеги до пары здоровых почек; силу обрезки выбирайте по желаемой высоте и прочности куста.")
    ), removeSteps = setOf("summer_pruning")),
    ProgramVariant("hydrangea", "hydrangea-arborescens", "Гортензия древовидная", setOf("hydrangea arborescens"), stepText = mapOf(
        "pruning" to StepText("Весенняя обрезка гортензии древовидной", "До активного роста укоротите прошлогодние побеги до пары здоровых почек; слабые и повреждённые стебли удалите полностью.")
    ), removeSteps = setOf("summer_pruning")),
    ProgramVariant("hydrangea", "hydrangea-macrophylla", "Гортензия крупнолистная", setOf("hydrangea macrophylla"), stepText = mapOf(
        "summer_pruning" to StepText("Удалить отцветшие соцветия крупнолистной гортензии", "После цветения срежьте соцветия над первой парой сильных почек. Не укорачивайте здоровые прошлогодние побеги: на них заложены цветочные почки.")
    ), removeSteps = setOf("pruning")),
    ProgramVariant("hydrangea", "hydrangea-quercifolia", "Гортензия дуболистная", setOf("hydrangea quercifolia"), stepText = mapOf(
        "pruning" to StepText("Санитарная обрезка гортензии дуболистной", "Весной удалите только мёртвые, повреждённые и чрезмерно длинные побеги; сильная формирующая обрезка обычно не требуется."),
        "summer_pruning" to StepText("Удалить отцветшие соцветия гортензии дуболистной", "После цветения удалите соцветия без сильного укорачивания прошлогодней древесины.")
    )),
    ProgramVariant("hydrangea", "hydrangea-climbing", "Гортензия черешковая", setOf("гортензия плетистая", "hydrangea petiolaris"), stepText = mapOf(
        "summer_pruning" to StepText("Обрезка черешковой гортензии после цветения", "Сразу после цветения укоротите только чрезмерно длинные побеги и сохраните основную цветущую древесину; сильную омолаживающую обрезку растяните на несколько лет.")
    ), removeSteps = setOf("pruning")),

    ProgramVariant("peony", "peony-herbaceous", "Пион травянистый", setOf("травянистые пионы")),
    ProgramVariant("peony", "peony-itoh", "Пион ИТО-гибрид", setOf("пион ито", "пион межсекционный"), stepText = mapOf(
        "pruning" to StepText("Весенняя санитарная обрезка ИТО-пиона", "Удалите только отмершие части над низким одревесневшим основанием, не повреждая молодые почки."),
        "autumn_pruning" to StepText("Осенняя обрезка ИТО-пиона", "После отмирания листвы срежьте травянистую часть, сохранив короткое одревесневшее основание и видимые почки.")
    )),
    ProgramVariant("peony", "peony-tree", "Пион древовидный", setOf("древовидные пионы"), stepText = mapOf(
        "pruning" to StepText("Лёгкая санитарная обрезка древовидного пиона", "В конце зимы удалите мёртвые стебли до здоровой почки. Здоровый одревесневший каркас не срезайте до земли."),
        "summer_pruning" to StepText("Лёгкая обрезка древовидного пиона после цветения", "При необходимости укоротите отцветшие побеги непосредственно над новым приростом, сохраняя постоянный древесный каркас.")
    ), removeSteps = setOf("autumn_pruning")),

    ProgramVariant("rose", "rose-hybrid-tea", "Роза чайно-гибридная", setOf("чайно гибридная роза"), stepText = mapOf(
        "pruning" to StepText("Весенняя обрезка чайно-гибридной розы", "Удалите слабые и повреждённые побеги, сильные укоротите примерно до 4–6 почек, более слабые — до 2–4 почек."),
        "summer_pruning" to StepText("Удалить отцветшие цветки чайно-гибридной розы", "Срезайте отцветшие цветки до сильного листа, чтобы поддержать повторное цветение.")
    )),
    ProgramVariant("rose", "rose-floribunda", "Роза флорибунда", setOf("флорибунда"), stepText = mapOf(
        "pruning" to StepText("Весенняя обрезка розы флорибунда", "Удалите слабые и повреждённые побеги; сильные побеги оставляйте длиннее, чем у чайно-гибридных роз, примерно 25–30 см от земли."),
        "summer_pruning" to StepText("Удалить отцветшие кисти розы флорибунда", "Регулярно удаляйте отцветшие кисти до сильного листа для непрерывного повторного цветения.")
    )),
    ProgramVariant("rose", "rose-climbing", "Роза плетистая повторноцветущая", setOf("роза клаймбер", "плетистая роза"), stepText = mapOf(
        "pruning" to StepText("Зимняя обрезка повторноцветущей плетистой розы", "Сохраните основные длинные побеги, подвяжите их ближе к горизонтали, а цветшие боковые побеги укоротите до нескольких почек."),
        "summer_pruning" to StepText("Удалить отцветшие кисти плетистой розы", "После волн цветения удаляйте отцветшие кисти и подвязывайте новые гибкие побеги к опоре.")
    )),
    ProgramVariant("rose", "rose-rambling", "Роза рамблер однократноцветущая", setOf("роза рамблер", "роза однократноцветущая плетистая"), stepText = mapOf(
        "pruning" to StepText("Санитарная обрезка розы рамблер", "До цветения ограничьтесь удалением мёртвых и повреждённых частей, чтобы не потерять цветение на прошлогодних побегах."),
        "summer_pruning" to StepText("Формирующая обрезка рамблера после цветения", "Сразу после цветения удалите часть старых цветших побегов у основания и подвяжите молодые побеги замещения.")
    )),
    ProgramVariant("rose", "rose-shrub", "Роза кустовая повторноцветущая", setOf("роза шраб", "английская роза"), stepText = mapOf(
        "pruning" to StepText("Весенняя обрезка повторноцветущей кустовой розы", "Удалите повреждённые ветви, сильный прирост укоротите примерно на треть и периодически вырезайте старые стебли у основания."),
        "summer_pruning" to StepText("Удалить отцветшие цветки кустовой розы", "Удаляйте отцветшие цветки для повторного цветения, сохраняя естественную форму куста.")
    )),

    ProgramVariant("apple", "apple-standard-rootstock", "Яблоня на сильнорослом подвое", setOf("яблоня стандартная", "яблоня семенной подвой"), stepText = mapOf(
        "pruning" to StepText("Позднезимняя обрезка сильнорослой яблони", "Сформируйте устойчивый центральный проводник и ярусы скелетных ветвей; ежегодно удаляйте сухие, больные, пересекающиеся ветви и волчки.")
    )),
    ProgramVariant("apple", "apple-dwarf-rootstock", "Яблоня на карликовом подвое", setOf("яблоня карликовая", "яблоня полукарликовая"), stepText = mapOf(
        "pruning" to StepText("Позднезимняя обрезка яблони на карликовом подвое", "Сохраняйте центральный проводник и опору, удаляйте конкурирующие и слишком толстые ветви; избегайте сильной обрезки, вызывающей лишний рост."),
        "summer_pruning" to StepText("Летняя корректировка карликовой яблони", "Удалите только волчки и конкуренты проводника, проверьте подвязку к постоянной опоре и не перегружайте молодые ветви плодами.")
    )),
    ProgramVariant("pear", "pear-standard-rootstock", "Груша на сильнорослом подвое", setOf("груша стандартная")),
    ProgramVariant("pear", "pear-dwarf-rootstock", "Груша на слаборослом подвое", setOf("груша карликовая", "груша полукарликовая"), stepText = mapOf(
        "pruning" to StepText("Позднезимняя обрезка груши на слаборослом подвое", "Сохраняйте центральный проводник, опору и лёгкую пирамидальную крону; обрезайте минимально, чтобы не задерживать плодоношение.")
    )),

    ProgramVariant("garden-strawberry", "strawberry-june-bearing", "Земляника садовая короткого дня", setOf("клубника одноразовая", "клубника июньская"), extraSteps = listOf(
        CareStepTemplate("post_harvest_pruning", "Обновить землянику после окончания урожая", offsetDays = 70, windowAfterDays = 10, note = "Только у взрослой посадки после последнего сбора скосите старую листву, не повреждая коронки, проредите ряды и обеспечьте полив для отрастания.")
    )),
    ProgramVariant("garden-strawberry", "strawberry-everbearing", "Земляника садовая ремонтантная", setOf("клубника ремонтантная"), extraSteps = listOf(
        CareStepTemplate("runner_pruning", "Удалить лишние усы ремонтантной земляники", offsetDays = 28, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 4), note = "Если размножение не планируется, удаляйте усы чистым инструментом, не повреждая коронку; после первой волны урожая не скашивайте всю листву.")
    )),
    ProgramVariant("garden-strawberry", "strawberry-day-neutral", "Земляника садовая нейтрального дня", setOf("клубника нейтрального дня"), extraSteps = listOf(
        CareStepTemplate("runner_pruning", "Удалить усы земляники нейтрального дня", offsetDays = 21, recurrence = CareRecurrence(RepeatType.CUSTOM, 21, 6), note = "Регулярно удаляйте усы, чтобы растение направляло силы на длительное плодоношение; после урожая не проводите сплошное скашивание листвы.")
    )),

    ProgramVariant("sweet-pepper", "sweet-pepper-determinate", "Перец сладкий детерминантный", setOf("перец сладкий низкорослый"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
    ProgramVariant("sweet-pepper", "sweet-pepper-indeterminate", "Перец сладкий индетерминантный", setOf("перец сладкий высокорослый", "перец тепличный высокорослый"), cultivationTypes = setOf(CultivationType.GREENHOUSE), extraSteps = listOf(
        CareStepTemplate("pruning_training", "Формировать и подвязывать высокорослый перец", offsetDays = 12, recurrence = CareRecurrence(RepeatType.CUSTOM, 10, 8), note = "В защищённом грунте ведите растение по выбранной схеме стеблей, удаляйте только предусмотренные схемой побеги и регулярно проверяйте подвязку.")
    )),
    ProgramVariant("zucchini", "zucchini-bush", "Кабачок кустовой", setOf("цуккини кустовой")),
    ProgramVariant("zucchini", "zucchini-vining", "Кабачок плетистый", setOf("цуккини плетистый"), extraSteps = listOf(
        CareStepTemplate("pruning_training", "Направить плети плетистого кабачка", offsetDays = 10, recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 5), note = "Оставьте достаточно места или направьте плети по прочной опоре; не удаляйте здоровые листья без необходимости.")
    )),
    ProgramVariant("pumpkin", "pumpkin-bush", "Тыква кустовая", setOf("тыква компактная")),
    ProgramVariant("pumpkin", "pumpkin-vining", "Тыква плетистая", setOf("тыква длинноплетистая"), extraSteps = listOf(
        CareStepTemplate("pruning_training", "Направить плети плетистой тыквы", offsetDays = 12, recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 5), note = "Размещайте плети свободно без перегибов; на шпалеру поднимайте только сорта с плодами подходящего размера и предусматривайте поддержку плодов.")
    )),
    ProgramVariant("garlic", "garlic-hardneck", "Чеснок стрелкующийся", setOf("чеснок озимый стрелкующийся", "чеснок hardneck"), extraSteps = listOf(
        CareStepTemplate("scape_pruning", "Удалить стрелки чеснока", offsetDays = 42, windowBeforeDays = 5, windowAfterDays = 7, note = "Удалите цветочные стрелки вскоре после начала закручивания, оставив несколько контрольных растений для наблюдения за созреванием.")
    )),
    ProgramVariant("garlic", "garlic-softneck", "Чеснок нестрелкующийся", setOf("чеснок softneck"))
)

internal fun expandProgramVarieties(baseTemplates: List<PlantCareTemplate>): List<PlantCareTemplate> = baseTemplates.flatMap { base ->
    programVariants.filter { it.baseId == base.id }.ifEmpty { return@flatMap listOf(base) }.map { it.applyTo(base) }
}

private fun ProgramVariant.applyTo(base: PlantCareTemplate): PlantCareTemplate {
    val transformedSteps = base.steps.mapNotNull { step ->
        if (step.id in removeSteps) return@mapNotNull null
        stepText[step.id]?.let { step.copy(title = it.title, note = it.note) } ?: step
    } + extraSteps
    val hasInitialPruning = transformedSteps.any { it.id == "pruning" }
    val finalSteps = if (hasInitialPruning) transformedSteps else transformedSteps.map { step ->
        if (step.id == "post_pruning_disease_treatment") step.copy(id = "early_disease_treatment", title = "Ранняя профилактика болезней по результатам осмотра") else step
    }
    return base.copy(
        id = id,
        canonicalName = name,
        aliases = base.aliases + aliases + name,
        version = base.version + 1,
        supportedCultivationTypes = cultivationTypes ?: base.supportedCultivationTypes,
        openGroundStartOffsetDays = base.openGroundStartOffsetDays + startShiftDays,
        greenhouseStartOffsetDays = base.greenhouseStartOffsetDays + startShiftDays,
        openGroundEndOffsetDays = base.openGroundEndOffsetDays + endShiftDays,
        greenhouseEndOffsetDays = base.greenhouseEndOffsetDays + endShiftDays,
        steps = finalSteps
    )
}
