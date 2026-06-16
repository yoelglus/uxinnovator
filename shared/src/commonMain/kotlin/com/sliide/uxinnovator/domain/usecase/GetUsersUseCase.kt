package com.sliide.uxinnovator.domain.usecase

import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(private val repository: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repository.getUsers()
}
