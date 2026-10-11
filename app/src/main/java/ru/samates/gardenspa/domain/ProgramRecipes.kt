package ru.samates.gardenspa.domain

/** Resolve catalog recipes without changing the saved contents of personal or edited work. */
object ProgramRecipes {
    fun recipeFor(stepId: String?, productName: String? = null): FolkFertilizerRecipe? {
        val step = stepId.orEmpty().substringBefore(":remaining:")
        val productId = step.substringAfter("~", "")
        val recipeId = when {
            step.startsWith("tank_mix:") -> step.removePrefix("tank_mix:").substringBefore("~")
            step.startsWith("folk_recipe:") -> step.removePrefix("folk_recipe:").substringBefore("~")
            productId.startsWith("mix_") -> productId.removePrefix("mix_")
            productId.isNotEmpty() -> null // A chosen replacement takes priority over the original phase's recipe.
            step == EradicationCare.AUTUMN_MIX_STEP -> EradicationCare.AUTUMN_RECIPE_ID
            step == EradicationCare.AUTUMN_IRON_STEP -> EradicationCare.IRON_RECIPE_ID
            step == SpringCarePrograms.GREEN_STEP -> FolkFertilizers.GREEN_CONE_ID
            step == SpringCarePrograms.AFTER_FLOWERING_STEP -> "btu_protection_tank_mix"
            else -> null
        }
        val recipes = FolkFertilizers.tankMixes + FolkFertilizers.recipes
        recipes.firstOrNull { it.id == recipeId }?.let { return it }
        val name = productName.orEmpty().trim().removeSuffix(" — Рецепт пользователя")
        return recipes.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: if (name == "Баковая смесь «Искореняющая»") EradicationCare.ironRecipe else null
    }

    /** Keep the reference's amounts and paragraphs, omitting only its repeated leading heading. */
    fun composition(recipe: FolkFertilizerRecipe): String {
        val text = recipe.ingredients.ifBlank { recipe.preparation }
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        return if (firstLine == recipe.name ||
            recipe.id == EradicationCare.IRON_RECIPE_ID && firstLine == "Осенняя искореняющая обработка")
            text.substringAfter('\n', "").trimStart()
        else text
    }
}
