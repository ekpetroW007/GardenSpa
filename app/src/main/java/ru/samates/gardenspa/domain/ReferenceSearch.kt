package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.DrugEntity

data class ReferenceSearchResult(val key: String, val title: String, val category: String, val screen: String, val details: String)

fun searchReference(query: String, personal: List<DrugEntity>): List<ReferenceSearchResult> {
    fun normalized(text: String) = text.lowercase().replace('ё', 'е')
    val terms = normalized(query).trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (terms.isEmpty()) return emptyList()
    val entries = ProductSection.entries.flatMap { section ->
        ProgramReferenceCatalog.entries(section).map { product ->
            ReferenceSearchResult("${section.name}:${product.id}", product.displayName, section.title,
                if (section == ProductSection.TREATMENT) "Препараты" else "Удобрения", "${product.purpose}\n${product.instruction}")
        }
    } + (FolkFertilizers.recipes + FolkFertilizers.tankMixes).map { recipe ->
        ReferenceSearchResult("recipe:${recipe.id}", recipe.name, if (recipe.isTankMix) "Баковые смеси" else "Народные рецепты",
            if (recipe.isTankMix) "Баковые смеси" else "Рецепты", "${recipe.purposeForDrug()}\n${recipe.consumptionRate}")
    } + personal.map { ReferenceSearchResult("my:${it.id}", it.name, "Мои средства", "Мои средства", it.careNote) }
    return entries.filter { entry ->
        val text = normalized("${entry.title} ${entry.category} ${entry.details}")
        terms.all(text::contains)
    }.sortedByDescending { entry -> terms.all(normalized(entry.title)::contains) }
}
