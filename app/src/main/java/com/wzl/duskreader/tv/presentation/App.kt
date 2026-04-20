package com.wzl.duskreader.tv.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wzl.duskreader.tv.presentation.screens.Screens
import com.wzl.duskreader.tv.presentation.screens.bookDetails.BookDetailsScreen
import com.wzl.duskreader.tv.presentation.screens.dashboard.DashboardScreen
import com.wzl.duskreader.tv.presentation.screens.reader.ReaderScreen

@Composable
fun App(
    onBackPressed: () -> Unit,
) {

    val navController = rememberNavController()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Screens.Dashboard(),
        builder = {
            composable(
                route = Screens.BookDetails(),
                arguments = listOf(
                    navArgument(BookDetailsScreen.BookIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                BookDetailsScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onStartReading = { book ->
                        navController.navigate(Screens.Reader.withArgs(book.id))
                    }
                )
            }
            composable(route = Screens.Dashboard()) {
                DashboardScreen(
                    openBookDetailsScreen = { bookId ->
                        navController.navigate(Screens.BookDetails.withArgs(bookId))
                    },
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    }
                )
            }
            composable(
                route = Screens.Reader(),
                arguments = listOf(
                    navArgument(ReaderScreen.BookIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                ReaderScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
        }
    )
}
