package ru.samates.gardenspa.domain

/** Each alternative keeps its own season, composition and application method. */
object ExpandedProgramProducts {
    private val hydrangeas = ExpandedCarePrograms.hydrangeaIds
    private val conifers = ExpandedCarePrograms.conifers.keys
    private const val FERTIKA = "https://fertika.com/product/dom-sad-i-ogorod/udobreniya/"
    private const val BUI = "https://bhzshop.ru/catalog/akvarin-garden/"
    private fun feed(id: String, name: String, maker: String, crops: Set<String>, composition: String,
        instruction: String, url: String) = ProgramProduct(id, name, maker, "Подкормить: $name",
        composition, instruction, url, crops, sections = setOf(ProductSection.FERTILIZER))

    val products = listOf(
        feed("conifer_fertika_spring", "Хвойное для вечнозелёных, весна", "Fertika", conifers + hydrangeas,
            "NPK 10:7:14. Гранулированное питание хвойных и гортензий в начале роста.",
            "Поверхностная подкормка: 60 г/м², 1–2 раза за сезон, заделать и полить. Отступить от ствола не менее 15 см. Гранулы не растворять. Посадочная норма отличается от подкормки.", FERTIKA + "sad-i-ogorod/prod-7/"),
        feed("conifer_fertika_summer", "Хвойное для вечнозелёных, лето", "Fertika", conifers + hydrangeas,
            "NPK 8:7:21. Летнее питание; при выборе перенесите дату на июнь–август.",
            "Корневой полив: 30 г/10 л в первой половине лета, 50 г/10 л во второй. По инструкции интервал 1–2 недели; учитывать ранее внесённое питание. Это летний вариант, не замена ранневесенней дозировки.", FERTIKA + "khvoynye-vechnozelenye/prod-23/"),
        feed("conifer_organic", "Удобрение для хвойных, гранулы", "Органик Микс", conifers, "NPK 6:3:3; органическое питание хвойных.",
            "Первый год: 100 г/растение, 2–3-летние саженцы: 200 г, взрослые: 300 г. Распределить по почве, заделать и полить. По карточке повтор через 30–45 дней; учитывать сезон и состояние растения.",
            "https://organic-mix.ru/catalog/udobrenie-dlya-khvoynykh-organik-miks-850-gr/"),
        feed("conifer_bui", "Акварин для хвойных культур", "Буйский химический завод", conifers,
            "Водорастворимое питание хвойных с макро- и микроэлементами.",
            "Корневой полив: 20–30 г/10 л, 1–3 раза за вегетацию через 15–20 дней; последняя подкормка в конце августа — начале сентября. Расход — по размеру корневого кома и инструкции. Норму опрыскивания не переносить на полив.", BUI + "akvarin-dlya-khvoynykh-kultur/"),
        feed("bona_acid_plants", "Водорастворимое для голубики, рододендронов и кислотолюбивых растений", "BonaForte",
            conifers + hydrangeas + "blueberry", "NPK 8:8:18, магний 4%, сера; для кислолюбивых культур.",
            "Корневой раствор 15–20 г/10 л воды ИЛИ сухое внесение 15–20 г/м². Выберите один способ, частоту и расход воды проверьте по упаковке и ранее внесённому питанию. Не вносить обе нормы одновременно.",
            "https://www.bona-forte.ru/catalog/udobrenie-vodorastvorimoe-dlya-golubiki-rododendronov-i-kislotolyubivykh-rastenij-s-seroj-i-magniem-1-l/"),
        feed("hydrangea_bui", "Акварин для гортензий", "Буйский химический завод", hydrangeas,
            "Водорастворимое комплексное питание гортензий.",
            "Корневой полив: 10–20 г/10 л, расход 4–10 л/м². 1–3 подкормки за вегетацию через 15–20 дней. Учитывайте состояние почвы и уже внесённые удобрения.", BUI + "akvarin-dlya-gortenziy/"),
        feed("blueberry_bui", "Акварин для голубики", "Буйский химический завод", setOf("blueberry"),
            "Водорастворимое питание голубики и вересковых культур.",
            "Корневой полив: 10–20 г/10 л, обычная поливочная норма. 1–3 раза за вегетацию с интервалом 15–20 дней. Корневая и листовая дозировки различаются.", BUI + "akvarin-dlya-golubiki/"),
        feed("blueberry_fertika_autumn", "Хвойное осень для вечнозелёных и голубики", "Fertika", conifers + "blueberry",
            "NPK 7,2:12,1:21. Осенняя подкормка голубики и хвойных.",
            "Применять осенью по норме для своей культуры и возраста на упаковке. При выборе этого варианта установите осеннюю дату. Не использовать дозу весенней или летней марки Fertika.",
            FERTIKA + "khvoynye-vechnozelenye/fertika-khvoynoe-osen-dlya-vechnozelenykh-i-golubiki/"),
        feed("lawn_fertika", "Газонное весна–лето, бесхлорное", "Fertika", setOf("lawn"), "NPK 11:12:26. Гранулированное питание газона.",
            "Весной 4–5 кг/100 м². При активном росте после каждого второго скашивания 5–7 кг/100 м². Распределить равномерно и полить. Норма при закладке газона отличается; не использовать её для обычной подкормки.", FERTIKA + "fertika-gazonnoe-vesna-leto-beskhlornoe/"),
        feed("lawn_bui", "Акварин для газонов", "Буйский химический завод", setOf("lawn"), "Водорастворимое питание газона.",
            "Корневой полив: 20–25 г/10 л весной и после скашивания; для нестриженого газона — через 10–15 дней. Расход воды сверить с упаковкой и влажностью почвы. Не путать с отдельной нормой опрыскивания.", BUI + "akvarin-dlya-gazonov/"),
        feed("hydrangea_citric_acid", "Лимонная кислота", "Рецепт пользователя", hydrangeas,
            "Полив под корень при бледных листьях по схеме пользователя.",
            "2 столовые ложки развести в 10 л воды. Поливать под корень, не опрыскивать. Объём раствора на куст и частота в рецепте не заданы. Перед применением оценить кислотность и влажность почвы; если листья не бледные, работу не выполнять.", ""),
        ProgramProduct("hydrangea_ferovit", "Феровит", "Нэст-М", "Восполнить дефицит железа",
            "Хелат железа; применение при сохраняющейся бледности листьев после повторного осмотра.",
            "На текущей странице производителя для формы 100 мл/1 л: 2–3 мл/1 л воды для опрыскивания или полива. При хлорозе интервал 7–10 дней. Сверьте форму своей упаковки: у прежних ампул встречаются другие нормы. Осмотр через 5 дней не означает повторное внесение каждые 5 дней.",
            "https://nest-m.ru/ferovite/", hydrangeas, sections = ProductSection.entries.toSet()),
        ProgramProduct("hydrangea_antichlorozin", "Антихлорозин", "Органик Микс", "Восполнить дефицит железа",
            "Средство от железодефицитного хлороза; другой состав и норма, чем у Феровита.",
            "При дефиците железа: 3 г/1 л воды, полив 2–5 л/м² и опрыскивание до смачивания по инструкции производителя; повтор через 7–10 дней. Профилактическая норма отличается. Осмотр через 5 дней не является назначением повторной подкормки.",
            "https://organic-mix.ru/catalog/antikhlorozin/", hydrangeas, sections = ProductSection.entries.toSet()),
        feed("hydrangea_magnesium", "Сульфат магния", "Буйский химический завод", hydrangeas,
            "MgO 16,5%, S 13%; при признаках недостатка магния, не любое пожелтение является дефицитом.",
            "Корневой полив: 15–20 г/10 л, расход 10 л/5 м². По инструкции 4–5 подкормок через 15–20 дней при необходимости; учитывать внесённый магний. Здесь назначается одна процедура после осмотра.",
            "https://bhzshop.ru/catalog/kompleksnye-udobreniya/sulfat-magniya/"),
        feed("hydrangea_magbor", "Маг-бор", "Агровит", hydrangeas, "MgO 15%, B 1,4%. Добавлен по прямому запросу пользователя; содержит также бор.",
            "Норму и способ внесения для декоративных растений подтвердите по вашей упаковке. В карточке производителя указан состав, но нет читаемого регламента для гортензии. Не переносить дозу сульфата магния: избыток бора вреден. Выбирается только после сверки инструкции.",
            "https://agrovit87.ru/katalog/udobreniya/udobrenie-mag-bor-100-g")
    )

    fun feedingIds(crop: String): Set<String> = products.filter {
        crop in it.crops && !it.id.startsWith("hydrangea_") && !it.id.endsWith("_autumn")
    }.map { it.id }.toSet() + if (crop in hydrangeas) setOf("hydrangea_bui") else emptySet()

    fun includePlantVariants(product: ProgramProduct): ProgramProduct {
        val hydrangeaCrops = if ("hydrangea" in product.crops) {
            if (product.id == "bona_hydrangea") setOf("hydrangea-macrophylla") else ExpandedCarePrograms.hydrangeas.keys
        } else emptySet()
        val flowerCrops = if (product.id in setOf("chistotsvet_flowers", "fitoverm_flowers")) ExpandedCarePrograms.flowers.keys else emptySet()
        return product.copy(crops = product.crops + hydrangeaCrops + flowerCrops,
            problems = product.problems + if (product.id == "skor_rose") setOf("black_spot") else emptySet())
    }
}
