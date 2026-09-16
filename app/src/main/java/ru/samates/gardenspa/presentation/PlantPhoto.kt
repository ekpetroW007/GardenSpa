package ru.samates.gardenspa.presentation

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samates.gardenspa.ui.theme.Forest700
import ru.samates.gardenspa.ui.theme.Mist

/** Own the selected file rather than relying on a gallery provider's temporary access. */
internal suspend fun storePlantPhoto(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
    val directory = File(context.filesDir, "plant-photos").apply { check(isDirectory || mkdirs()) }
    val target = File(directory, "${UUID.randomUUID()}.image")
    try {
        requireNotNull(context.contentResolver.openInputStream(source)).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Неподдерживаемое изображение" }
        Uri.fromFile(target).toString()
    } catch (error: Exception) {
        target.delete()
        throw error
    }
}

internal fun loadPlantPhoto(context: Context, uri: Uri): ImageBitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = maxOf(info.size.width, info.size.height) / 1280f
            if (scale > 1f) decoder.setTargetSize(
                (info.size.width / scale).toInt().coerceAtLeast(1),
                (info.size.height / scale).toInt().coerceAtLeast(1)
            )
        }.asImageBitmap()
    }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    options.inSampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1280) options.inSampleSize *= 2
    options.inJustDecodeBounds = false
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
    }
}

@Composable
fun PlantPhoto(photoUri: String?, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = if (photoUri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                loadPlantPhoto(context, Uri.parse(photoUri))
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(Forest700),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Фотография растения: $description",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("Фото растения", color = Mist)
        }
    }
}
