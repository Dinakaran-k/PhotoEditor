package com.dinakaran.interactivephotocanvas.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun loadBitmapFromUri(context: Context, uri: String): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri.toUri())?.use {
            BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadStickers(context: Context): List<ImageBitmap> {
    val stickers = mutableListOf<ImageBitmap>()
    val assetManager = context.assets
    val stickerFiles = assetManager.list("stickers") ?: emptyArray()
    stickerFiles.forEach { fileName ->
        try {
            assetManager.open("stickers/$fileName").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()?.let { stickers.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return stickers
}

fun combineBitmapWithDrawingAndStickers(
    bitmap: Bitmap,
    paths: List<Path>,
    sticker: Bitmap?,
    stickerPosition: Offset,
): Bitmap {
    val resultBitmap = createBitmap(bitmap.width, bitmap.height)
    val canvas = android.graphics.Canvas(resultBitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, null)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 5f
        style = android.graphics.Paint.Style.STROKE
    }

    paths.forEach { path ->
        canvas.drawPath(path.asAndroidPath(), paint)
    }

    sticker?.let {
        canvas.drawBitmap(it, stickerPosition.x, stickerPosition.y, null)
    }

    return resultBitmap
}

fun saveBitmapToStorage(context: Context, bitmap: Bitmap): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "EditedPhoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            true
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "EditedPhoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
            )
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}