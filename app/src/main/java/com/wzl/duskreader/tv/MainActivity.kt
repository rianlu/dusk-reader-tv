package com.wzl.duskreader.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.wzl.duskreader.tv.data.repositories.BookRepository
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.presentation.App
import com.wzl.duskreader.tv.presentation.common.StoragePermissionHandler
import com.wzl.duskreader.tv.presentation.theme.JetStreamTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bookRepository: BookRepository

    @Inject
    lateinit var fileTransferServer: FileTransferServer

    override fun onDestroy() {
        // @Singleton 服务不会随页面销毁，退出应用时必须显式停掉，否则 8080 端口常驻
        if (isFinishing) fileTransferServer.stop()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            JetStreamTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        StoragePermissionHandler(
                            onPermissionGranted = {
                                lifecycleScope.launch {
                                    bookRepository.scanLocalStorage()
                                }
                            },
                        ) {
                            App(onBackPressed = onBackPressedDispatcher::onBackPressed)
                        }
                    }
                }
            }
        }
    }
}
