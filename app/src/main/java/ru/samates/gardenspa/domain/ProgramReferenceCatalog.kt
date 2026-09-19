package ru.samates.gardenspa.domain

/** A shared reference view of program products; no copies are seeded into the user's database. */
object ProgramReferenceCatalog {
    fun allEntries(): List<ProgramProduct> = ProductSection.entries.flatMap { entries(it) }.distinctBy { it.id }

    private val fertilizerSteps = setOf("gumi_omi_planting", "gumi_omi_feeding", "gumi_omi_7_8_leaves",
        "fitosporin_roots", "kornesil_planting")
    private val sprayingSteps = setOf("fitosporin_spraying", "borogum_flowering", "bogaty_flowering")

    val products: List<ProgramProduct> by lazy {
        val originalProducts = PlantCareCatalog.all().filter { it.id in setOf("tomato", "cucumber") }.flatMap { crop ->
            crop.steps.mapNotNull { step ->
                val description = step.productDescription ?: return@mapNotNull null
                val sections = buildSet {
                    if (step.id in fertilizerSteps) add(ProductSection.FERTILIZER)
                    if (step.id in sprayingSteps) add(ProductSection.TREATMENT)
                }
                ProgramProduct("original_${crop.id}_${step.id}", description.substringBefore(" — "), "БашИнком",
                    step.title, "${crop.canonicalName}: ${step.title}", step.note,
                    Regex("https://\\S+").find(step.note)?.value.orEmpty(), crops = setOf(crop.id), sections = sections)
            }
        }
        // The cucumber mixture contains two products; both must also be individually discoverable.
        val cucumberMixture = originalProducts.first { it.id == "original_cucumber_gumi_omi_7_8_leaves" }
        val components = listOf(
            cucumberMixture.copy(id = "original_cucumber_gumi_omi_component", name = "Гуми-Оми Огурец, Кабачок, Бахчевые",
                sections = setOf(ProductSection.FERTILIZER)),
            cucumberMixture.copy(id = "original_cucumber_fitosporin_as", name = "Фитоспорин-АС",
                sections = setOf(ProductSection.FERTILIZER))
        )
        ProgramProductCatalog.products + originalProducts + components
    }

    fun entries(section: ProductSection, query: String = ""): List<ProgramProduct> = products
        .filter { section in it.sections }
        .groupBy { it.displayName }
        .map { (_, variants) ->
            variants.first().copy(
                crops = variants.flatMap { it.crops }.toSet(),
                purpose = variants.map { it.purpose }.distinct().joinToString("\n"),
                instruction = variants.map { "${it.taskTitle}\n${it.instruction}\nИсточник: ${it.sourceUrl}" +
                    (it.retailUrl?.let { url -> "\nВ каталоге Лемана Про: $url" } ?: "") }
                    .distinct().joinToString("\n\n")
            )
        }
        .filter { product ->
            query.isBlank() || listOf(product.name, product.manufacturer, product.purpose,
                product.crops.joinToString { crop -> PlantCareCatalog.all().firstOrNull { it.id == crop }?.canonicalName ?: crop })
                .any { it.contains(query.trim(), ignoreCase = true) }
        }
        .sortedBy { it.displayName }
}
