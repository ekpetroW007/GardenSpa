package ru.samates.gardenspa.domain

data class PlantNameSuggestion(
    val canonicalName: String,
    val careTemplateId: String?
)

/** Видовые названия и проверенные группы ухода для растений с готовыми программами. */
object PlantNameCatalog {
    private val plantNames = setOf(
        "Абрикос", "Абутилон", "Агастахе", "Агератум", "Агростемма", "Азарина", "Айва", "Аквилегия",
        "Акроклинум", "Алиссум", "Алыча", "Амарант", "Анис", "Арабис", "Арбуз", "Артишок", "Аспарагус",
        "Астра", "Базилик", "Баклажан", "Бакопа", "Бальзамин", "Бамия", "Барбарис", "Барвинок", "Бархатцы",
        "Бегония", "Бобы", "Боярышник", "Брахикома", "Броваллия", "Бругмансия", "Брюква", "Бузульник",
        "Василёк", "Ваточник", "Вербена", "Вероника", "Вигна", "Виноград", "Виола", "Вишня", "Вьюнок",
        "Газания", "Гайлардия", "Гвоздика", "Гейхера", "Гелиопсис", "Гелихризум", "Георгина", "Гербера",
        "Гибискус", "Гиацинтовые бобы", "Гипестис", "Гипсофила", "Глоксиния", "Годеция", "Голубика",
        "Гомфрена", "Горох", "Горчица салатная", "Гортензия", "Гравилат", "Груша", "Дайкон", "Дельфиниум",
        "Дихондра", "Доротеантус", "Душевик", "Душистый горошек", "Дыня", "Дурман", "Ежевика", "Жимолость",
        "Земляника", "Змееголовник", "Иберис", "Ипомея", "Ирга", "Иссоп", "Кабачок", "Кактус", "Календула",
        "Калибрахоа", "Калина", "Кальцеолярия", "Камнеломка", "Капуста", "Капуста декоративная", "Картофель",
        "Кассия", "Катарантус", "Кларкия", "Клематис", "Клещевина", "Клубника", "Кобея", "Колеус",
        "Колокольчик", "Кореопсис", "Кориандр", "Космея", "Котовник", "Кохия", "Красная смородина", "Кроссандра",
        "Крыжовник", "Кукуруза", "Лаванда", "Лаватера", "Лапчатка", "Левкой", "Лён крупноцветковый", "Лиатрис",
        "Лихнис", "Лобелия", "Лук", "Люпин", "Лютик", "Любисток", "Львиный зев", "Майоран", "Малина",
        "Мальва", "Маргаритка", "Маттиола", "Мелисса", "Микрозелень", "Мимулюс", "Мирабилис", "Монарда",
        "Морковь", "Мята", "Наперстянка", "Настурция", "Незабудка", "Немезия", "Немофила", "Облепиха",
        "Обриета", "Огурец", "Огуречная трава", "Орегано", "Остеоспермум", "Очиток", "Папавер", "Паслён",
        "Пастернак", "Пассифлора", "Патиссон", "Пеларгония", "Пентас", "Перец", "Перец декоративный", "Петрушка",
        "Петуния", "Петхоа", "Пион", "Платикодон", "Подсолнечник", "Портулак", "Примула", "Редис", "Редька",
        "Репа", "Рододендрон", "Роза", "Розмарин", "Ромашка", "Руккола", "Рудбекия", "Рябина", "Салат",
        "Сальвия", "Сальпиглоссис", "Санвиталия", "Свёкла", "Сельдерей", "Скабиоза", "Слива", "Смородина",
        "Спаржа", "Статица", "Суккулент", "Табак", "Табак душистый", "Тимьян", "Тмин", "Томат", "Торения",
        "Тунбергия", "Тыква", "Тысячелистник", "Укроп", "Фасоль", "Фенхель", "Фиалка", "Физалис", "Флокс",
        "Хоста", "Хризантема", "Целозия", "Цикламен", "Цинерария", "Цинния", "Чабер", "Черешня", "Чёрная смородина",
        "Шалфей", "Шиповник", "Шпинат", "Щавель", "Эвкалипт", "Экзакум", "Энотера", "Эстрагон", "Эустома",
        "Эшшольция", "Яблоня", "Ясколка"
    )

    private val careTemplates = PlantCareCatalog.all()
    private val suggestions = (plantNames.filter { name -> careTemplates.none { it.matches(name) } }.map { PlantNameSuggestion(it, null) } +
        careTemplates.map { PlantNameSuggestion(it.canonicalName, it.id) })
        .sortedBy { it.canonicalName }

    fun suggestions(userInput: String, limit: Int = 8): List<PlantNameSuggestion> {
        val normalized = normalizePlantName(userInput)
        if (normalized.isBlank()) return emptyList()
        return suggestions.asSequence()
            .filter { suggestion ->
                normalizePlantName(suggestion.canonicalName).startsWith(normalized) ||
                    suggestion.careTemplateId?.let(PlantCareCatalog::findById)?.aliases?.any { normalizePlantName(it).startsWith(normalized) } == true
            }
            .sortedWith(compareByDescending<PlantNameSuggestion> { normalizePlantName(it.canonicalName).startsWith(normalized) }.thenBy { it.canonicalName })
            .take(limit)
            .toList()
    }

    fun findExact(userInput: String): PlantNameSuggestion? {
        val normalized = normalizePlantName(userInput)
        return suggestions.firstOrNull { normalizePlantName(it.canonicalName) == normalized }
            ?: PlantCareCatalog.find(userInput)?.let { PlantNameSuggestion(it.canonicalName, it.id) }
    }

    fun all(): List<PlantNameSuggestion> = suggestions

    private fun PlantCareTemplate.matches(name: String): Boolean {
        val normalized = normalizePlantName(name)
        return normalizePlantName(canonicalName) == normalized || aliases.any { normalizePlantName(it) == normalized }
    }
}
