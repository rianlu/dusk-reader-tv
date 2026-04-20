package com.wzl.duskreader.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.size
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.*
import com.wzl.duskreader.tv.data.local.AppDatabase
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.ui.navigation.Screen
import com.wzl.duskreader.tv.ui.screen.*
import com.wzl.duskreader.tv.ui.theme.DuskReaderTVTheme
import com.wzl.duskreader.tv.ui.viewmodel.*
import com.wzl.duskreader.tv.ui.component.StoragePermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            DuskReaderTVTheme {
                StoragePermissionHandler {
                    MainScreen()
                }
            }
        }
    }
}


