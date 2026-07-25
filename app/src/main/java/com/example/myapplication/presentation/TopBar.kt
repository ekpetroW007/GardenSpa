package com.example.myapplication.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun TopBar(topBarText: String, userLogin: String) {
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
            when (topBarText) {
                "Профиль" -> {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.profile),
                        contentDescription = "Профиль",
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 20.dp)
                            .size(80.dp, 110.dp)
                    )
                }
            }
            Column {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    topBarText,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 35.sp,
                    color = White,
                    fontWeight = Bold
                )

                when (topBarText) {
                    "Профиль" -> {
                        Text(
                            userLogin,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(start = 22.dp),
                            color = White
                        )
                    }

                    "Календарь" -> {
                        Text(
                            "Календарь обработок",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = 20.sp,
                            color = White
                        )
                    }

                    "Препараты" -> {
                        Text(
                            "Справочник средств",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = 20.sp,
                            color = White
                        )
                    }

                    "Мои сады" -> {
                        Text(
                            "Управление садами",
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = 20.sp,
                            color = White
                        )
                    }
                }
            }
        }
    }
}