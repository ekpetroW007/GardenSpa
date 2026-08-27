package ru.samates.gardenspa.domain

data class FolkFertilizerRecipe(
    val id: String,
    val name: String,
    val purpose: String,
    val ingredients: String,
    val preparation: String,
    val consumptionRate: String,
    val warning: String,
    val sourceName: String,
    val sourceUrl: String
) {
    fun purposeForDrug(): String = buildString {
        append(purpose)
        append(" Приготовление: ")
        append(preparation)
        if (warning.isNotBlank()) {
            append(" Важно: ")
            append(warning)
        }
    }
}

object FolkFertilizers {
    private const val RHS_HOMEMADE = "https://www.rhs.org.uk/advice/profile?PID=1208"
    private const val OSU_COMPOST = "https://extension.oregonstate.edu/catalog/em-9308-how-use-compost-gardens-landscapes"
    private const val OSU_ASH = "https://extension.oregonstate.edu/catalog/ec-1503-fertilizing-your-garden-vegetables-fruits-ornamentals"
    private const val UMN_CLIPPINGS = "https://extension.umn.edu/lawns-and-landscapes-minnesota/what-do-lawn-clippings"
    private const val OSU_MILK_SPRAY = "https://extension.oregonstate.edu/sites/extd8/files/documents/58526/october-22-chat.pdf"
    private const val WVU_POWDERY_MILDEW = "https://extension.wvu.edu/lawn-gardening-pests/plant-disease/fruit-vegetable-diseases/powdery-mildew"
    private const val RSC_LATE_BLIGHT = "https://old.rosselhoscenter.ru/index.php/otchjoty-80/17718-rekomendatsii-spetsialistov-po-borbe-s-fitoftoroj"
    private const val RSC_RUST = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/sibirskiy/omskaya-oblast/chto-delat-esli-poshla-rzhavchina-na-gorokhe/"
    private const val RSC_APHIDS = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/tsentralnyy-okrug/kaluzhskaya-oblast/tlya-na-vishne-effektivnye-mery-borby/"
    private const val RSC_CATERPILLARS = "https://old.rosselhoscenter.ru/index.php/otdel-zashchity-rastenij-9/29190-kak-uberech-pomidory-ot-gusenits-sovki"
    private const val MUSTARD_SCAB = "https://lenta.ru/articles/2025/04/17/kak-izbavitsya-ot-parshi/"
    private const val RSC_ASH_FRUIT_SET = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/privolzhskiy/chuvashskaya-respublika/zola-prostaya-i-dostupnaya-podkormka-s-otlichnym-rezultatom-/"

