package com.dairypulse.app.ui.common

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp as DpUnit
import androidx.compose.ui.unit.dp
import com.dairypulse.app.data.Photos
import com.dairypulse.app.ui.theme.Dp
import kotlinx.coroutines.launch

/** The saved photo [name] decoded at about [maxPx], or null while loading / when there is none. */
@Composable
fun rememberPhoto(name: String?, maxPx: Int): ImageBitmap? {
    val context = LocalContext.current
    val photo by produceState(name?.let { Photos.cached(it, maxPx) }, name, maxPx) {
        value = name?.let { Photos.load(context, it, maxPx) }
    }
    return photo
}

/** A cow photo filling [modifier], or the first letter of [name] on a soft green ground when there is none. */
@Composable
fun CowPhoto(
    photo: String?,
    name: String,
    modifier: Modifier,
    shape: Shape,
    maxPx: Int = 512,
    initialSize: Int = 48,
    muted: Boolean = false,
) {
    val bitmap = rememberPhoto(photo, maxPx)
    Box(
        modifier.clip(shape).background(if (muted) Color(0xFFE6E0D2) else Dp.GreenPill),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, "Photo of $name", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Serif(name.take(1).uppercase().ifEmpty { "?" }, initialSize, color = if (muted) Dp.Neutral else Dp.GreenDark)
        }
    }
}

/** Round avatar showing the cow's photo when it has one. */
@Composable
fun CowAvatar(photo: String?, name: String, size: DpUnit = 40.dp, muted: Boolean = false) =
    CowPhoto(photo, name, Modifier.size(size), CircleShape, maxPx = 160, initialSize = (size.value * 0.42f).toInt(), muted = muted)

class PhotoPicker(val fromGallery: () -> Unit, val fromCamera: () -> Unit)

/**
 * Gallery and camera launchers. The chosen picture is shrunk to [maxSide] and saved,
 * then [onPicked] gets the new file name.
 */
@Composable
fun rememberPhotoPicker(maxSide: Int, onPicked: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onPicked)
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    fun save(uri: Uri) = scope.launch {
        val name = Photos.import(context, uri, maxSide)
        if (name != null) deliver(name) else Toast.makeText(context, "Couldn't read that picture", Toast.LENGTH_SHORT).show()
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::save) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) captureUri?.let(::save) }

    return remember(gallery, camera) {
        PhotoPicker(
            fromGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            fromCamera = {
                val uri = Photos.newCaptureUri(context)
                captureUri = uri
                runCatching { camera.launch(uri) }
                    .onFailure { Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show() }
            },
        )
    }
}

/** "Take photo" / "Gallery" buttons, with a remove button when a photo is set. */
@Composable
fun PhotoActions(picker: PhotoPicker, hasPhoto: Boolean, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        SoftButton("Take photo", picker.fromCamera, Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera)
        OutlineButton("Gallery", picker.fromGallery, Modifier.weight(1f), color = Dp.GreenDark, icon = Icons.Outlined.PhotoLibrary)
        if (hasPhoto) {
            Surface(
                onClick = onRemove, shape = CircleShape, color = Dp.ClayTint,
                modifier = Modifier.size(48.dp).semantics { contentDescription = "Remove photo" },
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, tint = Dp.Clay, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
