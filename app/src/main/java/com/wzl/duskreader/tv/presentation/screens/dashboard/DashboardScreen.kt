package com.wzl.duskreader.tv.presentation.screens.dashboard

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wzl.duskreader.tv.presentation.screens.Screens
import com.wzl.duskreader.tv.presentation.screens.bookshelf.BookshelfScreen
import com.wzl.duskreader.tv.presentation.screens.bookshelf.BookshelfScreenMode
import com.wzl.duskreader.tv.presentation.screens.settings.SettingsScreen
import com.wzl.duskreader.tv.presentation.screens.transfer.TransferScreen
import com.wzl.duskreader.tv.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

@Composable
fun DashboardScreen(
    openBookDetailsScreen: (bookId: Long) -> Unit,
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()

    var isTopBarVisible by remember { mutableStateOf(true) }
    var isTopBarFocused by remember { mutableStateOf(false) }
    var contentFocusRequestVersion by remember { mutableLongStateOf(0L) }

    var currentDestination: String? by remember { mutableStateOf(null) }
    val currentTopBarSelectedTabIndex by remember(currentDestination) {
        derivedStateOf {
            currentDestination?.let { destination ->
                TopBarTabs.indexOfFirst { it.name == destination }.takeIf { it >= 0 } ?: 0
            } ?: 0
        }
    }

    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    BackPressHandledArea(
        onBackPressed = {
            if (!isTopBarVisible) {
                isTopBarVisible = true
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
            } else if (currentTopBarSelectedTabIndex == 0) onBackPressed()
            else if (!isTopBarFocused) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
            } else TopBarFocusRequesters[0].requestFocus()
        }
    ) {
        var wasTopBarFocusRequestedBefore by rememberSaveable { mutableStateOf(false) }

        var topBarHeightPx: Int by rememberSaveable { mutableIntStateOf(0) }

        val topBarYOffsetPx by animateIntAsState(
            targetValue = if (isTopBarVisible) 0 else -topBarHeightPx,
            animationSpec = tween(),
            label = "",
            finishedListener = {
                if (it == -topBarHeightPx && isComingBackFromDifferentScreen) {
                    focusManager.moveFocus(FocusDirection.Down)
                    resetIsComingBackFromDifferentScreen()
                }
            }
        )

        val navHostTopPaddingDp by animateDpAsState(
            targetValue = if (isTopBarVisible) with(density) { topBarHeightPx.toDp() } else 0.dp,
            animationSpec = tween(),
            label = "",
        )

        LaunchedEffect(Unit) {
            if (!wasTopBarFocusRequestedBefore) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex].requestFocus()
                wasTopBarFocusRequestedBefore = true
            }
        }

        DashboardTopBar(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = topBarYOffsetPx) }
                .onSizeChanged { topBarHeightPx = it.height }
                .onFocusChanged { isTopBarFocused = it.hasFocus }
                .padding(
                    horizontal = ParentPadding.calculateStartPadding(
                        LocalLayoutDirection.current
                    ) + 8.dp
                )
                .padding(
                    top = ParentPadding.calculateTopPadding(),
                    bottom = ParentPadding.calculateBottomPadding()
                ),
            selectedTabIndex = currentTopBarSelectedTabIndex,
        ) { screen ->
            val targetRoute = screen()
            if (currentDestination != targetRoute) {
                navController.navigateTopLevel(screen)
            }
        }

        Body(
            openBookDetailsScreen = openBookDetailsScreen,
            updateTopBarVisibility = { isTopBarVisible = it },
            isTopBarVisible = isTopBarVisible,
            navController = navController,
            modifier = Modifier.offset(y = navHostTopPaddingDp),
            contentFocusRequestVersion = contentFocusRequestVersion,
            onRequestContentFocus = { contentFocusRequestVersion++ },
        )
    }
}

private fun NavHostController.navigateTopLevel(screen: Screens) {
    val targetRoute = screen()
    if (currentDestination?.route == targetRoute) return
    navigate(targetRoute) {
        // 与 JetStream 一致：只在切到首页时清空 back stack，其余 tab 只追加。
        // back stack entry 保留 → ViewModel 不销毁 → 切 tab 无需重新查询 Room → 无 loading 闪烁。
        // 返回键行为：Home → 按返回由 Dashboard.onBackPressed 处理（聚焦顶栏/退出）；
        // 其他 tab → 按返回逐层回退到上一个 tab → 最终到 Home。
        if (screen == TopBarTabs[0]) {
            popUpTo(TopBarTabs[0].invoke())
        }
        launchSingleTop = true
    }
}

@Composable
private fun BackPressHandledArea(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) =
    Box(
        modifier = Modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBackPressed()
                    true
                } else {
                    false
                }
            }
            .then(modifier),
        content = content
    )

@Composable
private fun Body(
    openBookDetailsScreen: (bookId: Long) -> Unit,
    updateTopBarVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    isTopBarVisible: Boolean = true,
    contentFocusRequestVersion: Long = 0L,
    onRequestContentFocus: () -> Unit = {},
) =
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screens.Home(),
        // TV 顶部 tab 切换应「即时换页」：去掉 navigation-compose 默认的 ~700ms 淡入淡出，
        // 消除每次切换时内容区先发虚再浮现的「闪一下」，以及双页面叠加重绘带来的卡顿。
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Screens.Home()) {
            BookshelfScreen(
                onBookClick = { book -> openBookDetailsScreen(book.id) },
                onGoTransfer = {
                    onRequestContentFocus()
                    navController.navigateTopLevel(Screens.Transfer)
                },
                onGoBookshelf = {
                    onRequestContentFocus()
                    navController.navigateTopLevel(Screens.Bookshelf)
                },
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible,
                mode = BookshelfScreenMode.Home,
                requestInitialFocus = false,
            )
        }
        composable(Screens.Bookshelf()) {
            BookshelfScreen(
                onBookClick = { book -> openBookDetailsScreen(book.id) },
                onGoTransfer = {
                    onRequestContentFocus()
                    navController.navigateTopLevel(Screens.Transfer)
                },
                onGoBookshelf = { },
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible,
                mode = BookshelfScreenMode.Library,
                requestInitialFocus = contentFocusRequestVersion > 0,
            )
        }
        composable(Screens.Transfer()) {
            TransferScreen(requestInitialFocusVersion = contentFocusRequestVersion)
        }
        composable(Screens.Settings()) {
            SettingsScreen()
        }
    }
