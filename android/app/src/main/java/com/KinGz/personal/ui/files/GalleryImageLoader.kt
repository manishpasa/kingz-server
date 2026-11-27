package com.KinGz.personal.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import com.KinGz.personal.network.MediaClient

@Composable
fun rememberGalleryImageLoader(): ImageLoader {
    val context = LocalContext.current

    return remember(context) {
        ImageLoader.Builder(context)
            .okHttpClient {
                MediaClient.create(context)
            }
            .build()
    }
}
