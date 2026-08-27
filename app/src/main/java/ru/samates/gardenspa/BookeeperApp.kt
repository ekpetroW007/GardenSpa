package ru.samates.gardenspa

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.getValue
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import ru.samates.gardenspa.data.database.AppDatabase
import ru.samates.gardenspa.data.climate.ClimateService
import ru.samates.gardenspa.data.repository.BookeeperRepository
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.notifications.GardenWorkReminderScheduler
import ru.samates.gardenspa.notifications.WeatherReminderJobService

class BookeeperApp : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        BookeeperRepository(
            drugDao = database.drugDao(),
            plantDAO = database.plantDao(),
            gardenDAO = database.gardenDao(),
            taskDAO = database.taskDao(),
            procedureDAO = database.procedureDao(),
            gardenWorkDAO = database.gardenWorkDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .addNetworkInterceptor(Interceptor { chain ->
                    val response = chain.proceed(chain.request())
                    val body = response.body
                    if (
                        response.request.url.host != "tile.openweathermap.org" ||
                        !response.isSuccessful ||
                        body == null ||
                        body.contentType()?.subtype != "png"
                    ) {
                        response
                    } else {
                        val contentType = body.contentType()
                        val original = body.bytes()
                        val visibleTile = styleOpenWeatherPrecipitationTile(original) ?: original
                        response.newBuilder()
                            .body(visibleTile.toResponseBody(contentType))
                            .build()
                    }
                })
                .build()
        )
        TreatmentReminderScheduler.schedule(this)
        TreatmentReminderScheduler.refreshOnceToday(this)
        runCatching { WeatherReminderJobService.reschedule(this) }
        GardenWorkReminderScheduler.schedule(this)
    }

    val climateService by lazy { ClimateService() }
}

private const val PRECIPITATION_BLUE = 0x082B4C
private const val PRECIPITATION_CLOUD = 0xFFFFFF

internal fun stylePrecipitationPixel(argb: Int): Int {
    val alpha = argb ushr 24
    if (alpha == 0) return 0
    val visibleAlpha = (alpha * 6).coerceIn(112, 224)
    return (visibleAlpha shl 24) or PRECIPITATION_BLUE
}

private fun styleOpenWeatherPrecipitationTile(bytes: ByteArray): ByteArray? {
    val bitmap = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: return null
    return try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val sourceAlpha = IntArray(pixels.size) { index -> pixels[index] ushr 24 }
        pixels.indices.forEach { index -> pixels[index] = stylePrecipitationPixel(pixels[index]) }
        addPrecipitationCloudPattern(pixels, sourceAlpha, bitmap.width, bitmap.height)
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        ByteArrayOutputStream().use { output ->
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) output.toByteArray() else null
        }
    } finally {
        bitmap.recycle()
    }
}

internal fun addPrecipitationCloudPattern(
    pixels: IntArray,
    sourceAlpha: IntArray,
    width: Int,
    height: Int
) {
    require(pixels.size == width * height && sourceAlpha.size == pixels.size)
    for (centerY in 32 until height step 64) {
        for (centerX in 32 until width step 64) {
            var rainySamples = 0
            for (y in (centerY - 24).coerceAtLeast(0) until (centerY + 24).coerceAtMost(height) step 4) {
                for (x in (centerX - 24).coerceAtLeast(0) until (centerX + 24).coerceAtMost(width) step 4) {
                    if (sourceAlpha[y * width + x] > 0) rainySamples++
                }
            }
            if (rainySamples < 12) continue
            for (y in (centerY - 18).coerceAtLeast(0)..(centerY + 10).coerceAtMost(height - 1)) {
                for (x in (centerX - 20).coerceAtLeast(0)..(centerX + 20).coerceAtMost(width - 1)) {
                    val index = y * width + x
                    if (sourceAlpha[index] > 0 && isCloudPixel(x - centerX, y - centerY)) {
                        pixels[index] = (248 shl 24) or PRECIPITATION_CLOUD
                    }
                }
            }
        }
    }
}

private fun isCloudPixel(x: Int, y: Int): Boolean =
    (x + 9) * (x + 9) + y * y <= 8 * 8 ||
        x * x + (y + 6) * (y + 6) <= 11 * 11 ||
        (x - 10) * (x - 10) + y * y <= 8 * 8 ||
        (x in -17..18 && y in 0..8)
