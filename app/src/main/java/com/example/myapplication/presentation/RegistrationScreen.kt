package com.example.myapplication.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.presentation.navigation.AppDestinations
import com.example.myapplication.viewmodel.UserViewModel

@Composable
fun Registration(navController: NavController, userViewModel: UserViewModel) {
    val loginState = remember { mutableStateOf("") }
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.green drug),
//            contentDescription = "Препарат",
//            modifier = Modifier
//                .size(55.dp)
//                .align(Alignment.TopStart)
//                .padding(2.dp)
//        )
//
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.tree),
//            contentDescription = "Дерево",
//            modifier = Modifier
//                .size(70.dp)
//                .align(Alignment.TopStart)
//                .padding(start = 50.dp, top = 30.dp)
//        )
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.pot),
//            contentDescription = "Горшок",
//            modifier = Modifier
//                .size(85.dp)
//                .align(Alignment.TopStart)
//                .padding(start = 30.dp, top = 10.dp)
//        )
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.greenmailed),
//            contentDescription = "Листок",
//            modifier = Modifier
//                .size(80.dp)
//                .align(Alignment.TopStart)
//                .padding(start = 2.dp, top = 100.dp)
//        )
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.replant),
//            contentDescription = "Росток",
//            modifier = Modifier
//                .size(75.dp)
//                .align(Alignment.TopStart)
//                .padding(start = 110.dp, top = 10.dp)
//        )
//        Image(
//            bitmap = ImageBitmap.imageResource(R.drawable.drop),
//            contentDescription = "Капля",
//            modifier = Modifier
//                .size(75.dp)
//                .align(Alignment.TopStart)
//                .padding(start = 45.dp)
//        )
//    }
    Box(
        modifier = Modifier
            .size(1000.dp, 1000.dp)
            .background(Color(0xFF2E8C3F))
    ) {

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier
                .size(height = 470.dp, width = 350.dp)
                .padding(start = 35.dp, top = 210.dp)
                .background(
                    White, shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomEnd = 22.dp,
                        bottomStart = 22.dp
                    )
                )
                .fillMaxSize(),
        ) {
            Column {
                Row {
                    Text(
                        "Bookeeper",
                        modifier = Modifier
                            .padding(top = 20.dp, start = 55.dp),
                        fontSize = 33.sp,
                        fontWeight = Medium,
                        color = Color(0xFF000000)
                    )
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.picofplant),
                        contentDescription = "Картинка растения в профиле",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(55.dp, 80.dp)
                    )
                }
                Column {
                    Text(
                        "Логин",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                    TextField(
                        value = loginState.value,
                        textStyle = TextStyle(fontSize = 20.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = White,
                            unfocusedTextColor = Black,
                            focusedContainerColor = White,
                            focusedTextColor = Black,
                        ),
                        onValueChange = { newText -> loginState.value = newText },
                    )
                }
                Button(
                    onClick = {
                        if (loginState.value.isNotBlank()) {
                            userViewModel.registerUser(loginState.value)
                            navController.navigate(AppDestinations.MAINSCREEN_ROUTE) {
                                popUpTo(AppDestinations.REGISTRATION_ROUTE) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(start = 35.dp, top = 18.dp)
                        .size(height = 50.dp, width = 250.dp)
                        .background(
                            White, shape = RoundedCornerShape(topStart = 9.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = White,
                        containerColor = Color(0xFF2DBB4A),
                    )
                ) {
                    Text(
                        "Войти",
                        fontSize = 25.sp,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )
                }
            }
        }
    }
}




