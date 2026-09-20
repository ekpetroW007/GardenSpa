package ru.samates.gardenspa.domain

/** Product-specific instructions checked against manufacturer cards on 2026-09-17. */
object SeasonalProgramProducts {
    private val berries = setOf("blackberry", "raspberry", "currant", "garden-strawberry")
    private const val BONA_UNIVERSAL = "https://www.bona-forte.ru/catalog/udobrenie-universalnoe-2-5-kg/"
    private const val ORGANIC_BERRIES = "https://organic-mix.ru/catalog/udobrenie-dlya-klubniki-i-yagodnykh-organik-miks-2-8-kg/"
    private const val ORGANIC_FLOWERS = "https://organic-mix.ru/catalog/udobrenie-dlya-roz-i-tsvetov-organik-miks-50-gr/"
    private const val GRANULES = "Распределить по поверхности почвы, аккуратно заделать рыхлением и полить. Не раствор для опрыскивания."

    private fun feed(id: String, name: String, manufacturer: String, crops: Set<String>, instruction: String, url: String) =
        ProgramProduct(id, name, manufacturer, "Подкормить растение",
            "Альтернатива по задаче питания; состав, расход и период действия отличаются.",
            instruction, url, crops, sections = setOf(ProductSection.FERTILIZER))

    val products = listOf(
        feed("organic_flowers", "Удобрение для роз и цветов, гранулы", "Органик Микс", setOf("peony", "rose"),
            "Взрослый пион — 100 г/куст, роза — 80 г/куст; саженцы — 50 г/куст. Интервал по карточке 30–45 дней. $GRANULES", ORGANIC_FLOWERS),
        feed("bona_flowers", "ПРЕМИУМ для роз и пионов, гранулы", "BonaForte", setOf("peony", "rose"),
            "Подкормка: 15–25 г/м². Всего 1–2 подкормки за сезон; не переносить частоту обычного растворимого удобрения. $GRANULES",
            "https://www.bona-forte.ru/catalog/udobrenie-granulirovannoe-prolongirovannoe-premium-dlya-roz-i-pionov-2-5-kg/"),
        feed("organic_hydrangea", "Удобрение для гортензий, гранулы", "Органик Микс", setOf("hydrangea"),
            "Для 2-летнего куста — 200 г, 3-летнего — 250–300 г, от 4 лет — 300 г. При посадке — 50–100 г. Интервал 30–45 дней, апрель–октябрь. $GRANULES",
            "https://organic-mix.ru/catalog/udobrenie-dlya-gortenziy-organik-miks-2-8-kg/"),
        feed("bona_hydrangea", "Для голубых гортензий, жидкое удобрение", "BonaForte", setOf("hydrangea"),
            "Только для голубых гортензий: корневая подкормка 80 мл/12 л, расход 10 л на 5 м². В открытом грунте раз в неделю, завершить за месяц до заморозков. После пересадки выждать 7–14 дней. Для других гортензий выбирайте другой продукт.",
            "https://www.bona-forte.ru/catalog/udobrenie-dlya-golubykh-gortenzij-1-5-l/"),
        ProgramProduct("agricola_hydrangea", "Агрикола Аква для гортензий", "Агрикола",
            "Подкормить гортензию", "Жидкая альтернатива гранулированному питанию; NPK 6:4:5, микроэлементы и гуматы.",
            "15 мл на 1 л воды. Подкормки с апреля по октябрь раз в 2 недели. Полив — до промачивания земляного кома; по листу — до смачивания. Выберите один способ и учитывайте уже внесённое питание.",
            "https://agricola.ru/products/agrikola-akva-dlya-gortenziy/", crops = setOf("hydrangea"),
            sections = setOf(ProductSection.FERTILIZER, ProductSection.TREATMENT)),
        feed("organic_berries", "Удобрение для клубники и ягодных, гранулы", "Органик Микс", berries,
            "Земляника — 20 г/куст; малина, ежевика и смородина — 100 г/куст. Интервал 30–45 дней, апрель–октябрь. $GRANULES", ORGANIC_BERRIES),
        feed("bona_berries", "Универсальное, гранулы — ягодные культуры", "BonaForte", berries,
            "Земляника: 6 г/м² в начале роста и после сбора урожая. Ягодные кустарники: 30 г/м², всего 2–3 подкормки в сезон; завершить за 14 дней до сбора. $GRANULES", BONA_UNIVERSAL),
        feed("organic_blueberry", "Удобрение для голубики, гранулы", "Органик Микс", setOf("blueberry"),
            "Двухлетний куст — 150 г; от 3 лет — 200 г; при посадке — 50–100 г. Интервал 30–45 дней, апрель–октябрь. $GRANULES",
            "https://organic-mix.ru/catalog/udobrenie-dlya-golubiki-organik-miks-50-gr/"),
        feed("bona_blueberry", "Для голубики и лесных ягод, гранулы", "BonaForte", setOf("blueberry"),
            "Подкормка голубики — 40–60 г/куст; всего 2–3 раза за сезон. $GRANULES",
            "https://www.bona-forte.ru/catalog/udobrenie-dlya-golubiki-i-lesnykh-yagod-2-5-kg/"),
        feed("organic_fruit", "Эликсир №1 для плодовых деревьев", "Органик Микс", setOf("apple", "pear"),
            "Для корневого полива 10 мл/л; на бедной почве 20 мл/л. Не чаще раза в неделю, март–октябрь. Взболтать перед разведением. Объём полива зависит от дерева и почвы; на странице нет фиксированной нормы на дерево.",
            "https://organic-mix.ru/catalog/eliksir-1-dlya-plodovykh-derevev-250-ml-new/"),
        feed("bona_fruit", "Универсальное, гранулы — плодовые деревья", "BonaForte", setOf("apple", "pear"),
            "Подкормка — 40 г/м² приствольного круга, всего 2–3 раза за сезон; завершить за 14 дней до сбора. $GRANULES", BONA_UNIVERSAL),
        feed("organic_potato", "Удобрение для картофеля, гранулы", "Органик Микс", setOf("potato"),
            "При окучивании — 40–60 г/куст. Для посадки предусмотрена другая норма: 40 г/лунку. Сезон апрель–август. Учитывайте уже внесённое при посадке питание. $GRANULES",
            "https://organic-mix.ru/catalog/udobrenie-organik-miks-dlya-kartofelya-850g/"),
        feed("bona_potato", "Универсальное, гранулы — картофель", "BonaForte", setOf("potato"),
            "Подкормка — 15 г/м²; 1–2 раза с интервалом 10–15 дней, только до начала цветения. $GRANULES", BONA_UNIVERSAL),
        feed("bona_vegetables", "Универсальное, гранулы — томаты и огурцы", "BonaForte", setOf("tomato", "cucumber"),
            "Подкормка — 15 г/м², начиная через 2 недели после высадки. 1–4 раза с интервалом 10–15 дней, завершить за 14 дней до сбора. $GRANULES", BONA_UNIVERSAL),
        feed("organic_lawn", "Для газона Весна-Лето, гранулы", "Органик Микс", setOf("lawn"),
            "Подкормка растущего газона — 100 г/м². Рассыпать равномерно и полить. Интервал 30–40 дней, март–сентябрь.",
            "https://organic-mix.ru/catalog/udobrenie-dlya-gazona-organik-miks-7-kg-vedro/"),
        feed("bona_lawn", "Универсальное, гранулы — газон", "BonaForte", setOf("lawn"),
            "Подкормка — 30 г/м² после скашивания. Равномерно распределить и полить. По карточке не менее двух подкормок за сезон; частоту и уже внесённое питание сверьте с упаковкой.", BONA_UNIVERSAL),
        feed("organic_lawn_autumn", "Для газона Осень, гранулы", "Органик Микс", setOf("lawn"),
            "100–150 г/м² для всех типов газона. Внести равномерно и полить; сезон применения и предельную кратность уточнить по упаковке.",
            "https://organic-mix.ru/catalog/udobrenie-dlya-gazona-osen-organik-miks-25-kg-/"),
        feed("bona_lawn_autumn", "Газонное лето-осень, гранулы", "BonaForte", setOf("lawn"),
            "10–15 г/м² после скашивания. Последнюю подкормку проводить не позднее середины сентября, с учётом местных заморозков. Равномерно распределить по газону и полить.",
            "https://www.bona-forte.ru/catalog/udobrenie-gazonnoe-leto-osen-5-kg/"),
        ProgramProduct("reanimator_lawn", "Фитоспорин-М РеаниматоР — газон", "БашИнком",
            "Обработать газон при подтверждённой грибной болезни", "Производитель рекомендует РеаниматоР для газона при появлении пятен; сначала исключите засуху и нарушение питания.",
            "Среднее поражение: 1 часть препарата на 20 частей воды; сильное: 1 на 2. Расход раствора 1 л/10 м². Степень поражения и повторения определяйте по инструкции. Рекомендация для газона: https://www.bashinkom.ru/upload/tmp/iblock/f16/oopih6bt2bsv0bwuk96ekew0r60petjg/mgi_05_2026_1_36_compressed.pdf (стр. 24).",
            "https://www.bashinkom.ru/products/ojz/FitosporinMReanimatoR02l", crops = setOf("lawn"),
            problems = setOf("leaf_spot"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("fitoverm_potato", "Фитоверм 0,2%, КЭ — картофель", "Фармбиомед",
            "Обработать картофель от колорадского жука", "Аверсектин С, 2 г/л. Не использовать норму для форм 1% или 5%.",
            "1 мл/л; до 4 л/100 м². Ожидание 1 сутки, максимум 3 обработки. Выход на работы через сутки.",
            "https://pharmbiomed.ru/product/fitoverm-02-ke", crops = setOf("potato"),
            problems = setOf("colorado_beetle"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("fitoverm_currant", "Фитоверм 0,2%, КЭ — смородина", "Фармбиомед",
            "Обработать смородину от клеща", "Аверсектин С, 2 г/л. Только соответствующий вредитель, не универсальное средство от всех насекомых.",
            "2 мл/л; до 1 л/куст. Ожидание 3 суток, максимум 2 обработки. Выход на работы через сутки.",
            "https://pharmbiomed.ru/product/fitoverm-02-ke", crops = setOf("currant"),
            problems = setOf("spider_mite"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("fitoverm_apple", "Фитоверм 0,2%, КЭ — яблонная плодожорка", "Фармбиомед",
            "Обработать яблоню от плодожорки", "Аверсектин С, 2 г/л. Не переносить этот регламент на грушу.",
            "2 мл/л; до 5 л/дерево. Ожидание 3 суток, максимум 1 обработка. Выход на работы через сутки.",
            "https://pharmbiomed.ru/product/fitoverm-02-ke", crops = setOf("apple"),
            problems = setOf("codling_moth"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("fitoverm_flowers", "Фитоверм 0,2%, КЭ — цветы открытого грунта", "Фармбиомед",
            "Обработать цветы от обнаруженного вредителя", "Аверсектин С, 2 г/л; нормы различаются по вредителю.",
            "Паутинный клещ: 2 мл/л; тля: 8 мл/л; трипсы: 10 мл/л. До 10 л/100 м². Предельную кратность и сроки выхода проверьте в инструкции своей фасовки.",
            "https://pharmbiomed.ru/product/fitoverm-02-ke", crops = setOf("peony", "rose"),
            problems = setOf("spider_mite", "aphids", "thrips"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("chistotsvet_flowers", "Чистоцвет, КЭ — цветочные культуры", "Август",
            "Обработать цветы от подтверждённой болезни", "Дифеноконазол 250 г/л; собственные нормы для каждой болезни.",
            "Мучнистая роса: 2 мл/3 л; серая гниль и пятнистости: 4 мл/3 л. Расход 3 л/100 м². Интервал 14 дней; максимум 2 обработки от росы/гнили, 4 от пятнистостей. Не опрыскивать раскрытые цветки.",
            "https://dacha.avgust.com/catalog/chistotsvet/", crops = setOf("peony", "rose"),
            problems = setOf("powdery_mildew", "gray_mold", "leaf_spot"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("chistotsvet_shrubs", "Чистоцвет, КЭ — декоративные кустарники", "Август",
            "Обработать кустарник от подтверждённой болезни", "Для декоративных кустарников; расход отличается от цветочных культур.",
            "Мучнистая роса: 2 мл/10 л; пятнистости: 4 мл/10 л. Расход 0,5–1 л/куст. Интервал 14 дней, максимум 2 обработки от росы и 4 от пятнистостей. Регламент этой строки не включает серую гниль.",
            "https://dacha.avgust.com/catalog/chistotsvet/", crops = setOf("hydrangea"),
            problems = setOf("powdery_mildew", "leaf_spot"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("rayek_fruit", "Раёк, КЭ — яблоня и груша", "Август",
            "Обработать дерево от парши или мучнистой росы", "Дифеноконазол 250 г/л.",
            "1,5–2 мл/10 л, расход 2–5 л/дерево. Регламент: зелёный конус, розовый бутон, затем после цветения через 10–15 дней. Максимум 4 обработки, ожидание 20 суток. Не переносить эту норму на картофель.",
            "https://dacha.avgust.com/catalog/rayek/", crops = setOf("apple", "pear"),
            problems = setOf("scab", "powdery_mildew"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("rayek_vegetables", "Раёк, КЭ — картофель и томат открытого грунта", "Август",
            "Обработать растение от альтернариоза", "Дифеноконазол 250 г/л. Только альтернариоз, не фитофтороз.",
            "4 мл/5 л, расход 5 л/100 м². Интервал 10–15 дней, максимум 2 обработки. Ожидание 28 суток.",
            "https://dacha.avgust.com/catalog/rayek/", crops = setOf("potato", "tomato"),
            problems = setOf("alternaria"), cultivationTypes = setOf(CultivationType.OPEN_GROUND)),
        ProgramProduct("ordan_potato", "Ордан, СП — картофель", "Август",
            "Обработать картофель от фитофтороза или альтернариоза", "Хлорокись меди и цимоксанил; только в пределах регламента раннего применения.",
            "25 г/5 л, расход 5 л/100 м²; первое применение до смыкания ботвы или не позже 2 суток после инфицирования. Интервал 7–14 дней, максимум 3 обработки; ожидание 20 суток. Запущенную болезнь эта схема не описывает.",
            "https://dacha.avgust.com/catalog/ordan/", crops = setOf("potato"),
            problems = setOf("late_blight", "alternaria"), cultivationTypes = setOf(CultivationType.OPEN_GROUND))
    ) + listOf(CultivationType.OPEN_GROUND, CultivationType.GREENHOUSE).flatMap { ground ->
        listOf("tomato", "cucumber").map { crop ->
            val greenhouse = ground == CultivationType.GREENHOUSE
            ProgramProduct("ordan_${crop}_${ground.name.lowercase()}", "Ордан, СП — ${if (crop == "tomato") "томат" else "огурец"}, ${ground.displayName.lowercase()}", "Август",
                "Провести обработку от подтверждённой болезни", "Норма привязана к культуре и грунту.",
                "25 г/${if (greenhouse) "8" else "5"} л воды, расход ${if (greenhouse) "8" else "5"} л/100 м². Первое применение в фазе 4–6 листьев или не позже 2 суток после инфицирования; интервал 7–10 дней, максимум 3 обработки. Ожидание ${if (greenhouse) "3" else "5"} суток.",
                "https://dacha.avgust.com/catalog/ordan/", crops = setOf(crop),
                problems = if (crop == "tomato") setOf("late_blight", "alternaria") else setOf("downy_mildew"), cultivationTypes = setOf(ground))
        }
    }

    fun feedingIds(crop: String): Set<String> = when (crop) {
        "peony", "rose" -> setOf("organic_flowers", "bona_flowers")
        in ExpandedCarePrograms.hydrangeaIds -> setOf("organic_hydrangea", "agricola_hydrangea", "bona_hydrangea")
        in berries -> setOf("organic_berries", "bona_berries")
        "blueberry" -> setOf("organic_blueberry", "bona_blueberry")
        "apple", "pear" -> setOf("bona_fruit", "organic_fruit")
        "potato" -> setOf("bona_potato", "organic_potato")
        "lawn" -> setOf("organic_lawn", "bona_lawn")
        else -> emptySet()
    }
}
