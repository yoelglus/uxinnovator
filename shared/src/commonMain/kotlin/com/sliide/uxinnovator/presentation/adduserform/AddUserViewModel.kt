package com.sliide.uxinnovator.presentation.adduserform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.uxinnovator.domain.usecase.CreateUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
private val NAME_REGEX = Regex("^[A-Za-z\\s'-]{2,50}$")

class AddUserViewModel(private val createUser: CreateUserUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AddUserState())
    val state: StateFlow<AddUserState> = _state.asStateFlow()

    fun onAction(action: AddUserAction) = when (action) {
        is AddUserAction.UpdateName -> updateName(action.name)
        is AddUserAction.UpdateEmail -> updateEmail(action.email)
        AddUserAction.Submit -> submit()
        AddUserAction.Dismiss -> _state.value = AddUserState()
    }

    private fun updateName(name: String) {
        _state.update {
            it.copy(
                name = name,
                nameError = if (name.isNotEmpty() && !NAME_REGEX.matches(name))
                    "Name must be 2–50 letters" else null,
            )
        }
    }

    private fun updateEmail(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = if (email.isNotEmpty() && !EMAIL_REGEX.matches(email))
                    "Enter a valid email address" else null,
            )
        }
    }

    private fun submit() {
        val s = _state.value
        val nameError = when {
            s.name.isBlank() -> "Name is required"
            !NAME_REGEX.matches(s.name) -> "Name must be 2–50 letters"
            else -> null
        }
        val emailError = when {
            s.email.isBlank() -> "Email is required"
            !EMAIL_REGEX.matches(s.email) -> "Enter a valid email address"
            else -> null
        }
        if (nameError != null || emailError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching { createUser(s.name.trim(), s.email.trim()) }
                .onSuccess { _state.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, emailError = e.message) }
                }
        }
    }
}
