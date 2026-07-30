package ru.samates.gardenspa.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Forest800
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Glass
import ru.samates.gardenspa.ui.theme.GlassStroke
import ru.samates.gardenspa.ui.theme.Leaf200
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

val GlassShape = RoundedCornerShape(24.dp)
val CompactGlassShape = RoundedCornerShape(18.dp)
val SentenceKeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
fun BotanicalBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Forest950, Forest900, Color(0xFF09231C))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x553F7455), Color.Transparent),
                    center = Offset(size.width * .18f, size.height * .12f),
                    radius = size.width * .65f
                ),
                radius = size.width * .65f,
                center = Offset(size.width * .18f, size.height * .12f)
            )
            rotate(-24f, Offset(size.width * .88f, size.height * .20f)) {
                drawOval(
                    color = Color(0x183F8A5C),
                    topLeft = Offset(size.width * .70f, size.height * .02f),
                    size = Size(size.width * .25f, size.height * .38f)
                )
            }
            rotate(28f, Offset(size.width * .04f, size.height * .80f)) {
                drawOval(
                    color = Color(0x143F8A5C),
                    topLeft = Offset(-size.width * .10f, size.height * .60f),
                    size = Size(size.width * .35f, size.height * .42f)
                )
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.then(clickable),
        shape = GlassShape,
        colors = CardDefaults.cardColors(containerColor = Glass),
        border = BorderStroke(1.dp, GlassStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Glass, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", color = Cream, fontSize = 32.sp, lineHeight = 32.sp) }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 0.dp else 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Cream)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Mist)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Leaf300,
            contentColor = Forest950,
            disabledContainerColor = Forest700,
            disabledContentColor = Mist
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 15.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Forest700,
            contentColor = Cream,
            disabledContainerColor = Forest800,
            disabledContentColor = Mist
        )
    ) { Text(text) }
}

@Composable
fun DangerAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Danger.copy(alpha = 0.18f),
            contentColor = Danger,
            disabledContainerColor = Forest800,
            disabledContentColor = Mist
        )
    ) { Text(text) }
}

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Cream)
        if (action != null && onAction != null) {
            Text(action, color = Leaf300, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Leaf200, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Mist, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyGlassState(title: String, description: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Cream)
            Text(description, color = Mist, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Подтверждение удаления") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Точно ли вы хотите это удалить?")
                if (!itemName.isNullOrBlank()) {
                    Text(itemName, color = Mist)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Да", color = ru.samates.gardenspa.ui.theme.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Нет", color = Leaf300)
            }
        }
    )
}

@Composable
fun glassTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
    focusedBorderColor = Leaf300,
    unfocusedBorderColor = GlassStroke,
    focusedLabelColor = Leaf300,
    unfocusedLabelColor = Mist,
    cursorColor = Leaf300,
    focusedContainerColor = Color(0x1AFFFFFF),
    unfocusedContainerColor = Color(0x12FFFFFF)
)
