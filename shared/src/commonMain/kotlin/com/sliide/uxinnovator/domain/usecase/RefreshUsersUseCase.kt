package com.sliide.uxinnovator.domain.usecase

import com.sliide.uxinnovator.domain.repository.UserRepository

class RefreshUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke() = repository.refreshUsers()
}
