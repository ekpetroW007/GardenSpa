package ru.samates.gardenspa.domain

/** Classic fertilizer names describe a family, not a universal formula or dose. Checked 2026-09-19. */
object MineralProgramProducts {
    private val vegetables = setOf("tomato", "cucumber", "sweet-pepper", "eggplant", "zucchini", "pumpkin", "cabbage", "carrot", "beet", "onion", "garlic", "potato")
    private val berries = setOf("blackberry", "raspberry", "currant", "garden-strawberry")
    private val fruit = setOf("apple", "pear")
    private val crops = vegetables + berries + fruit + setOf("rose", "peony")
    private const val BASE = "https://bhzshop.ru/catalog/traditsionnye-udobreniya/"
    private const val NOTICE = "Выберите одну схему питания с учётом состояния почвы и уже внесённых удобрений. Простое удобрение восполняет отдельные элементы и не равно полному комплексу. Нормы относятся только к указанному продукту; на упаковке другой марки состав может отличаться."

    private fun product(id: String, name: String, composition: String, purpose: String, instruction: String,
        path: String, supported: Set<String> = crops) = ProgramProduct(
        id, name, "Буйский химический завод", "Корневая подкормка: $name",
        "Состав: $composition. $purpose", "$instruction\n$NOTICE", BASE + path,
        crops = supported, sections = setOf(ProductSection.FERTILIZER))

    val products = listOf(
        ProgramProduct("mineral_azofoska_agricola", "Азофоска (нитроаммофоска) 16:16:16, Агрикола", "Агрикола",
            "Внести азофоску в почву", "NPK 16:16:16. Техноэкспорт / Агрикола; комплексное минеральное питание.",
            "Для подготовки почвы: 20–25 г/м² на окультуренной, 25–30 г/м² на слабоокультуренной. Картофель: подкормка 25–30 г/м² в фазе полных всходов. Земляника: 40–60 г/м² весной и после сбора. Ягодные кустарники: 45–60 г/м² весной, после цветения и после сбора; деревья: 40–50 г/м² весной. Для овощных подкормок уточните норму упаковки, не заменяйте её нормой подготовки почвы. $NOTICE",
            "https://www.technoexport.ru/catalog/household/fertilizer/granular-fertilizer/azofoska/",
            crops = crops, sections = setOf(ProductSection.FERTILIZER)),
        product("mineral_azofoska", "Азофоска (нитроаммофоска) 16:16:16",
            "N 16%, P₂O₅ 16%, K₂O 16%, MgO 0,5%, S 1,5%", "Комплексное питание азотом, фосфором и калием.",
            "Овощи — 25–30 г/м², две подкормки за сезон, последняя за 2–3 недели до сбора. Картофель — 25–30 г/м² в бутонизацию и цветение. Плодовые деревья — 30–40 г/м² приствольного круга весной и при формировании плодов. Ягодные кустарники — 25–30 г/м² в те же фазы; земляника — 20–25 г/м² до цветения и после сбора. Для цветов уточните норму подкормки по упаковке. Не переносите азотную схему на подготовку многолетников к зиме.", "azofoska-/"),
        product("mineral_diammofoska", "Диаммофоска 9:25:25", "N 9%, P₂O₅ 25%, K₂O 25%; содержит хлор",
            "Комплекс с повышенной долей фосфора и калия; отличается от азофоски и от марок 10:26:26.",
            "Овощи — 20–25 г/м², 2–3 раза с интервалом 10–15 дней; завершить за 2–3 недели до сбора. Картофель — 20–25 г/м² в бутонизацию. Деревья весной — 25–30 г/м² приствольного круга, ягодные кустарники — 20–25 г/м². Учитывайте наличие хлора и чувствительность культуры: для чувствительных посадок выберите бесхлорный вариант.", "diammofoska/", vegetables + fruit + setOf("currant")),
        product("mineral_urea", "Карбамид (мочевина)", "N 46,2%", "Азотное питание в начале роста.",
            "Применяют весной и в первой половине вегетации, завершая не позднее двух недель до сбора. Для корневой подкормки на странице приведены 5–10 г/м² либо раствор 20–30 г/10 л; конкретную строку для своей культуры и расход воды проверьте на упаковке. Заделать в почву и полить. Не использовать как осеннюю замену фосфорно-калийному питанию.", "karbamid-mochevina/"),
        product("mineral_superphosphate", "Суперфосфат, марка БХЗ", "N 5%, P₂O₅ 26%, CaO 7%, MgO 8%, S 3%",
            "Фосфорное питание с кальцием, магнием и серой. Это не двойной суперфосфат 46–49%.",
            "Корневая подкормка — 15–20 г/м², заделать при рыхлении и полить. По инструкции 2–3 раза за вегетацию с интервалом 3–4 недели с учётом уже внесённого фосфора. Для подготовки почвы применяется другая норма — 20–40 г/м².", "superfosfat-guminizirovannyy/"),
        product("mineral_potassium_sulfate", "Сульфат калия (калий сернокислый)", "K₂O 50%, S 17%",
            "Калийное питание с серой без добавления азота.",
            "Подкормка плодовых деревьев — 20–50 г/м² приствольного круга, ягодных кустарников — 15–20 г/м², земляники — 10–15 г/м². Для овощей и цветов норму именно подкормки сверяйте на упаковке: на странице также указаны отдельные нормы для перекопки почвы. Вносить в почву, не переносить дозу сухого внесения в раствор для опрыскивания.", "sulfat-kaliya/"),
        product("mineral_kalimagnesia", "Калимагнезия", "K₂O 32%, MgO 12%, S 10,5%",
            "Бесхлорное калийно-магниевое питание.",
            "Вносится сухой в почву с последующим поливом. Для подготовки почвы весной или осенью — 30–40 г/м²; для подкормок указаны отдельные нормы 15–30 г/м², выбирайте строку своей культуры на упаковке. Дозировка основного внесения не является дозой каждой подкормки.", "kalimagneziya/"),
        ProgramProduct("mineral_monopotassium", "Монокалийфосфат (монофосфат калия), марка БХЗ", "Буйский химический завод",
            "Подкормить монокалийфосфатом", "Состав: P₂O₅ 50%, K₂O 33%. Фосфорно-калийное питание без азота; состав отличается от марок 52:34.",
            "Корневая подкормка овощей — 10–20 г/10 л, 2–3 раза с интервалом 10–15 дней; расход по инструкции 4–10 л/м². Плодовые деревья: 10–15 г/10 л после цветения, через 15–20 дней и после урожая, расход 10–20 л/растение по его размеру. Для ягодных кустарников, земляники, цветов, газона и внекорневого применения сверяйте отдельную строку этикетки; корневую норму не переносите на листья. $NOTICE",
            "https://bhzshop.ru/catalog/kompleksnye-udobreniya/monokaliyfosfat/", crops = crops + "lawn",
            sections = setOf(ProductSection.FERTILIZER, ProductSection.TREATMENT))
    )

    fun forStep(programId: String, stepId: String): Set<String> {
        val ids = when (stepId) {
            "mineral_nutrition", "nutrition", "nutrition_review", "gumi_omi_feeding", "gumi_omi_7_8_leaves" -> products.map { it.id }.toSet()
            "gumi_omi_planting" -> setOf("mineral_azofoska", "mineral_azofoska_agricola", "mineral_superphosphate")
            "lawn_autumn_nutrition" -> setOf("mineral_monopotassium")
            else -> emptySet()
        }
        return products.filter { it.id in ids && programId in it.crops }.map { it.id }.toSet()
    }
}
