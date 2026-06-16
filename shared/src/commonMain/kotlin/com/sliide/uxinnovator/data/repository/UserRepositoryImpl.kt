package com.sliide.uxinnovator.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sliide.uxinnovator.data.local.UserDatabase
import com.sliide.uxinnovator.data.remote.api.UserApiService
import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.model.UserStatus
import com.sliide.uxinnovator.domain.repository.UserRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val api: UserApiService,
    private val database: UserDatabase,
) : UserRepository {

    override fun getUsers(): Flow<List<User>> =
        database.userEntityQueries.getAllUsers()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun refreshUsers() {
        val users = api.getRecentUsers()
        val now = Clock.System.now().toString()
        database.userEntityQueries.transaction {
            database.userEntityQueries.deleteAllUsers()
            users.forEach { dto ->
                database.userEntityQueries.insertUser(
                    id = dto.id,
                    name = dto.name,
                    email = dto.email,
                    status = dto.status,
                    fetchedAt = now,
                )
            }
        }
    }

    override suspend fun createUser(name: String, email: String): User {
        val dto = api.createUser(name, email)
        database.userEntityQueries.insertUser(
            id = dto.id,
            name = dto.name,
            email = dto.email,
            status = dto.status,
            fetchedAt = Clock.System.now().toString(),
        )
        return dto.toDomain()
    }

    override suspend fun deleteUser(id: Long) {
        api.deleteUser(id)
        database.userEntityQueries.deleteUser(id)
    }
}

private fun com.sliide.uxinnovator.data.local.UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email,
    status = if (status == "active") UserStatus.ACTIVE else UserStatus.INACTIVE,
    fetchedAt = Instant.parse(fetchedAt),
)
