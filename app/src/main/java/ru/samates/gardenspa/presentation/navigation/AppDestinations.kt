package ru.samates.gardenspa.presentation.navigation

object AppDestinations {
    const val REGISTRATION_ROUTE = "registration"
    const val MAINSCREEN_ROUTE = "mainScreen"

    const val DRUG_ADD_ROUTE = "drugsAddScreen"

    const val GARDEN_ADD = "gardenAddScreen"

    const val DRUG_INFO = "drugInfoScreen"

    const val PLANT_ADD = "plantAddScreen/{selectedDate}"
    const val PLANT_DETAILS = "plantDetails/{plantId}"
    const val ALL_PLANTS = "allPlants"

    fun plantAdd(selectedDate: String) = "plantAddScreen/$selectedDate"
    fun plantDetails(plantId: Int) = "plantDetails/$plantId"
}
