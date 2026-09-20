package ru.samates.gardenspa.domain

/** Phase dates are checkpoints; the actual buds and each product's label take precedence. */
object SpringCarePrograms {
    val woodyCrops = setOf("apple", "pear", "hydrangea", "rose", "blackberry", "raspberry", "currant", "blueberry", "cherry", "plum") + ExpandedCarePrograms.hydrangeas.keys
    const val EARLY_STEP = "before_bud_break"
    const val PEST_STEP = "before_bud_break_pests"
    const val GREEN_STEP = "green_cone_biological"
    const val AFTER_FLOWERING_STEP = "post_flowering_tank_mix"
    const val AUTUMN_STEP = "after_leaf_fall_iron"
    const val TEMPERATURE_NOTICE = "Весенние работы проводят при устойчивой температуре +3…+5 °C и выше; если этикетка требует более высокой температуры, соблюдайте её. Для масляных средств — не ниже +4 °C. Для биопрепаратов +3…+5 °C не означает достаточную рабочую температуру."
    private const val SEPARATE = "Применять отдельно. Не соединять четыре ранневесенних средства и не добавлять их в биологическую смесь по зелёному конусу. Интервал между разными обработками — по инструкциям; календарный промежуток в программе не подтверждает совместимость."

    val products = listOf(
        ProgramProduct("spring_iron", "Железный купорос «Сила железа»", "Био-комплекс",
            "Обработать до распускания почек: железный купорос", "Обработка деревьев и кустарников до появления листьев.",
            "Рецепт производителя для деревьев и кустарников: 400 г купороса предварительно растворить в 1,5 л горячей воды; добавить 1 столовую ложку лимонной кислоты и 200 мл жидкого мыла, довести водой до 10 л. Применять только до появления листьев; не переносить концентрацию на зелёные ткани. Расход на растение и применимость к сорту сверить по упаковке. $SEPARATE",
            "https://bio-kompleks.ru/catalog/vsya_produktsiya/dachnyy-pomoshchnik-sila-zheleza-zheleznyy-kuporos/",
            crops = woodyCrops, cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("spring_bordeaux", "Бордоская смесь", "Green Belt",
            "Обработать до распускания почек: бордоская смесь", "Медный купорос и известь; вариант для защиты от болезней, не замена масляному средству от вредителей.",
            "Ранневесенняя обработка до распускания почек. Концентрацию и количество обоих компонентов выбирайте по инструкции вашей фасовки и культуры: производитель различает ранневесеннюю и вегетационную обработку. Для яблони и груши расход 2–5 л/дерево, смородины 1–1,5 л/куст; для малины уточнить этикетку. Не использовать железную тару, готовить непосредственно перед применением. $SEPARATE",
            "https://greenbelt.ru/products/bordoskaya-smes/", crops = setOf("apple", "pear", "currant", "raspberry"),
            cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("spring_30_plus", "Препарат 30 Плюс, ММЭ", "НПФ Собер",
            "Обработать до распускания почек: 30 Плюс", "Масляная обработка от зимующих стадий вредителей.",
            "500 мл на 10 л воды. Весной до распускания почек, при температуре выше +4 °C. Яблоня и груша: 2–5 л/дерево; смородина и малина: до 2 л/куст; декоративные культуры: 10 л/100 м². Одна обработка в этом весеннем этапе. Не применять по цветкам. $SEPARATE",
            "https://grepharm.ru/upload/Catalog-2025.pdf", crops = setOf("apple", "pear", "currant", "raspberry", "rose", "hydrangea"),
            cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("spring_profilaktin", "Профилактин, МКЭ", "Август",
            "Обработать до распускания почек: Профилактин", "Классический Профилактин: малатион 13 г/л + вазелиновое масло 658 г/л. Формы БИО и ЛАЙТ имеют другой состав.",
            "По каталогу производителя 2022 года: 500 мл на 10 л воды, до распускания почек при температуре не ниже +4 °C. Яблоня и груша: 2–5 л/дерево; смородина: 1–1,5 л/куст. Одна обработка, срок ожидания 60 дней. Перед применением проверьте актуальную этикетку именно формы МКЭ; не переносите этот регламент на БИО или ЛАЙТ. $SEPARATE",
            "https://dacha.avgust.com/upload/Avgust_catalog_2022_1.pdf", crops = setOf("apple", "pear", "currant"),
            cultivationTypes = setOf(CultivationType.OPEN_GROUND))
    ) + listOf(
        ProgramProduct("spring_bordeaux_avgust", "Бордоская жидкость, ВСК", "Август",
            "Обработать от болезней до распускания почек", "Готовый водно-суспензионный концентрат; не сухая смесь купороса и извести.",
            "Использовать норму ранневесенней обработки своей культуры с этикетки ВСК. Не переносить граммы сухой бордосской смеси в миллилитры этого концентрата. $TEMPERATURE_NOTICE $SEPARATE",
            "https://dacha.avgust.com/catalog/bordoskaya-zhidkost/", crops = setOf("apple", "pear", "cherry", "plum", "currant")),
        ProgramProduct("spring_bordeaux_fasko", "Бордоская смесь-Ф", "Фаско",
            "Обработать от болезней до распускания почек", "Сухая смесь сульфата меди и гидроксида кальция; другая фасовка.",
            "Сведения о продукте есть в каталоге производителя. Перед применением подтвердите на актуальной упаковке культуру, фазу и соотношение компонентов; нормы других фасовок не переносить. $TEMPERATURE_NOTICE $SEPARATE",
            "https://hitsad.ru/userfiles/pdf/fasko_2016.pdf", crops = setOf("apple", "pear", "cherry", "plum", "currant")),
        ProgramProduct("spring_profilaktin_light", "Профилактин ЛАЙТ, ВЭ", "Август",
            "Обработать от вредителей до распускания почек", "Вазелиновое масло 658 г/л; отдельная форма без малатиона.",
            "До распускания почек при температуре не ниже +4 °C. Норму разведения, расход и кратность сверить с этикеткой именно ЛАЙТ. $SEPARATE",
            "https://dacha.avgust.com/catalog/profilaktin-layt/", crops = setOf("apple", "pear", "cherry", "plum", "currant", "rose", "hydrangea") + ExpandedCarePrograms.hydrangeas.keys),
        ProgramProduct("spring_profilaktin_bio", "Профилактин БИО, ВЭ", "Август",
            "Обработать от вредителей до распускания почек", "Вазелиновое масло 658 г/л и матрин 2,2 г/л; отличается от классического Профилактина.",
            "0,5 л/10 л воды. До распускания почек при температуре не ниже +4 °C, одна обработка. Плодовые деревья: 1–5 л/дерево; смородина и декоративные кустарники: 0,5–1,5 л/куст. Расход зависит от кроны. $SEPARATE",
            "https://dacha.avgust.com/catalog/profilaktin-bio/", crops = setOf("apple", "pear", "cherry", "plum", "currant", "rose", "hydrangea") + ExpandedCarePrograms.hydrangeas.keys),
        ProgramProduct("autumn_iron", "Железный купорос — после листопада", "Био-комплекс",
            "После листопада: обработать железным купоросом", "Обработка безлистных ветвей листопадных деревьев и кустарников.",
            "После полного листопада, до морозов. Концентрацию и расход выберите по осеннему регламенту вашей упаковки и культуры. Не использовать по листьям и вечнозелёной хвое. Не смешивать с другими средствами. Весенний рецепт не переносится автоматически.",
            "https://bio-kompleks.ru/catalog/vsya_produktsiya/dachnyy-pomoshchnik-sila-zheleza-zheleznyy-kuporos/", crops = woodyCrops)
    )

    val diseaseIds = setOf("spring_bordeaux", "spring_bordeaux_avgust", "spring_bordeaux_fasko", "spring_iron")
    val pestIds = setOf("spring_30_plus", "spring_profilaktin", "spring_profilaktin_light", "spring_profilaktin_bio")

    val mixtureProducts: List<ProgramProduct> get() = FolkFertilizers.tankMixes.map { mixture ->
        ProgramProduct("mix_${mixture.id}", mixture.name, "Рецепт пользователя", "Обработать баковой смесью",
            mixture.purpose, "${mixture.ingredients}\n${mixture.preparation}\n${mixture.consumptionRate}\n${mixture.warning}",
            "", crops = woodyCrops)
    }

    fun withSpringStages(template: PlantCareTemplate): PlantCareTemplate {
        if (template.id !in woodyCrops) return template
        val mixture = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        val pest = products.firstOrNull { it.id == "spring_30_plus" && (template.id in it.crops || template.id in ExpandedCarePrograms.hydrangeaIds) }
        val afterFlowering = FolkFertilizers.tankMixes.single { it.id == "btu_protection_tank_mix" }
        return template.copy(
            version = maxOf(template.version + 1, 10),
            openGroundStartOffsetDays = template.openGroundStartOffsetDays - 21,
            steps = listOf(
                CareStepTemplate(EARLY_STEP, "Обработать от болезней до распускания почек", offsetDays = 0,
                    productDescription = "Бордосская смесь",
                    note = "Основное средство по схеме пользователя — бордосская смесь. Выберите производителя и проверьте наличие своей культуры в инструкции; для декоративных и кислолюбивых кустарников не переносите регламент яблони. Альтернатива по этой задаче — железный купорос. $TEMPERATURE_NOTICE\n$SEPARATE"),
                CareStepTemplate(PEST_STEP, "Обработать от вредителей до распускания почек", offsetDays = 7,
                    productDescription = pest?.displayName ?: "Препарат 30 Плюс или Профилактин — по регламенту культуры",
                    note = "Через 7 дней после этапа от болезней, только если почки ещё не распустились. Семь дней — плановый интервал, сверяйте допустимый перерыв по этикеткам. Выберите один препарат. $TEMPERATURE_NOTICE\n$SEPARATE"),
                CareStepTemplate(GREEN_STEP, "Обработать по зелёному конусу", offsetDays = 14,
                    productDescription = mixture.name,
                    note = "Срок ориентировочный: дождитесь зелёного конуса, учитывайте рабочую температуру препаратов. Если фаза прошла, не переносите эту обработку на другую фазу автоматически. $TEMPERATURE_NOTICE\n${mixture.ingredients}\n${mixture.preparation}\n${mixture.consumptionRate}\n${mixture.warning}\nИсточник: ${mixture.sourceName}")
            ) + template.steps.map { if (it.anchor == CareAnchor.START_DATE) it.copy(offsetDays = it.offsetDays + 21) else it } +
                (if (template.id in setOf("apple", "pear")) listOf(CareStepTemplate(AFTER_FLOWERING_STEP,
                    "После цветения: обработать баковой смесью от вредителей и болезней", offsetDays = 66,
                    productDescription = afterFlowering.name,
                    note = "Выполнять только после фактического окончания цветения; выберите смесь по задаче.\n${afterFlowering.purposeForDrug()}\n${afterFlowering.consumptionRate}")) else emptyList()) +
                CareStepTemplate("$AUTUMN_STEP~autumn_iron", "После листопада: обработать железным купоросом",
                    CareAnchor.SAFE_AUTUMN_DATE, 14, productDescription = "Железный купорос",
                    note = "После полного листопада, до морозов. Дата ориентировочная, по местному окончанию безморозного периода.\n${products.first { it.id == "autumn_iron" }.note}")
        )
    }
}
