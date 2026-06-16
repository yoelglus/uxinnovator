package com.sliide.uxinnovator.domain.model

import kotlin.time.Instant

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val status: UserStatus,
    val fetchedAt: Instant,
)

enum class UserStatus { ACTIVE, INACTIVE }
