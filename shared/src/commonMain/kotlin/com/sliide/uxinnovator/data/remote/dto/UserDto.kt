package com.sliide.uxinnovator.data.remote.dto

import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.model.UserStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain() = User(
        id = id,
        name = name,
        email = email,
        status = if (status == "active") UserStatus.ACTIVE else UserStatus.INACTIVE,
        fetchedAt = Clock.System.now(),
    )
}

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    // GoRest requires gender and status fields; hardcoded as they are not part of the challenge requirements
    val gender: String = "female",
    val status: String = "active",
)
