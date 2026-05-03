package com.wzl.duskreader.tv.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.graphics.vector.ImageVector
import com.wzl.duskreader.tv.presentation.screens.bookDetails.BookDetailsScreen
import com.wzl.duskreader.tv.presentation.screens.reader.ReaderScreen

enum class Screens(
    private val args: List<String>? = null,
    val isTabItem: Boolean = false,
    val tabIcon: ImageVector? = null,
    val tabLabel: String? = null,
) {
    Bookshelf(isTabItem = true, tabIcon = Icons.AutoMirrored.Filled.LibraryBooks, tabLabel = "书架"),
    Transfer(isTabItem = true, tabIcon = Icons.Default.CloudUpload, tabLabel = "传书"),
    Dashboard,
    BookDetails(listOf(BookDetailsScreen.BookIdBundleKey)),
    Reader(listOf(ReaderScreen.BookIdBundleKey));

    operator fun invoke(): String {
        val argList = StringBuilder()
        args?.let { nnArgs ->
            nnArgs.forEach { arg -> argList.append("/{$arg}") }
        }
        return name + argList
    }

    fun withArgs(vararg args: Any): String {
        val destination = StringBuilder()
        args.forEach { arg -> destination.append("/$arg") }
        return name + destination
    }
}
