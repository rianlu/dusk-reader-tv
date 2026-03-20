package com.wzl.duskreader.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.wzl.duskreader.tv.data.local.AppDatabase
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.ui.navigation.Screen
import com.wzl.duskreader.tv.ui.screen.*
import com.wzl.duskreader.tv.ui.theme.DuskReaderTVTheme
import com.wzl.duskreader.tv.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private var transferServer: FileTransferServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DuskReaderTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen { server: FileTransferServer ->
                        transferServer = server
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        transferServer?.stop()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(onServerInit: (FileTransferServer) -> Unit) {
    var selectedScreen by remember { mutableStateOf(Screen.Shelf) }
    var readingBook by remember { mutableStateOf<Book?>(null) }
    
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val repository = remember(database) { BookRepository(database.bookDao()) }
    val shelfViewModel: ShelfViewModel = viewModel(
        factory = ShelfViewModelFactory(repository)
    )
    val explorerViewModel: FileExplorerViewModel = viewModel(
        factory = FileExplorerViewModelFactory(repository)
    )

    // 初始化传书服务器
    val transferServer = remember {
        FileTransferServer(context, repository).also {
            it.start()
            onServerInit(it)
        }
    }

    if (readingBook != null) {
        val readerViewModel = remember(readingBook) {
            ReaderViewModel(readingBook!!, repository)
        }
        ReaderScreen(
            viewModel = readerViewModel,
            onBack = { readingBook = null }
        )
    } else {
        // 显式管理 Drawer 状态，确保在 Apple TV 风格切换时绝对稳定
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        NavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(12.dp)
                        .selectableGroup()
                        // 核心：监听整个侧边栏区域的焦点
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                drawerState.setValue(DrawerValue.Open)
                            } else {
                                drawerState.setValue(DrawerValue.Closed)
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "暮阅",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Screen.entries.filter { it != Screen.Debug }.forEach { screen ->
                        NavigationDrawerItem(
                            selected = (selectedScreen == screen),
                            onClick = { selectedScreen = screen },
                            // 焦点跟随逻辑：确保在 Apple TV 模式下，Drawer 依然认为自己是有焦点的
                            modifier = Modifier.onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    selectedScreen = screen
                                }
                            },
                            leadingContent = { 
                                Icon(
                                    imageVector = screen.icon, 
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            }
                        ) {
                            Text(
                                text = screen.title, 
                                modifier = Modifier.padding(horizontal = 12.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 24.dp)
            ) {
                when (selectedScreen) {
                    Screen.Shelf -> ShelfScreen(shelfViewModel) { book -> readingBook = book }
                    Screen.Recent -> RecentScreen(shelfViewModel) { book -> readingBook = book }
                    Screen.Explorer -> FileExplorerScreen(explorerViewModel) { book -> readingBook = book }
                    Screen.Transfer -> TransferScreen(transferServer)
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("正在开发中: ${selectedScreen.title}", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}
