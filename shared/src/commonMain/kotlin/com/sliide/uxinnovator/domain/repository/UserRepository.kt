package com.sliide.uxinnovator.domain.repository

import com.sliide.uxinnovator.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun refreshUsers()
    suspend fun createUser(name: String, email: String): User
    suspend fun deleteUser(id: Long)
}
