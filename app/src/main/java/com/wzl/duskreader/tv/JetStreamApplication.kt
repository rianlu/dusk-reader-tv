package com.wzl.duskreader.tv

import android.app.Application
import com.wzl.duskreader.tv.network.FileTransferServer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JetStreamApplication : Application() {

    @Inject
    lateinit var fileTransferServer: FileTransferServer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { fileTransferServer.start() }
    }
}
