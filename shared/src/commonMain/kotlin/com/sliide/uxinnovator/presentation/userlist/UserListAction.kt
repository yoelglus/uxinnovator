package com.sliide.uxinnovator.presentation.userlist

import com.sliide.uxinnovator.domain.model.User

sealed interface UserListAction {
    data object Refresh : UserListAction
    data class RequestDelete(val user: User) : UserListAction
    data class ConfirmDelete(val user: User) : UserListAction
    data class UndoDelete(val user: User) : UserListAction
}
