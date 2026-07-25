package com.example.myapplication.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BookeeperApp
import com.example.myapplication.R
import com.example.myapplication.data.database.entity.PlantEntity
import com.example.myapplication.presentation.navigation.AppDestinations
import com.example.myapplication.viewmodel.PlantsViewmodel
import com.example.myapplication.viewmodel.PlantsViewmodelFactory
import com.example.myapplication.viewmodel.TasksViewmodel
import com.example.myapplication.viewmodel.TasksViewmodelFactory
import io.github.boguszpawlowski.composecalendar.SelectableWeekCalendar
import io.github.boguszpawlowski.composecalendar.WeekCalendarState
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.rememberSelectableWeekCalendarState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.DayOfWeek
import java.time.LocalDate

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Calendar(innerPadding: PaddingValues, navController: NavController) {
    val weekState: WeekCalendarState<DynamicSelectionState> = rememberSelectableWeekCalendarState()
    weekState.weekState.currentWeek

    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelPlantsFactory = PlantsViewmodelFactory(application.repository)
    val plantsViewmodel: PlantsViewmodel = viewModel(factory = viewmodelPlantsFactory)
    val plantList by plantsViewmodel.plants.collectAsState()

    var filteredPlantList by remember { mutableStateOf<List<PlantEntity>>(emptyList()) }


    LaunchedEffect(key1 = weekState.weekState.currentWeek, key2 = plantList) {
        filteredPlantList = plantList.filter { plant ->
            Log.d("filtration", "plant = $plant")
            val cD: LocalDate = LocalDate.parse(plant.creationDate)
            Log.d("filtration", "creationDate = $cD")
            var nextDayOfWatering = cD.plusDays(plant.wateringInterval.toLong())
            Log.d("filtration", "nextDayOfWatering  = $nextDayOfWatering ")
            var isInWeek = false

            val weekDates = weekState.weekState.currentWeek.days
            Log.d("filtration", "weekDates  = $weekDates ")
            while (nextDayOfWatering.dayOfYear < 365) {
                isInWeek = nextDayOfWatering in weekDates
                if (isInWeek) {
                    break
                }
                Log.d("filtration", "isInWeek  = $isInWeek ")
                nextDayOfWatering = nextDayOfWatering.plusDays(plant.wateringInterval.toLong())
                Log.d("filtration", "nextDayOfWatering  = $nextDayOfWatering in while ")
            }
            isInWeek
        }
    }
    Box(
        modifier = Modifier
            .background(White)
            .size(1000.dp)
    )
    LaunchedEffect(Unit) { Log.d("plantList", plantList.toString()) }
    Scaffold(

        modifier = Modifier.padding(innerPadding),
        floatingActionButton = {
            Button(
                onClick = { navController.navigate(AppDestinations.PLANT_ADD) },
                colors = ButtonDefaults.buttonColors(
                    contentColor = White,
                    containerColor = Color(0xFF40BE54)
                )
            ) {
                Text("+")
            }
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .background(White)
                .size(1000.dp)
        )
        Column {
            SelectableWeekCalendar(
                dayContent = { WeekCalendar(it) },
                firstDayOfWeek = DayOfWeek.MONDAY,
                calendarState = weekState
            )
            LazyColumn(modifier = Modifier.padding(start = 30.dp, top = 30.dp)) {
                items(filteredPlantList) { plant ->
                    DayCard(
                        plant.plantName,
                        plant.taskName,
                        plant.wateringInterval,
                        plantsViewmodel,
                        plant.id,
                        plant.drugName,
                        plant.gardenName
                    )
                }
            }
        }
    }
}

@Composable
fun WeekCalendar(dayState: DayState<DynamicSelectionState>) {
    Column {
        Text(
            text = dayState.date.dayOfMonth.toString(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Black,
        )
    }
}

@Composable
fun DayCard(
    plantName: String,
    taskName: String,
    wateringInterval: Int,
    plantsViewmodel: PlantsViewmodel,
    id: Int,
    drugName: String,
    gardenName: String
) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelTasksFactory = TasksViewmodelFactory(application.repository)
    val tasksViewmodel: TasksViewmodel = viewModel(factory = viewmodelTasksFactory)
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier
            .padding(top = 8.dp)
            .size(height = 140.dp, width = 340.dp)
            .background(
                color = Color(0xFFFBFCFB), shape = RoundedCornerShape(
                    topStart = 16.dp,
                )
            )
    ) {
        Row {
            Column(modifier = Modifier.padding(start = 10.dp, top = 5.dp)) {
                Text(
                    plantName,
                    fontSize = 25.sp,
                    color = Color(0xFF000000),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    gardenName,
                    fontSize = 17.sp,
                    color = Color(0xFF075E10),
                )
                Text(
                    "Препарат: $drugName",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 1.dp),
                    color = Color(0xFF000000),
                )
                Text(
                    "Задача: $taskName",
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 1.dp),
                    color = Color(0xFF000000),
                )
                Text(
                    "Интервал: $wateringInterval дней",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 1.dp),
                    color = Color(0xFF000000),
                )
            }
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.delete),
                contentDescription = "Удалить",
                modifier = Modifier
                    .padding(start = 60.dp, top = 10.dp)
                    .size(15.dp, 20.dp)
                    .clickable { plantsViewmodel.deletePlant(id); tasksViewmodel.deleteTask(id) }
            )
        }

    }
}