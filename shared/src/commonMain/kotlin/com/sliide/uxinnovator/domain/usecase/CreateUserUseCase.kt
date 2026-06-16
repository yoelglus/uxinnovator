package com.sliide.uxinnovator.domain.usecase

import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.repository.UserRepository

class CreateUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(name: String, email: String): User =
        repository.createUser(name, email)
}
