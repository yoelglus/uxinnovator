package com.sliide.uxinnovator.domain.usecase

import com.sliide.uxinnovator.domain.repository.UserRepository

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteUser(id)
}
