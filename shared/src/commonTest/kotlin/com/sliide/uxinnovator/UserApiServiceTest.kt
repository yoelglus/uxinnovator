package com.sliide.uxinnovator

import com.sliide.uxinnovator.data.remote.api.UserApiService
import com.sliide.uxinnovator.data.remote.dto.UserDto
import com.sliide.uxinnovator.domain.model.UserStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserApiServiceTest {

    private val singleUserJson = """
        {"id":1,"name":"Ada Lovelace","email":"ada@test.com","status":"active","created_at":"2024-01-15T10:30:00.000Z"}
    """.trimIndent()

    private val userListJson = "[$singleUserJson]"

    private fun jsonContentType() = headersOf(
        HttpHeaders.ContentType, ContentType.Application.Json.toString(),
    )

    private fun buildClient(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun `getRecentUsers fetches page 1 and returns parsed users`() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            respond(content = userListJson, status = HttpStatusCode.OK, headers = jsonContentType())
        }

        val result = UserApiService(buildClient(engine)).getRecentUsers()

        assertEquals(1, callCount, "Expected exactly 1 request")
        assertEquals(1, result.size)
        assertEquals("Ada Lovelace", result[0].name)
        assertEquals("1", engine.requestHistory[0].url.parameters["page"], "Should query page=1")
    }

    @Test
    fun `getRecentUsers sends Authorization header`() = runTest {
        val engine = MockEngine { request ->
            assertTrue(
                request.headers.contains(HttpHeaders.Authorization),
                "Missing Authorization header on GET",
            )
            respond(content = userListJson, status = HttpStatusCode.OK, headers = jsonContentType())
        }

        UserApiService(buildClient(engine)).getRecentUsers()
    }

    @Test
    fun `createUser sends POST with Authorization header and parses response`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(
                request.headers.contains(HttpHeaders.Authorization),
                "Missing Authorization header",
            )
            respond(
                content = singleUserJson,
                status = HttpStatusCode.Created,
                headers = jsonContentType(),
            )
        }

        val result = UserApiService(buildClient(engine)).createUser("Ada Lovelace", "ada@test.com")

        assertEquals(1L, result.id)
        assertEquals("Ada Lovelace", result.name)
        assertEquals("ada@test.com", result.email)
        assertEquals("active", result.status)
    }

    @Test
    fun `createUser sends JSON content type`() = runTest {
        val engine = MockEngine { request ->
            // In Ktor 3.x + ContentNegotiation, content type lives on the body, not in raw headers
            val contentType = request.body.contentType?.toString()
            assertEquals(
                contentType?.startsWith("application/json"),
                true,
                "Expected application/json content type, got: $contentType"
            )
            respond(content = singleUserJson, status = HttpStatusCode.Created, headers = jsonContentType())
        }

        UserApiService(buildClient(engine)).createUser("Ada Lovelace", "ada@test.com")
    }

    @Test
    fun `deleteUser sends DELETE to correct URL with Authorization header`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertTrue(
                request.url.encodedPath.endsWith("/42"),
                "Expected path ending in /42, got: ${request.url.encodedPath}",
            )
            assertTrue(
                request.headers.contains(HttpHeaders.Authorization),
                "Missing Authorization header",
            )
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        UserApiService(buildClient(engine)).deleteUser(42L) // should not throw
    }

    @Test
    fun `UserDto toDomain maps active status to ACTIVE`() {
        val dto = UserDto(1L, "Ada", "ada@test.com", "active", "2024-01-15T10:30:00.000Z")
        val user = dto.toDomain()
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(1L, user.id)
        assertEquals("Ada", user.name)
        assertEquals("ada@test.com", user.email)
    }

    @Test
    fun `UserDto toDomain maps inactive status to INACTIVE`() {
        val dto = UserDto(2L, "Bob", "bob@test.com", "inactive", "2024-01-15T10:30:00.000Z")
        assertEquals(UserStatus.INACTIVE, dto.toDomain().status)
    }

    @Test
    fun `UserDto toDomain parses ISO 8601 timestamp correctly`() {
        val dto = UserDto(1L, "Ada", "ada@test.com", "active", "2024-01-15T10:30:00.000Z")
        val user = dto.toDomain()
        assertNotNull(user.fetchedAt)
    }

}
