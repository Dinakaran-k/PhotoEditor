package com.dinakaran.interactivephotocanvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinakaran.interactivephotocanvas.utils.combineBitmapWithDrawingAndStickers
import com.dinakaran.interactivephotocanvas.utils.loadBitmapFromUri
import com.dinakaran.interactivephotocanvas.utils.loadStickers
import com.dinakaran.interactivephotocanvas.utils.saveBitmapToStorage
import com.dinakaran.interactivephotocanvas.viewmodel.EditPhotoViewModel
import kotlinx.coroutines.launch

@Composable
fun EditPhotoScreen(
    uri: String,
    onBack: () -> Unit,
    storagePermissionGranted: Boolean,
    viewModel: EditPhotoViewModel = viewModel()
) {
    val context = LocalContext.current
    val bitmap by remember { mutableStateOf(loadBitmapFromUri(context, uri)) }
    val stickers by remember { mutableStateOf(loadStickers(context)) }
    val paths by viewModel.paths.collectAsState()
    val selectedSticker by viewModel.selectedSticker.collectAsState()
    val stickerPosition by viewModel.stickerPosition.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        viewModel.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Canvas for photo, drawing, and stickers
        bitmap?.let { bmp ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (selectedSticker != null) {
                                viewModel.updateStickerPosition(
                                    Offset(
                                        stickerPosition.x + dragAmount.x,
                                        stickerPosition.y + dragAmount.y
                                    )
                                )
                            } else {
                                viewModel.addPathPoint(change.position)
                            }
                        }
                    }
            ) {
                drawImage(bmp.asImageBitmap())
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Red,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
                    )
                }
                selectedSticker?.let { sticker ->
                    drawImage(
                        image = sticker,
                        topLeft = stickerPosition
                    )
                }
            }
        } ?: Text("Failed to load Picture", color = Color.White)

        // Sticker selection grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp)
        ) {
            items(stickers.size) { index ->
                val sticker = stickers[index]
                Image(
                    bitmap = sticker,
                    contentDescription = "Sticker",
                    modifier = Modifier
                        .size(50.dp)
                        .padding(4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                viewModel.selectSticker(sticker)
                            }
                        }
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                bitmap?.let { bitmap ->
                    if (storagePermissionGranted || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val editedBitmap = combineBitmapWithDrawingAndStickers(
                            bitmap = bitmap,
                            paths = paths,
                            sticker = selectedSticker?.asAndroidBitmap(),
                            stickerPosition = stickerPosition
                        )
                        val success = saveBitmapToStorage(context, editedBitmap)
                        coroutineScope.launch {
                            if (success) {
                                snackbarHostState.showSnackbar("Photo saved successfully")
                            } else {
                                snackbarHostState.showSnackbar("Failed to saved photo")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Storage permission is required to export")
                        }
                    }
                }
            }) {
                Text("Save")
            }
            Button(onClick = onBack) {
                Text("Discard")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}