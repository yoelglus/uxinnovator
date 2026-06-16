package com.sliide.uxinnovator.presentation.userlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.presentation.components.UserCard
import com.sliide.uxinnovator.presentation.components.UserCardShimmer
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    viewModel: UserListViewModel = koinViewModel(),
    onAddUser: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var deleteTarget by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UserListEvent.ShowDeleteConfirmation -> deleteTarget = event.user
                is UserListEvent.ShowUndoSnackbar -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "${event.user.name} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(UserListAction.UndoDelete(event.user))
                    }
                }
                is UserListEvent.ShowRefreshError -> scope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short,
                    )
                }
                is UserListEvent.ScrollToIndex -> scope.launch {
                    gridState.animateScrollToItem(event.index)
                }
            }
        }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) gridState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("User Directory") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUser) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            val columns = if (maxWidth >= 600.dp) 2 else 1

            when (val s = state) {
                UserListState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(8) { UserCardShimmer() }
                    }
                }

                is UserListState.Error -> {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = if (s.isNetworkError) "📡" else "⚠️",
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Text(
                            text = if (s.isNetworkError) "No Internet Connection" else "Something Went Wrong",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(onClick = { viewModel.onAction(UserListAction.Refresh) }) {
                            Text("Try Again")
                        }
                    }
                }

                is UserListState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.onAction(UserListAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(s.users, key = { it.id }) { user ->
                                UserCard(
                                    user = user,
                                    onLongPress = { viewModel.onAction(UserListAction.RequestDelete(user)) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete User") },
            text = { Text("Remove ${user.name} from the list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(UserListAction.ConfirmDelete(user))
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}
