package ru.samates.gardenspa.domain

object ExpandedCarePrograms {
    val hydrangeas = linkedMapOf("hydrangea-paniculata" to "Гортензия метельчатая",
        "hydrangea-macrophylla" to "Гортензия крупнолистная", "hydrangea-arborescens" to "Гортензия древовидная")
    val conifers = linkedMapOf("thuja" to "Туя", "spruce" to "Ель", "pine" to "Сосна", "juniper" to "Можжевельник")
    val flowers = linkedMapOf("lily" to "Лилия", "tulip" to "Тюльпан", "gladiolus" to "Гладиолус", "iris" to "Ирис")
    val stoneFruit = linkedMapOf("cherry" to "Вишня", "plum" to "Слива")
    val hydrangeaIds = hydrangeas.keys + "hydrangea"
    const val SHELTER_STEP = "hydrangea_winter_cover"
    private const val LEAF_NOTICE = "Схема пользователя. Применять средство только при указанных признаках, а не автоматически. Бледность и пожелтение могут иметь разные причины: оцените влажность, корни и кислотность почвы. Если признаков нет, средство не требуется."

    fun expand(original: List<PlantCareTemplate>): List<PlantCareTemplate> {
        val hydrangea = original.single { it.id == "hydrangea" }
        val hydrangeaTemplates = (listOf(hydrangea) + hydrangeas.map { (id, name) ->
            hydrangea.copy(id = id, canonicalName = name, aliases = setOf(name.lowercase()))
        }).map { template ->
            val extra = mutableListOf<CareStepTemplate>()
            if (template.id in setOf("hydrangea-paniculata", "hydrangea-arborescens")) extra += CareStepTemplate(
                "hydrangea_pruning", "Обрезка", offsetDays = -14,
                note = "Ранней весной, до активного роста: обрезка метельчатой или древовидной гортензии. Сверьте сорт и возраст куста. У крупнолистной такую обрезку не назначать. ${SpringCarePrograms.TEMPERATURE_NOTICE}")
            extra += CareStepTemplate("hydrangea_pale_acid~hydrangea_citric_acid", "Осмотр: бледные листья", offsetDays = 28,
                productDescription = "Лимонная кислота", note = "$LEAF_NOTICE\nЛимонная кислота: 2 столовые ложки развести в 10 л воды. Полив под корень. Расход на куст в рецепте не уточнён; не использовать для опрыскивания.")
            extra += CareStepTemplate("hydrangea_pale_iron", "Осмотр: бледные листья — повтор через 5 дней", offsetDays = 33,
                productDescription = "Феровит или Антихлорозин", note = "$LEAF_NOTICE\nЧерез 5 дней повторно оцените листья. Если бледность сохраняется, выберите Феровит ИЛИ Антихлорозин с собственной инструкцией. Не смешивать варианты.")
            extra += CareStepTemplate("hydrangea_yellow_magnesium", "Осмотр: пожелтение листьев — ещё через 5 дней", offsetDays = 38,
                productDescription = "Сульфат магния или Маг-бор", note = "$LEAF_NOTICE\nЕщё через 5 дней оцените пожелтение. Выберите сульфат магния ИЛИ Маг-бор; второй также содержит бор и не является заменой с тем же составом.")
            if (template.id == "hydrangea-macrophylla") extra += CareStepTemplate(SHELTER_STEP, "Укрытие", CareAnchor.SAFE_AUTUMN_DATE, 21,
                note = "Поздней осенью подготовьте крупнолистную гортензию к зиме и укройте с сохранением цветочных почек. Работа исключается при выборе южного региона. Дата — ориентир по местному окончанию безморозного периода; уточните по погоде и сорту.", excludeInSouthernRegion = true)
            template.copy(version = 9, steps = template.steps + extra)
        }
        val newFlowers = flowers.map { (id, name) ->
            PlantCareTemplate(id, name, setOf(name.lowercase()), 1, setOf(CultivationType.OPEN_GROUND), -7,
                steps = listOf(CareStepTemplate("nutrition", "Подкормить: $name", offsetDays = 14,
                    note = "Подкормка только при активном росте по инструкции выбранного удобрения. Срок зависит от вида, сорта и посадки. Выберите одно средство; не вносите все аналоги вместе.")))
        }
        val newFruit = stoneFruit.map { (id, name) ->
            original.single { it.id == "apple" }.copy(id = id, canonicalName = name, aliases = setOf(name.lowercase()), version = 1,
                steps = listOf(CareStepTemplate("nutrition", "Подкормить: $name", offsetDays = 0,
                    note = "Питание при начале роста по оттаявшей почве. Выберите удобрение для плодовых деревьев; норма по площади приствольного круга не равна норме на дерево.")))
        }
        val newConifers = conifers.map { (id, name) ->
            PlantCareTemplate(id, name, setOf(name.lowercase()), 1, setOf(CultivationType.OPEN_GROUND), -14,
                steps = listOf(
                    CareStepTemplate("conifer_sun_protection", "Защита от весеннего солнца", offsetDays = -21,
                        note = "Для молодых или чувствительных растений на открытом месте сохраните притенение, пока корни в мёрзлой почве. Убирайте защиту постепенно после оттаивания; не укутывайте крону герметично. Срок уточняйте по сорту и месту. Источник: https://www.rhs.org.uk/plants/types/conifers/growing-guide"),
                    CareStepTemplate("conifer_sanitation", "Удалить сухие и повреждённые ветви", offsetDays = 0,
                        note = "Санитарная обрезка только явно погибших частей. Не срезайте здоровую верхушку и не переносите сильную формирующую обрезку на все хвойные. ${SpringCarePrograms.TEMPERATURE_NOTICE}"),
                    CareStepTemplate("nutrition~conifer_fertika_spring", "Подкормить: $name", offsetDays = 14,
                        productDescription = "Хвойное для вечнозелёных, весна — Fertika",
                        note = "После оттаивания почвы оцените прирост и потребность в питании. При достаточном питании подкормка не обязательна. При необходимости выберите одну схему для хвойных. Норма и повтор — по выбранному продукту. Не подкармливать сухое или недавно пересаженное растение без сверки инструкции."),
                    CareStepTemplate("conifer_water", "Полив по влажности почвы", offsetDays = 21,
                        recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 8),
                        note = "Поливайте при недостатке влаги, учитывая осадки, возраст растения и дренаж. Если почва влажная, полив пропустите. Не допускать застоя воды у корней."),
                    CareStepTemplate("conifer_winter", "Подготовить хвойное растение к зиме", CareAnchor.SAFE_AUTUMN_DATE, 7,
                        note = "При сухой осени увлажните почву до её промерзания. У многоствольных и шаровидных сортов свободно закрепите ветви от тяжёлого снега, не стягивая хвою. Железный купорос для безлистных ветвей не применять по вечнозелёной хвое. Источник: https://fertika.com/product/dom-sad-i-ogorod/udobreniya/khvoynye-vechnozelenye/prod-23/")
                ))
        }
        return (original.filterNot { it.id == "hydrangea" } + hydrangeaTemplates + newConifers + newFlowers + newFruit).map { template ->
            if (template.id != "blueberry" && template.id !in conifers) template else template.copy(steps = template.steps +
                CareStepTemplate("acid_plants_autumn_nutrition~blueberry_fertika_autumn", "Осенняя подкормка при необходимости",
                    CareAnchor.SAFE_AUTUMN_DATE, -30, productDescription = "Хвойное осень для вечнозелёных и голубики — Fertika",
                    note = "Осенью при необходимости: выберите норму для своей культуры и возраста на упаковке Fertika NPK 7,2:12,1:21. Учитывайте уже внесённое питание. Источник: https://fertika.com/product/dom-sad-i-ogorod/udobreniya/khvoynye-vechnozelenye/fertika-khvoynoe-osen-dlya-vechnozelenykh-i-golubiki/"))
        }
    }
}
