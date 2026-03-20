package com.wzl.duskreader.tv.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.ui.viewmodel.FileExplorerViewModel

/**
 * 文件管理页面：允许用户浏览存储目录并导入书籍。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    onBookClick: (Book) -> Unit
) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.files.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    // 处理返回键：如果在子目录中，则返回上一级
    BackHandler(enabled = currentPath?.parentFile != null) {
        viewModel.navigateUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "文件管理",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = currentPath?.absolutePath ?: "存储根目录",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
            
            if (isImporting) {
                Text("正在导入...", color = MaterialTheme.colorScheme.primary)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.medium,
            colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.2f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 如果有父级目录，显示“返回上一级”
                if (currentPath?.parentFile != null) {
                    item {
                        FileListItem(
                            name = ".. (返回上一级)",
                            isFolder = true,
                            onClick = { viewModel.navigateUp() },
                            isUpItem = true
                        )
                    }
                }

                items(files) { file ->
                    FileListItem(
                        name = file.name,
                        isFolder = file.isDirectory,
                        onClick = {
                            if (file.isDirectory) {
                                viewModel.loadDirectory(file)
                            } else {
                                viewModel.importFile(file) { book ->
                                    if (book != null) onBookClick(book)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 列表项组件
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileListItem(
    name: String,
    isFolder: Boolean,
    onClick: () -> Unit,
    isUpItem: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    isUpItem -> Icons.Default.KeyboardArrowUp
                    isFolder -> Icons.Default.Folder
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = if (isFolder) MaterialTheme.colorScheme.secondary else Color.Gray
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }
}
