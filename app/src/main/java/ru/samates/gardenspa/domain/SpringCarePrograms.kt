package ru.samates.gardenspa.domain

/** Phase dates are checkpoints; the actual buds and each product's label take precedence. */
object SpringCarePrograms {
    val woodyCrops = setOf("apple", "pear", "hydrangea", "rose", "blackberry", "raspberry", "currant", "blueberry")
    const val EARLY_STEP = "before_bud_break"
    const val GREEN_STEP = "green_cone_biological"
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
    )

    fun withSpringStages(template: PlantCareTemplate): PlantCareTemplate {
        if (template.id !in woodyCrops) return template
        val mixture = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        val first = products.first()
        return template.copy(
            version = 7,
            openGroundStartOffsetDays = template.openGroundStartOffsetDays - 21,
            steps = listOf(
                CareStepTemplate("$EARLY_STEP~${first.id}", "Обработать до распускания почек", offsetDays = 0,
                    productDescription = first.displayName,
                    note = "Ранневесенний этап. Выберите подходящее средство: железный купорос или бордосскую смесь по задаче защиты от болезней; 30 Плюс или Профилактин — от зимующих вредителей. Доступные варианты зависят от культуры. Выполняйте только по фактической фазе, до распускания почек.\n${first.note}"),
                CareStepTemplate(GREEN_STEP, "Обработать по зелёному конусу", offsetDays = 14,
                    productDescription = mixture.name,
                    note = "Срок ориентировочный: дождитесь зелёного конуса, учитывайте рабочую температуру препаратов. Если фаза прошла, не переносите эту обработку на другую фазу автоматически.\n${mixture.ingredients}\n${mixture.preparation}\n${mixture.consumptionRate}\n${mixture.warning}\nИсточник: ${mixture.sourceName}")
            ) + template.steps.map { it.copy(offsetDays = it.offsetDays + 21) }
        )
    }
}
