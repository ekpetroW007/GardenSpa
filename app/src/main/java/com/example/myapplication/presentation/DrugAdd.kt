package com.example.myapplication.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BookeeperApp
import com.example.myapplication.R
import com.example.myapplication.viewmodel.DrugsViewmodel
import com.example.myapplication.viewmodel.DrugsViewmodelFactory

@Composable
fun DrugAdd(navController: NavController) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val viewmodelFactory = DrugsViewmodelFactory(application.repository)
    val drugsViewmodel: DrugsViewmodel = viewModel(factory = viewmodelFactory)
    val name = remember { mutableStateOf("") }
    val purpose = remember { mutableStateOf("") }
    val consumptionRate = remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .size(600.dp, 100.dp)
            .background(
                Color(0xFF40BE54), shape = RoundedCornerShape(
                    bottomEnd = 25.dp,
                    bottomStart = 25.dp
                )
            )
    ) {
        Row {
            Image(
                bitmap = ImageBitmap.imageResource(R.drawable.arrow),
                contentDescription = "Стрелка",
                modifier = Modifier
                    .padding(start = 22.dp, top = 28.dp)
                    .size(35.dp, 40.dp)
                    .clickable { navController.popBackStack() }
            )
            Text(
                "Добавление препарата",
                modifier = Modifier.padding(start = 40.dp, top = 25.dp),
                fontSize = 28.sp,
                fontWeight = Bold,
                color = White
            )
        }
    }
    Column {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .padding(start = 10.dp, top = 140.dp)
                .size(height = 100.dp, width = 365.dp)
                .background(
                    White, shape = RoundedCornerShape(
                        topStart = 16.dp
                    )
                )
        ) {
            Column {
                Text(
                    "Название препарата",
                    modifier = Modifier
                        .padding(start = 18.dp, top = 22.dp), fontSize = 22.sp
                )

                Text(name.value, fontSize = 20.sp)
                TextField(
                    value = name.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(fontSize = 20.sp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = White,
                    ),
                    onValueChange = { newText -> name.value = newText }
                )
            }
        }
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .padding(start = 10.dp, top = 20.dp)
                .size(height = 100.dp, width = 365.dp)
                .background(
                    White, shape = RoundedCornerShape(
                        topStart = 16.dp
                    )
                )
        ) {
            Column {
                Text(
                    "Цель препарата",
                    modifier = Modifier
                        .padding(start = 18.dp, top = 22.dp), fontSize = 22.sp
                )

                Text(purpose.value, fontSize = 20.sp)
                TextField(
                    value = purpose.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(fontSize = 20.sp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = White,
                    ),
                    onValueChange = { newText -> purpose.value = newText }
                )
            }
        }
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .padding(start = 10.dp, top = 20.dp)
                .size(height = 100.dp, width = 365.dp)
                .background(
                    White, shape = RoundedCornerShape(
                        topStart = 16.dp
                    )
                )
        ) {
            Column {
                Text(
                    "Норма расхода препарата",
                    modifier = Modifier
                        .padding(start = 18.dp, top = 22.dp), fontSize = 22.sp
                )

                Text(consumptionRate.value, fontSize = 20.sp)
                TextField(
                    value = consumptionRate.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = TextStyle(fontSize = 20.sp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = White,
                        focusedTextColor = White,
                    ),
                    onValueChange = { newText -> consumptionRate.value = newText }
                )
            }
        }
        Button(
            onClick = {
                drugsViewmodel.addDrug(name.value, purpose.value, consumptionRate.value)
                navController.popBackStack()

            },
            modifier = Modifier
                .padding(start = 10.dp, top = 40.dp)
                .size(height = 70.dp, width = 365.dp)
                .background(
                    White, shape = RoundedCornerShape(
                        topStart = 16.dp,
                    )
                ),
            colors = ButtonDefaults.buttonColors(
                contentColor = White,
                containerColor = Color(0xFF40BE54),

                )
        ) {
            Text(
                "Cохранить", fontSize = 25.sp, modifier = Modifier
                    .padding(horizontal = 22.dp)
            )
        }
    }
}