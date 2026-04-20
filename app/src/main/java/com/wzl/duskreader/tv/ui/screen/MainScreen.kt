package com.wzl.duskreader.tv.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wzl.duskreader.tv.data.local.AppDatabase
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.network.FileTransferServer
import com.wzl.duskreader.tv.ui.animation.AnimationSpecs
import com.wzl.duskreader.tv.ui.component.AppBackground
import com.wzl.duskreader.tv.ui.component.FullScreenLoading
import com.wzl.duskreader.tv.ui.component.TopNavBar
import com.wzl.duskreader.tv.ui.component.TopNavItem
import com.wzl.duskreader.tv.ui.navigation.Screen
import com.wzl.duskreader.tv.ui.viewmodel.ReaderViewModel
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen() {
    var selectedScreen by remember { mutableStateOf(Screen.Home) }
    var readingBook by remember { mutableStateOf<Book?>(null) }
    val context = LocalContext.current

    val repositoryState = produceState<BookRepository?>(initialValue = null) {
        withContext(Dispatchers.IO) {
            value = BookRepository(AppDatabase.getDatabase(context).bookDao())
        }
    }
    val repository = repositoryState.value

    if (repository == null) {
        AppBackground {
            FullScreenLoading(message = "初始化书库...")
        }
        return
    }

    val shelfViewModel: ShelfViewModel = viewModel(factory = ShelfViewModelFactory(repository))
    val fileTransferServer = remember(repository) { FileTransferServer(context, repository) }

    val homeTabRequester = remember { FocusRequester() }
    val shelfTabRequester = remember { FocusRequester() }
    val transferTabRequester = remember { FocusRequester() }
    val settingsTabRequester = remember { FocusRequester() }
    val homeEntryRequester = remember { FocusRequester() }
    val shelfEntryRequester = remember { FocusRequester() }
    val transferEntryRequester = remember { FocusRequester() }
    val settingsEntryRequester = remember { FocusRequester() }

    val navItems = remember {
        listOf(
            TopNavItem(Screen.Home, homeTabRequester, homeEntryRequester),
            TopNavItem(Screen.Shelf, shelfTabRequester, shelfEntryRequester),
            TopNavItem(Screen.Transfer, transferTabRequester, transferEntryRequester),
            TopNavItem(Screen.Settings, settingsTabRequester, settingsEntryRequester)
        )
    }

    LaunchedEffect(shelfViewModel) {
        shelfViewModel.scanStorageWithDelay()
    }

    LaunchedEffect(selectedScreen) {
        if (selectedScreen == Screen.Transfer) {
            fileTransferServer.start()
        } else {
            fileTransferServer.stop()
        }
    }

    LaunchedEffect(repository) {
        try { homeTabRequester.requestFocus() } catch (_: Exception) {}
    }

    AnimatedContent(
        targetState = readingBook,
        transitionSpec = {
            fadeIn(AnimationSpecs.PageTransitionFade) togetherWith
                fadeOut(AnimationSpecs.PageTransitionFade)
        },
        label = "reader-content"
    ) { currentBook: Book? ->
        if (currentBook != null) {
            val readerViewModel = remember(currentBook) { ReaderViewModel(currentBook, repository) }
            ReaderScreen(
                viewModel = readerViewModel,
                onBack = {
                    readerViewModel.saveProgress()
                    readingBook = null
                }
            )
            LaunchedEffect(currentBook) {
                fileTransferServer.stop()
            }
        } else {
            AppBackground {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopNavBar(
                        selectedScreen = selectedScreen,
                        items = navItems,
                        onScreenSelected = { selectedScreen = it }
                    )

                    AnimatedContent(
                        targetState = selectedScreen,
                        transitionSpec = {
                            fadeIn(AnimationSpecs.PageTransitionFade) togetherWith
                                fadeOut(AnimationSpecs.PageTransitionFade)
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        label = "content-area"
                    ) { screen ->
                        val screenModifier = Modifier.fillMaxSize()
                        when (screen) {
                            Screen.Home -> HomeScreen(
                                viewModel = shelfViewModel,
                                entryRequester = homeEntryRequester,
                                tabUpRequester = homeTabRequester,
                                onBookClick = { readingBook = it },
                                onNavigateToTransfer = {
                                    selectedScreen = Screen.Transfer
                                    try { transferTabRequester.requestFocus() } catch (_: Exception) {}
                                },
                                modifier = screenModifier
                            )
                            Screen.Shelf -> ShelfScreen(
                                viewModel = shelfViewModel,
                                entryRequester = shelfEntryRequester,
                                tabUpRequester = shelfTabRequester,
                                onBookClick = { readingBook = it },
                                modifier = screenModifier
                            )
                            Screen.Transfer -> TransferScreen(
                                server = fileTransferServer,
                                entryRequester = transferEntryRequester,
                                tabUpRequester = transferTabRequester,
                                modifier = screenModifier
                            )
                            Screen.Settings -> SettingsScreen(
                                entryRequester = settingsEntryRequester,
                                tabUpRequester = settingsTabRequester,
                                onBackHome = {
                                    selectedScreen = Screen.Home
                                    try { homeTabRequester.requestFocus() } catch (_: Exception) {}
                                },
                                modifier = screenModifier
                            )
                            Screen.Debug -> DebugScreen(viewModel = shelfViewModel)
                        }
                    }
                }
            }
        }
    }
}
