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
    private const val RSC_LATE_BLIGHT = "https://old.rosselhoscenter.ru/index.php/otchjoty-80/17718-rekomendatsii-spetsialistov-po-borbe-s-fitoftoroj"
    private const val RSC_RUST = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/sibirskiy/omskaya-oblast/chto-delat-esli-poshla-rzhavchina-na-gorokhe/"
    private const val RSC_APHIDS = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/tsentralnyy-okrug/kaluzhskaya-oblast/tlya-na-vishne-effektivnye-mery-borby/"
    private const val RSC_CATERPILLARS = "https://old.rosselhoscenter.ru/index.php/otdel-zashchity-rastenij-9/29190-kak-uberech-pomidory-ot-gusenits-sovki"
    private const val MUSTARD_SCAB = "https://lenta.ru/articles/2025/04/17/kak-izbavitsya-ot-parshi/"
    private const val RSC_ASH_FRUIT_SET = "https://rosselhoscenter.ru/ob-uchrezhdenii/filialy/privolzhskiy/chuvashskaya-respublika/zola-prostaya-i-dostupnaya-podkormka-s-otlichnym-rezultatom-/"

    val recipes = listOf(
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
