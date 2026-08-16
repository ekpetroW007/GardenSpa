package ru.samates.gardenspa.presentation.navigation

object AppDestinations {
    const val REGISTRATION_ROUTE = "registration"
    const val MAINSCREEN_ROUTE = "mainScreen"

    const val DRUG_ADD_ROUTE = "drugsAddScreen"
    const val DRUG_EDIT_ROUTE = "drugEditScreen/{drugId}"

    const val GARDEN_ADD = "gardenAddScreen"

    const val DRUG_INFO = "drugInfoScreen"

    const val PLANT_ADD = "plantAddScreen/{selectedDate}?gardenId={gardenId}"
    const val PLANT_EDIT = "plantEditScreen/{plantId}"
    const val PLANT_DETAILS = "plantDetails/{plantId}"
    const val ALL_PLANTS = "allPlants"

    fun plantAdd(selectedDate: String, gardenId: Int? = null): String = buildString {
        append("plantAddScreen/$selectedDate")
        gardenId?.let { append("?gardenId=$it") }
    }
    fun plantEdit(plantId: Int) = "plantEditScreen/$plantId"
    fun plantDetails(plantId: Int) = "plantDetails/$plantId"
    fun drugEdit(drugId: Int) = "drugEditScreen/$drugId"
}
