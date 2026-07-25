package com.example.myapplication.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun AppButtonBar(onClick: (String) -> Unit) {
    Row(modifier = Modifier.padding(start = 6.dp).background(White)) {
        NavigationItem("Календарь", onClick)
        NavigationItem("Мои сады", onClick)
        NavigationItem("Препараты", onClick)
        NavigationItem("Профиль", onClick)
    }
}

@Composable
fun NavigationItem(text: String, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(5.dp)
            .clickable { onClick(text) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (text) {
            "Профиль" -> {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.bottomprofile),
                    contentDescription = "Картинка профиля",
                    modifier = Modifier
                        .size(22.dp)
                        .padding(start = 4.dp)
                )
                Text(
                    text, fontSize = 15.sp,
                    color = Color(0xFF48484B), modifier = Modifier.padding(start = 4.dp)
                )
            }

            "Препараты" -> {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.drug),
                    contentDescription = "Картинка препаратов",
                    modifier = Modifier
                        .size(22.dp)
                        .padding(start = 4.dp)
                )
                Text(
                    text, fontSize = 15.sp,
                    color = Color(0xFF48484B), modifier = Modifier.padding(start = 4.dp)
                )
            }

            "Мои сады" -> {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.mygardens),
                    contentDescription = "Картинка моих садов",
                    modifier = Modifier
                        .size(22.dp)
                        .padding(start = 4.dp)
                )
                Text(
                    text, fontSize = 15.sp,
                    color = Color(0xFF48484B), modifier = Modifier.padding(start = 4.dp)
                )
            }

            "Календарь" -> {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.calendar),
                    contentDescription = "Картинка календаря",
                    modifier = Modifier
                        .size(22.dp)
                        .padding(start = 4.dp)
                )
                Text(
                    text, fontSize = 15.sp,
                    color = Color(0xFF48484B), modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
