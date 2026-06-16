package com.sliide.uxinnovator.data.remote.api

import com.sliide.uxinnovator.Config
import com.sliide.uxinnovator.data.remote.dto.CreateUserRequest
import com.sliide.uxinnovator.data.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class UserApiService(private val client: HttpClient) {

    suspend fun getRecentUsers(): List<UserDto> =
        client.get("${Config.BASE_URL}/users") {
            header(HttpHeaders.Authorization, "Bearer ${Config.GOREST_TOKEN}")
            parameter("page", 1)
        }.body()

    suspend fun createUser(name: String, email: String): UserDto =
        client.post("${Config.BASE_URL}/users") {
            header(HttpHeaders.Authorization, "Bearer ${Config.GOREST_TOKEN}")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(name = name, email = email))
        }.body()

    suspend fun deleteUser(id: Long) {
        client.delete("${Config.BASE_URL}/users/$id") {
            header(HttpHeaders.Authorization, "Bearer ${Config.GOREST_TOKEN}")
        }
    }
}
