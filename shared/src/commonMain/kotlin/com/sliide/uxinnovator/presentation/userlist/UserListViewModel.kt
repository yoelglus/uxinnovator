package com.sliide.uxinnovator.presentation.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.data.remote.NetworkError
import com.sliide.uxinnovator.data.remote.NetworkErrorMapper
import com.sliide.uxinnovator.domain.usecase.DeleteUserUseCase
import com.sliide.uxinnovator.domain.usecase.GetUsersUseCase
import com.sliide.uxinnovator.domain.usecase.RefreshUsersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UNDO_WINDOW_MS = 4_000L

class UserListViewModel(
    private val getUsers: GetUsersUseCase,
    private val refreshUsers: RefreshUsersUseCase,
    private val deleteUser: DeleteUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UserListState>(UserListState.Loading)
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = MutableSharedFlow<UserListEvent>()
    val events: SharedFlow<UserListEvent> = _events.asSharedFlow()

    private var pendingDeleteJob: Job? = null
    private var pendingDeleteIndex: Int = -1

    init {
        observeUsers()
        refresh()
    }

    fun onAction(action: UserListAction) = when (action) {
        UserListAction.Refresh -> refresh()
        is UserListAction.RequestDelete -> requestDelete(action.user)
        is UserListAction.ConfirmDelete -> confirmDelete(action.user)
        is UserListAction.UndoDelete -> undoDelete(action.user)
    }

    private fun observeUsers() {
        getUsers()
            .onEach { users ->
                _state.update { current ->
                    when (current) {
                        is UserListState.Success -> current.copy(users = users)
                        else -> UserListState.Success(users = users)
                    }
                }
            }
            .catch { _state.value = UserListState.Error("Something went wrong") }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        viewModelScope.launch {
            if (_state.value is UserListState.Success) {
                _isRefreshing.value = true
            } else {
                _state.value = UserListState.Loading
            }
            try {
                refreshUsers()
            } catch (e: Exception) {
                val networkError = NetworkErrorMapper.map(e)
                val currentUsers = (_state.value as? UserListState.Success)?.users
                if (currentUsers.isNullOrEmpty()) {
                    _state.value = UserListState.Error(
                        message = networkError.toMessage(),
                        isNetworkError = networkError is NetworkError.NoInternet || networkError is NetworkError.Timeout,
                    )
                } else {
                    // Cached data is shown — surface the error as a snackbar instead of replacing the list
                    _events.emit(UserListEvent.ShowRefreshError(networkError.toMessage()))
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun requestDelete(user: User) {
        viewModelScope.launch { _events.emit(UserListEvent.ShowDeleteConfirmation(user)) }
    }

    private fun confirmDelete(user: User) {
        val snapshot = (_state.value as? UserListState.Success)?.users ?: return
        pendingDeleteIndex = snapshot.indexOf(user)
        // Optimistically remove from list
        _state.update { current ->
            (current as? UserListState.Success)?.copy(
                users = current.users.filter { it.id != user.id },
                pendingDeleteUser = user,
            ) ?: current
        }
        pendingDeleteJob = viewModelScope.launch {
            _events.emit(UserListEvent.ShowUndoSnackbar(user))
            // Delay actual deletion to allow undo
            delay(UNDO_WINDOW_MS)
            runCatching { deleteUser(user.id) }
                .onFailure {
                    _state.update { current ->
                        (current as? UserListState.Success)?.copy(
                            users = snapshot,
                            pendingDeleteUser = null,
                        ) ?: current
                    }
                }
                .onSuccess {
                    _state.update { current ->
                        (current as? UserListState.Success)?.copy(pendingDeleteUser = null) ?: current
                    }
                }
        }
    }

    private fun undoDelete(user: User) {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        val insertAt = pendingDeleteIndex.also { pendingDeleteIndex = -1 }
        _state.update { current ->
            (current as? UserListState.Success)?.let { success ->
                val users = success.users.toMutableList()
                users.add(insertAt.coerceIn(0, users.size), user)
                success.copy(users = users, pendingDeleteUser = null)
            } ?: current
        }
        viewModelScope.launch { _events.emit(UserListEvent.ScrollToIndex(insertAt)) }
    }
}
