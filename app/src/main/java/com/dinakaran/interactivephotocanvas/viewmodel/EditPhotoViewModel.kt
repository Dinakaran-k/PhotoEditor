package com.dinakaran.interactivephotocanvas.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class EditPhotoViewModel : ViewModel() {
    private val _paths = MutableStateFlow<List<Path>>(emptyList())
    val paths: StateFlow<List<Path>> = _paths

    private val _selectedSticker = MutableStateFlow<ImageBitmap?>(null)
    val selectedSticker: StateFlow<ImageBitmap?> = _selectedSticker

    private val _stickerPosition = MutableStateFlow(Offset(0f, 0f))
    val stickerPosition: StateFlow<Offset> = _stickerPosition

    private var currentPath: Path? = null

    fun addPathPoint(position: Offset) {
        if (currentPath == null) {
            currentPath = Path().apply {
                moveTo(
                    position.x,
                    position.y
                )
            }
        } else {
            currentPath?.lineTo(
                position.x,
                position.y
            )
        }
        currentPath?.let {
            _paths.value = _paths.value + it
        }
    }

    fun selectSticker(sticker: ImageBitmap) {
        _selectedSticker.value = sticker
        _stickerPosition.value = Offset(100f, 100f)
    }

    fun updateStickerPosition(newPosition: Offset) {
        _stickerPosition.value = newPosition
    }

    fun clear() {
        _paths.value = emptyList()
        _selectedSticker.value = null
        _stickerPosition.value = Offset(0f, 0f)
        currentPath = null
    }
}