    val recipes = listOf(
        FolkFertilizerRecipe(
            id = "nettle_liquid",
            name = "Концентрат из крапивы",
            purpose = "Жидкая растительная подкормка для рассады, растений в контейнерах, овощных культур и декоративных однолетников.",
            ingredients = "Свежая крапива без семян, ёмкость с крышкой.",
            preparation = "Измельчить и плотно утрамбовать крапиву без добавления воды. Закрыть и проверять раз в неделю; слить тёмную жидкость после разложения массы.",
            consumptionRate = "1 часть концентрата на 20 частей воды; вносить под корень.",
            warning = "Собирать в перчатках. Не использовать как листовой спрей из-за риска вдыхания микроорганизмов.",
            sourceName = "Royal Horticultural Society",
            sourceUrl = RHS_HOMEMADE
        ),
        FolkFertilizerRecipe(
            id = "dried_green_feed",
            name = "Настой сухой зелёной массы",
            purpose = "Мягкая жидкая подкормка из высушенной травы или листьев.",
            ingredients = "Высушенная зелёная масса, вода, закрывающаяся ёмкость.",
            preparation = "Утрамбовать растительный материал и залить водой так, чтобы она полностью его покрыла. Держать под крышкой, пока жидкость не станет тёмной, затем процедить.",
            consumptionRate = "1 часть настоя на 10 частей воды; поливать под корень.",
            warning = "Не использовать растения с признаками болезни, семенами сорняков и одревесневшие побеги.",
            sourceName = "Royal Horticultural Society",
            sourceUrl = RHS_HOMEMADE
        ),
        FolkFertilizerRecipe(
            id = "finished_compost",
            name = "Внесение зрелого компоста",
            purpose = "Улучшение структуры почвы и постепенное пополнение органического вещества и питательных элементов.",
            ingredients = "Полностью созревший растительный компост.",
            preparation = "Разложить равномерным слоем по поверхности существующей грядки и аккуратно заделать в верхний слой почвы.",
            consumptionRate = "Слой 0,6–2,5 см один раз в год для существующих грядок.",
            warning = "Не использовать горячий или недозревший компост и не превышать норму: избыток тоже может повредить растениям.",
            sourceName = "Oregon State University Extension",
            sourceUrl = OSU_COMPOST
        ),
        FolkFertilizerRecipe(
            id = "wood_ash",
            name = "Древесная зола",
            purpose = "Источник калия и материал для снижения кислотности почвы.",
            ingredients = "Просеянная зола из чистой необработанной древесины.",
            preparation = "Равномерно рассыпать по влажной почве и слегка заделать, не оставляя куч.",
            consumptionRate = "Только по результатам анализа почвы: при дефиците калия — 50–75 г/м². Не применять при pH 7 и выше.",
            warning = "Не применять при pH 7 и выше, под голубику, рододендроны, азалии и картофель; не брать золу угля, мусора или окрашенной древесины.",
            sourceName = "Oregon State University Extension",
            sourceUrl = OSU_ASH
        ),
        FolkFertilizerRecipe(
            id = "grass_mulch",
            name = "Мульча из скошенной травы",
            purpose = "Постепенное питание почвы, удержание влаги и подавление сорняков.",
            ingredients = "Подсушенная трава с газона, который в последнее время не обрабатывали гербицидами.",
            preparation = "Подсушить траву и разложить тонким рыхлым слоем вокруг растений.",
            consumptionRate = "Слой 2,5–5 см за одно внесение.",
            warning = "Не использовать мокрую слежавшуюся траву или траву с газона, недавно обработанного гербицидами.",
            sourceName = "University of Minnesota Extension",
            sourceUrl = UMN_CLIPPINGS
        ),
        FolkFertilizerRecipe(
            id = "milk_powdery_mildew",
            name = "Молочный раствор от мучнистой росы",
            purpose = "Домашняя профилактическая обработка листьев против мучнистой росы. Эффективность может различаться в зависимости от растения и условий.",
            ingredients = "1 часть нежирного молока и 9 частей воды.",
            preparation = "Смешать молоко с водой непосредственно перед применением. Равномерно опрыскать листья с обеих сторон.",
            consumptionRate = "До равномерного смачивания листьев без стекания. Начинать обработку при первых признаках заболевания.",
            warning = "Сначала проверить раствор на нескольких листьях и оценить их состояние через 1–2 дня. Не хранить готовую смесь.",
            sourceName = "Oregon State University Extension",
            sourceUrl = OSU_MILK_SPRAY
        ),
        FolkFertilizerRecipe(
            id = "baking_soda_powdery_mildew",
            name = "Содовый раствор от мучнистой росы",
            purpose = "Домашняя обработка против мучнистой росы, создающая на поверхности листа неблагоприятную для грибка среду.",
            ingredients = "1 чайная ложка пищевой соды и 0,95 л воды.",
            preparation = "Полностью растворить соду в воде и использовать свежий раствор. Опрыскать поражаемые листья с обеих сторон.",
            consumptionRate = "До равномерного смачивания листьев без стекания. Применять на ранней стадии заболевания.",
            warning = "Сначала проверить на небольшом участке растения. Не превышать концентрацию и не допускать регулярного попадания раствора в почву.",
            sourceName = "West Virginia University Extension",
            sourceUrl = WVU_POWDERY_MILDEW
        ),
        FolkFertilizerRecipe(
            id = "milk_iodine_late_blight",
            name = "Молоко с йодом от фитофторы",
            purpose = "Народная профилактическая обработка томатов до появления признаков фитофторы.",
            ingredients = "1 л обезжиренного молока, 15 капель аптечного йода и 9 л воды.",
            preparation = "Влить молоко в воду, добавить йод и тщательно перемешать. Использовать раствор сразу после приготовления.",
            consumptionRate = "Равномерно опрыскивать листья 4–5 раз с интервалом 10 дней до появления заболевания.",
            warning = "Это профилактический народный рецепт, а не лечение развившейся фитофторы. Сначала проверить раствор на нескольких листьях и не увеличивать количество йода.",
            sourceName = "Россельхозцентр",
            sourceUrl = RSC_LATE_BLIGHT
        ),
        FolkFertilizerRecipe(
            id = "ash_spray_rust",
            name = "Зольный настой от ржавчины",
            purpose = "Народная обработка растений при первых признаках ржавчины.",
            ingredients = "1 столовая ложка древесной золы, 2 стакана тёплой воды и немного жидкого мыла.",
            preparation = "Смешать золу с водой, настоять сутки, процедить и добавить немного жидкого мыла для прилипания.",
            consumptionRate = "Опрыскивать листья два раза в неделю, предварительно проверив настой на небольшом участке.",
            warning = "Использовать только золу чистой необработанной древесины. При сильном распространении ржавчины народного настоя может быть недостаточно.",
            sourceName = "Россельхозцентр",
            sourceUrl = RSC_RUST
        ),
        FolkFertilizerRecipe(
            id = "ash_soap_aphids",
            name = "Зольно-мыльный настой от тли",
            purpose = "Народная контактная обработка небольших колоний тли на листьях и молодых побегах.",
            ingredients = "1 стакан древесной золы, 40 г хозяйственного мыла и 10 л воды.",
            preparation = "Залить золу водой, дать настояться, процедить и растворить в настое измельчённое хозяйственное мыло.",
            consumptionRate = "Смачивать места скопления тли, включая нижнюю сторону листьев; при необходимости повторить после осмотра растения.",
            warning = "Бытовые мыльные растворы могут обжечь чувствительные листья. Сначала обработать небольшой участок и не опрыскивать цветки.",
            sourceName = "Россельхозцентр",
            sourceUrl = RSC_APHIDS
        ),
        FolkFertilizerRecipe(
            id = "mustard_caterpillars",
            name = "Горчичный настой от гусениц",
            purpose = "Народный раствор для отпугивания гусениц совки на томатах и других огородных культурах.",
            ingredients = "100 г сухого горчичного порошка и 10 л кипятка.",
            preparation = "Всыпать горчицу в кипяток, тщательно перемешать и оставить на 48 часов, затем процедить.",
            consumptionRate = "Опрыскивать заражённые листья до равномерного смачивания утром, вечером или в пасмурную погоду.",
            warning = "Не превышать концентрацию. Сначала проверить раствор на одном растении и не использовать по цветкам.",
            sourceName = "Россельхозцентр",
            sourceUrl = RSC_CATERPILLARS
        ),
        FolkFertilizerRecipe(
            id = "mustard_apple_scab",
            name = "Горчичный раствор от парши",
            purpose = "Народная обработка яблони или груши при первых признаках парши.",
            ingredients = "4 столовые ложки сухого горчичного порошка и 10 л тёплой воды.",
            preparation = "Сначала размешать горчицу в небольшом количестве тёплой воды, затем довести общий объём раствора до 10 л.",
            consumptionRate = "Равномерно опрыскать листья и ветви; начинать с небольшой части кроны и наблюдать за реакцией растения.",
            warning = "Народный раствор не восстанавливает уже повреждённые плоды. Не применять в жаркую солнечную погоду и не увеличивать дозировку.",
            sourceName = "Рекомендация садовода, опубликованная Lenta.ru",
            sourceUrl = MUSTARD_SCAB
        ),
        FolkFertilizerRecipe(
            id = "ash_feed_fruit_set",
            name = "Зольный настой для сохранения завязей",
            purpose = "Народная калийная подкормка огурцов и томатов в период образования и налива завязей.",
            ingredients = "1 стакан древесной золы и 10 л воды.",
            preparation = "Размешать золу в воде и внести настой под корень по предварительно увлажнённой почве.",
            consumptionRate = "Применять не чаще одного раза в 1–2 недели, наблюдая за состоянием растений.",
            warning = "Использовать только золу чистой древесины и не смешивать её с азотными удобрениями. Опадение завязей также бывает из-за жары, холода, нехватки света, неправильного полива или плохого опыления.",
            sourceName = "Россельхозцентр",
            sourceUrl = RSC_ASH_FRUIT_SET
        ),
        FolkFertilizerRecipe(
            id = "magic_plant_drink_tank_mix",
            name = "Баковая смесь «Волшебный напиток для растений»",
            purpose = "Баковая смесь для комплексной обработки растений.",
            ingredients = "На 10 л воды: Алирин-Б — 4 таблетки; Гамаир — 4 таблетки; Циркон — 2 мл; Силиплант — 20 мл; Фитоверм — 20 мл.",
            preparation = "Добавить перечисленные компоненты в 10 л воды, тщательно перемешать и использовать сразу после приготовления.",
            consumptionRate = "Расходовать согласно норме для обрабатываемой культуры и инструкциям применяемых препаратов.",
            warning = "",
            sourceName = "Рецепт пользователя",
            sourceUrl = ""
        )
    )
}
