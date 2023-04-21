package dev.johnoreilly.confetti.auto.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dev.johnoreilly.confetti.R
import dev.johnoreilly.confetti.utils.format
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import java.io.File
import java.time.format.DateTimeFormatter

private const val TAG = "AutoUtils"

val client = HttpClient()

private lateinit var defaultBitmap: Bitmap

fun getDefaultBitmap(carContext: CarContext): Bitmap {
    if (!::defaultBitmap.isInitialized) {
        defaultBitmap = ContextCompat.getDrawable(carContext, R.drawable.ic_filled_person)!!.toBitmap()
    }

    return defaultBitmap
}

fun fetchImage(carContext: CarContext, tag: String, link: String, imageData: AutoImageData) {
    Log.d(TAG, "Retrieving picture from: $tag")
    try {
        CoroutineScope(Dispatchers.IO).launch {
            val url = Url(link)
            val file = File("${carContext.filesDir.path}/${System.currentTimeMillis()}.jpeg")
            val result = client.get(url) {
                header(HttpHeaders.Accept, "image/jpeg")
            }

            if (result.status == HttpStatusCode.OK) {
                result.bodyAsChannel().copyAndClose(file.writeChannel())
                val bitmap = BitmapFactory.decodeFile(file.path)
                if (bitmap != null) {
                    imageData.onImageFetch(bitmap)
                    return@launch
                }
            }

            imageData.onImageFetch(getDefaultBitmap(carContext))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Unable to fetch image from the network. Reason=$e")
        imageData.onImageFetch(getDefaultBitmap(carContext))
    }
}

fun colorize(str: String, color: CarColor, index: Int, length: Int): CharSequence {
    return SpannableString(str).apply {
        setSpan(
            ForegroundCarColorSpan.create(color),
            index,
            index + length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

fun formatDateTime(time: LocalDateTime): String {
    return DateTimeFormatter.ofPattern("MMM d, HH:mm").format(time)
}