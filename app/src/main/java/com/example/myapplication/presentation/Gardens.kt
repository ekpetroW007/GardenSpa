package com.example.myapplication.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BookeeperApp
import com.example.myapplication.R
import com.example.myapplication.data.database.entity.PlantEntity
import com.example.myapplication.presentation.navigation.AppDestinations
import com.example.myapplication.viewmodel.GardensViewmodel
import com.example.myapplication.viewmodel.GardensViewmodelFactory
import com.example.myapplication.viewmodel.PlantsViewmodel
import com.example.myapplication.viewmodel.PlantsViewmodelFactory
import com.example.myapplication.viewmodel.TasksViewmodel
import com.example.myapplication.viewmodel.TasksViewmodelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable

fun MyGardens(navController: NavController, innerPadding: PaddingValues) {

    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelGardenFactory = GardensViewmodelFactory(application.repository)
    val gardensViewmodel: GardensViewmodel = viewModel(factory = viewmodelGardenFactory)
    val gardensList by gardensViewmodel.gardens.collectAsState()
    Box(modifier = Modifier.background(White).size(1000.dp))

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        floatingActionButton = {
            Button(
                onClick = { navController.navigate(AppDestinations.GARDEN_ADD) },
                colors = ButtonDefaults.buttonColors(
                    contentColor = White,
                    containerColor = Color(0xFF40BE54)
                )
            ) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.background(White).size(1000.dp))
        Column(modifier = Modifier.padding(innerPadding)) {

            LazyColumn(modifier = Modifier.padding(start = 5.dp, top = 5.dp)) {
                items(gardensList) { garden ->
                    GardensCard(garden.name, gardensViewmodel, garden.id)
                }
            }
        }
    }
}

@Composable
fun GardensCard(gardenName: String, gardenViewmodel: GardensViewmodel, id: Int) {
    val application1 = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelTasksFactory = TasksViewmodelFactory(application1.repository)
    val tasksViewmodel: TasksViewmodel = viewModel(factory = viewmodelTasksFactory)
    val viewmodelPlantsFactory = PlantsViewmodelFactory(application1.repository)
    val plantsViewmodel: PlantsViewmodel = viewModel(factory = viewmodelPlantsFactory)
    val plantList by plantsViewmodel.plants.collectAsState()
    val taskList by tasksViewmodel.tasks.collectAsState()
    val filteredPlantList = plantList.filter { it.gardenId == id }
    val context = LocalContext.current
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier
            .padding(top = 20.dp)
            .size(height = 500.dp, width = 365.dp)
            .background(
                color = Color(0xFFFBFCFB), shape = RoundedCornerShape(
                    topStart = 16.dp,
                )
            )
    ) {
        Row {
            Column {
                Row {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.export),
                        contentDescription = "Экспортировать",
                        modifier = Modifier
                            .padding(start = 10.dp, top = 10.dp)
                            .size(15.dp, 20.dp)
                            .clickable {
                                exportGardenToFile(
                                    context = context,
                                    gardenName = gardenName,
                                    plants = filteredPlantList
                                )
                            }
                    )
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.delete),
                        contentDescription = "Удалить",
                        modifier = Modifier
                            .padding(start = 10.dp, top = 10.dp)
                            .size(15.dp, 20.dp)
                            .clickable { gardenViewmodel.deleteGarden(id) }
                    )
                }
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.greenmygarden),
                    contentDescription = "Картинка моих садов",
                    modifier = Modifier
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                        .size(40.dp)
                )
            }
            Text(
                gardenName,
                fontSize = 25.sp,
                modifier = Modifier.padding(top = 38.dp, start = 35.dp),
                color = Color(0xFF000000),
                fontWeight = FontWeight.Medium
            )
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.longline),
            contentDescription = "Линия",
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .size(323.dp, 1.dp)
        )
        Row {
            Spacer(modifier = Modifier.width(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 90.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            filteredPlantList.size.toString(),
                            color = Color(0xFF40BE54),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 22.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Растений",
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 16.sp,
                            color=Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            taskList.size.toString(),
                            color = Color(0xFF40BE54),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 22.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Задач",
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 16.sp,
                            color=Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 85.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            plantList.size.toString(),
                            color = Color(0xFF40BE54),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 22.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Обработок",
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 16.sp,
                            color=Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.line),
            contentDescription = "Линия",
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .size(323.dp, 1.dp)
        )
        Column {
            Text(
                "Растения:",
                modifier = Modifier.padding(top = 23.dp, start = 22.dp),
                color=Color.Black,
                fontSize = 22.sp,
            )
            LazyColumn(modifier = Modifier.padding(top = 5.dp)) {
                items(filteredPlantList) { plant ->
                    PlantsAtMygardensScreen(
                        plant.plantName,
                        plantsViewmodel
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                "Задачи:",
                modifier = Modifier.padding(top = 70.dp, start = 22.dp),
                color=Color.Black,
                fontSize = 22.sp,
            )
            LazyColumn(modifier = Modifier.padding(top = 5.dp)) {
                items(filteredPlantList) { plant ->
                    TasksAtMygardensScreen(
                        plant.taskName, tasksViewmodel
                    )
                }
            }
        }
    }
}

private fun exportGardenToFile(context: Context, gardenName: String, plants: List<PlantEntity>) {
    try {
        val content = buildString {
            appendLine("Сад: $gardenName")
            appendLine("Дата экспорта: ${getCurrentDateTime()}")
            appendLine("=".repeat(50)) // разделитель

            plants.forEach { plant ->
                appendLine("${plant.plantName} - ${plant.drugName} - ${plant.taskName}")
            }

            appendLine("=".repeat(50)) // разделитель
            appendLine("Всего растений: ${plants.size}")
        }
        val fileName = "сад_${gardenName}_${getCurrentDateTimeForFileName()}.txt"
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(content, Charsets.UTF_8) // Eight-bit Transformation Format
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Экспорт сада: $gardenName")
            putExtra(Intent.EXTRA_TEXT, "Файл экспорта сада \"$gardenName\"")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Экспортировать сад"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Ошибка при экспорте: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun getCurrentDateTime(): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) // формат написания даты
    return formatter.format(Date())
}

private fun getCurrentDateTimeForFileName(): String {
    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    return formatter.format(Date())
}


@Composable
fun PlantsAtMygardensScreen(plantName: String, plantsViewmodel: PlantsViewmodel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFA2FFAC)),
        border = BorderStroke(1.dp, Color(0xFF075E10)),
        modifier = Modifier
            .padding(start = 55.dp, top = 5.dp)
            .size(height = 35.dp, width = 155.dp)
            .background(
                White, shape = RoundedCornerShape(topStart = 16.dp)
            )
    ) {
        Text(
            plantName,
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 15.sp,
            color = Color(0xFF075E10)
        )
    }
}

@Composable
fun TasksAtMygardensScreen(name: String, tasksViewmodel: TasksViewmodel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFA2FFAC)),
        border = BorderStroke(1.dp, Color(0xFF075E10)),
        modifier = Modifier
            .padding(start = 55.dp, top = 5.dp)
            .size(height = 35.dp, width = 175.dp)
            .background(
                White, shape = RoundedCornerShape(topStart = 16.dp)
            )
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 15.sp,
            color = Color(0xFF075E10)
        )
    }
}