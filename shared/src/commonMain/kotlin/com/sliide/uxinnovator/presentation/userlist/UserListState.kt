package com.sliide.uxinnovator.presentation.userlist

import com.sliide.uxinnovator.domain.model.User

sealed interface UserListState {
    data object Loading : UserListState
    data class Success(
        val users: List<User>,
        val pendingDeleteUser: User? = null,
    ) : UserListState
    data class Error(val message: String, val isNetworkError: Boolean = false) : UserListState
}

sealed interface UserListEvent {
    data class ShowDeleteConfirmation(val user: User) : UserListEvent
    data class ShowUndoSnackbar(val user: User) : UserListEvent
    data class ShowRefreshError(val message: String) : UserListEvent
    data class ScrollToIndex(val index: Int) : UserListEvent
}
