package com.sliide.uxinnovator.presentation.adduserform

data class AddUserState(
    val name: String = "",
    val email: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

sealed interface AddUserAction {
    data class UpdateName(val name: String) : AddUserAction
    data class UpdateEmail(val email: String) : AddUserAction
    data object Submit : AddUserAction
    data object Dismiss : AddUserAction
}
