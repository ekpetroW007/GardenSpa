package com.example.myapplication.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BookeeperApp
import com.example.myapplication.R
import com.example.myapplication.viewmodel.DrugsViewmodel
import com.example.myapplication.viewmodel.DrugsViewmodelFactory
import com.example.myapplication.viewmodel.GardensViewmodel
import com.example.myapplication.viewmodel.GardensViewmodelFactory
import com.example.myapplication.viewmodel.PlantsViewmodel
import com.example.myapplication.viewmodel.PlantsViewmodelFactory
import com.example.myapplication.viewmodel.TasksViewmodel
import com.example.myapplication.viewmodel.TasksViewmodelFactory

@Composable
fun Profile(navController: NavController) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelPlantsFactory = PlantsViewmodelFactory(application.repository)
    val plantsViewmodel: PlantsViewmodel = viewModel(factory = viewmodelPlantsFactory)
    val viewmodelGardensFactory = GardensViewmodelFactory(application.repository)
    val gardensViewmodel: GardensViewmodel = viewModel(factory = viewmodelGardensFactory)
    val viewmodelFactory = DrugsViewmodelFactory(application.repository)
    val drugsViewmodel: DrugsViewmodel = viewModel(factory = viewmodelFactory)
    val viewmodelTasksFactory = TasksViewmodelFactory(application.repository)
    val tasksViewmodel: TasksViewmodel = viewModel(factory = viewmodelTasksFactory)
    val drugList by drugsViewmodel.drugs.collectAsState()
    val plantList by plantsViewmodel.plants.collectAsState()
    val taskList by tasksViewmodel.tasks.collectAsState()
    val gardenList by gardensViewmodel.gardens.collectAsState()
    Box(
        modifier = Modifier
            .background(White)
            .size(1000.dp)
    )
    Column(modifier = Modifier.padding(top = 100.dp, end = 17.dp)) {
        Text(
            "Статистика",
            fontSize = 25.sp,
            fontWeight = Bold,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)
        )

        Row(modifier = Modifier.padding(top = 12.dp, start = 12.dp)) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(70.dp)
                    .background(
                        White, shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        )
                    )

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
                            gardenList.size.toString(),
                            color = Color(0xFF18C933),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center, fontWeight = Bold
                        )
                        Text(
                            "Садов",
                            modifier = Modifier.fillMaxWidth(1f),
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 90.dp)
                    .background(
                        White, shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        )
                    )
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
                            color = Color(0xFF18C933),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 20.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Растений",
                            modifier = Modifier.fillMaxWidth(1f),
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 100.dp)
                    .background(
                        White, shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        )
                    )
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
                            drugList.size.toString(),
                            color = Color(0xFF18C933),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 20.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Препаратов",
                            modifier = Modifier.fillMaxWidth(1f),
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(15.dp))
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .size(height = 70.dp, width = 100.dp)
                    .background(
                        White, shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                        )
                    )
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
                            color = Color(0xFF18C933),
                            modifier = Modifier.fillMaxWidth(1f),
                            fontSize = 20.sp, fontWeight = Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Задач",
                            modifier = Modifier.fillMaxWidth(1f),
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Column {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .padding(start = 18.dp, top = 12.dp, bottom = 10.dp)
                    .size(height = 150.dp, width = 375.dp)
                    .background(
                        White, shape = RoundedCornerShape(
                            topStart = 16.dp,
                        )
                    )
            ) {
                Row {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.picofplant),
                        contentDescription = "Картинка растения в профиле",
                        modifier = Modifier
                            .padding(horizontal = 22.dp)
                            .size(50.dp, 80.dp)
                    )
                    Text(
                        "Bookeeper",
                        modifier = Modifier
                            .padding(vertical = 20.dp, horizontal = 5.dp),
                        fontSize = 30.sp,
                        letterSpacing = 0.3.em,
                        fontWeight = Bold,
                        color = Color(0xFF000000)
                    )
                }
                Column {
                    Text(
                        "Приложение для планирования и учета обработки",
                        modifier = Modifier
                            .padding(start = 10.dp),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF000000)
                    )
                    Text(
                        "растений в саду. Создавайте графики работ,",
                        modifier = Modifier
                            .padding(start = 10.dp),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF000000)
                    )
                    Text(
                        "отслеживайте выполнение и ведите историю обработок",
                        modifier = Modifier
                            .padding(start = 10.dp),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF000000)
                    )
                }
            }
//            Card(
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                colors = CardDefaults.cardColors(containerColor = White),
//                border = BorderStroke(1.dp, Color(0xFFBD1A0E)),
//                modifier = Modifier
//                    .padding(start = 20.dp, top = 10.dp, end = 0.dp, bottom = 0.dp)
//                    .size(height = 50.dp, width = 375.dp)
//                    .background(
//                        White, shape = RoundedCornerShape(
//                            topStart = 16.dp,
//                            topEnd = 0.dp,
//                            bottomEnd = 0.dp,
//                            bottomStart = 0.dp
//                        )
//                    )
//            ) {
//                Row {
//                    Image(
//                        bitmap = ImageBitmap.imageResource(R.drawable.logout),
//                        contentDescription = "Картинка выхода из аккаунта",
//                        modifier = Modifier
//                            .padding(horizontal = 22.dp)
//                            .size(30.dp, 60.dp)
//                    )
//                    Column {
//                        Text(
//                            "Выйти из аккаунта",
//                            modifier = Modifier
//                                .padding(horizontal = 15.dp, vertical = 10.dp),
//                            fontSize = 25.sp,
//                            color = Color(0xFFBD1A0E)
//                        )
//                    }
//                }
//            }
        }
    }
}
