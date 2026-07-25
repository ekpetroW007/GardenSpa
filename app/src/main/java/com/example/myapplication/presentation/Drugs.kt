package com.example.myapplication.presentation

import androidx.compose.material3.Button
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BookeeperApp
import com.example.myapplication.R
import com.example.myapplication.presentation.navigation.AppDestinations
import com.example.myapplication.viewmodel.DrugsViewmodel
import com.example.myapplication.viewmodel.DrugsViewmodelFactory

@Composable
fun Drugs(
    navController: NavController, innerPadding: PaddingValues
) {

    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelFactory = DrugsViewmodelFactory(application.repository)
    val drugsViewmodel: DrugsViewmodel = viewModel(factory = viewmodelFactory)
    val drugList by drugsViewmodel.drugs.collectAsState()
    Box(modifier = Modifier.background(White).size(1000.dp))
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        floatingActionButton = {
            Button(
                onClick = { navController.navigate(AppDestinations.DRUG_ADD_ROUTE) },
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
//            Card(
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                colors = CardDefaults.cardColors(containerColor = White),
//                border = BorderStroke(1.dp, Color(0xFF000000)),
//                modifier = Modifier
//                    .padding(start = 25.dp, top = 10.dp)
//                    .size(height = 45.dp, width = 365.dp)
//                    .background(
//                        White, shape = RoundedCornerShape(
//                            topStart = 16.dp,
//                        )
//                    )
//            ) {
//                Row {
//                    Image(
//                        bitmap = ImageBitmap.imageResource(R.drawable.search),
//                        contentDescription = "Картинка поиска",
//                        modifier = Modifier
//                            .padding(horizontal = 22.dp)
//                            .size(20.dp, 45.dp)
//                    )
//                    Text(
//                        "Поиск использованных препаратов...", // TODO (СДЕЛАТЬ ПОИСК ПРЕПАРАТОВ)
//                        modifier = Modifier
//                            .padding(vertical = 14.dp),
//                        fontSize = 15.sp,
//                        color = Color(0xD2343538)
//                    )
//                }
//            }
            LazyColumn(modifier = Modifier.padding(start = 9.dp, top = 6.dp)) {
                items(drugList) { drug ->
                    DrugCard(drug.name, drug.purpose, drug.consumptionRate, navController, drugsViewmodel, drug.id)
                }
            }
        }
    }
}

@Composable
fun DrugCard(
    drugName: String,
    purpose: String,
    consumptionRate: String,
    navController: NavController,
    drugsViewmodel: DrugsViewmodel,
    id: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        onClick = { navController.navigate("drugInfoScreen/${drugName}/${purpose}/${consumptionRate}") },
        modifier = Modifier
            .padding(top = 20.dp)
            .size(height = 200.dp, width = 365.dp)
            .background(
                White, shape = RoundedCornerShape(
                    topStart = 16.dp,
                )
            )
    ) {
        Row {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.drug),
                contentDescription = "Картинка препарата",
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 25.dp)
                    .size(30.dp, 55.dp)
            )
            Row {
                Column {
                    Text(
                        drugName,
                        fontSize = 21.sp, modifier = Modifier.padding(top = 23.dp),
                        color = Color(0xFF000000), fontWeight = FontWeight.Bold
                    )
                    Text(
                        purpose,
                        fontSize = 16.sp,
                        color = Color(0xFF6C6A6A)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp)
        Column {
            Text(
                "Норма расхода: ",
                fontSize = 15.sp, modifier = Modifier.padding(start = 22.dp, top = 12.dp),
                color = Color(0xFF6C6A6A)
            )
            Text(
                consumptionRate,
                fontSize = 17.sp, modifier = Modifier.padding(start = 22.dp),
                color = Color(0xFF333030), fontWeight = FontWeight.Bold
            )
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.delete),
                contentDescription = "Удалить",
                modifier = Modifier
                    .padding(start = 340.dp, top = 20.dp)
                    .size(15.dp, 20.dp)
                    .clickable { drugsViewmodel.deleteDrug(id) }
            )
        }
    }
}