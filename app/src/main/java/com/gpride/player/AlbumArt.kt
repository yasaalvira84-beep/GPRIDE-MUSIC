package com.gpride.player

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val artCache = LruCache<Long, Bitmap>(24)

private fun loadArt(context: Context, song: Song): Bitmap? {
    artCache.get(song.id)?.let { return it }
    val resolver = context.contentResolver
    val bitmap = runCatching {
        if (Build.VERSION.SDK_INT >= 29) {
            resolver.loadThumbnail(song.uri, Size(512, 512), null)
        } else {
            val artUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                song.albumId,
            )
            resolver.openInputStream(artUri)?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()
    if (bitmap != null) artCache.put(song.id, bitmap)
    return bitmap
}

/** Sampul album dari MediaStore; tampil ikon not musik jika lagu tidak punya sampul. */
@Composable
fun AlbumArt(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val context = LocalContext.current
    var bitmap by remember(song?.id) { mutableStateOf<Bitmap?>(song?.let { artCache.get(it.id) }) }
    LaunchedEffect(song?.id) {
        if (song != null && bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { loadArt(context, song) }
        }
    }
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}
