package com.wzl.duskreader.tv.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.wzl.duskreader.tv.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wzl.duskreader.tv.util.DebugLogger

enum class ReaderTheme(val bgColor: Color, val textColor: Color, val displayName: String) {
    Parchment(Color(0xFFF5F2E9), Color(0xFF2C2C2C), "羊皮纸"),
    DeepSea(Color(0xFF1A1C2C), Color(0xFFA0A5B1), "深海")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val content by viewModel.content.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentIndex by viewModel.currentChapterIndex.collectAsState()
    
    var showControls by remember { mutableStateOf(false) }
    var fontSize by remember { mutableIntStateOf(32) }
    var currentTheme by remember { mutableStateOf(ReaderTheme.Parchment) }
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    LaunchedEffect(currentIndex) {
        scrollState.scrollTo(0)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(400.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(24.dp)
            ) {
                Text("目录", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    TvLazyColumn {
                        items(chapters.size) { index ->
                            NavigationDrawerItem(
                                selected = (currentIndex == index),
                                onClick = {
                                    viewModel.loadChapter(index)
                                    scope.launch { drawerState.setValue(DrawerValue.Closed) }
                                },
                                leadingContent = { Box(Modifier.size(4.dp)) }
                            ) {
                                Text(text = chapters[index].title, maxLines = 1, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentTheme.bgColor)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> { showControls = !showControls; true }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { if (!showControls) viewModel.nextChapter(); true }
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { if (!showControls) viewModel.prevChapter(); true }
                            android.view.KeyEvent.KEYCODE_BACK -> {
                                if (drawerState.currentValue == DrawerValue.Open) {
                                    scope.launch { drawerState.setValue(DrawerValue.Closed) }
                                } else if (showControls) {
                                    showControls = false
                                } else {
                                    onBack()
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 60.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = chapters.getOrNull(currentIndex)?.title ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = currentTheme.textColor.copy(alpha = 0.6f)
                    )
                }

                Box(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.5).sp),
                        color = currentTheme.textColor,
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text(text = "进度: ${currentIndex + 1} / ${chapters.size} 章节", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { scope.launch { drawerState.setValue(DrawerValue.Open) }; showControls = false }) {
                                Icon(Icons.Default.Menu, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("目录")
                            }
                            
                            // 主题切换按钮
                            OutlinedButton(onClick = {
                                currentTheme = if (currentTheme == ReaderTheme.Parchment) ReaderTheme.DeepSea else ReaderTheme.Parchment
                            }) {
                                Icon(Icons.Default.Palette, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(currentTheme.displayName)
                            }

                            OutlinedButton(onClick = { if (fontSize > 16) fontSize -= 4 }) { Text("A-") }
                            OutlinedButton(onClick = { if (fontSize < 64) fontSize += 4 }) { Text("A+") }
                            OutlinedButton(onClick = onBack) { Text("退出阅读") }
                        }
                    }
                }
            }
        }
    }
}
