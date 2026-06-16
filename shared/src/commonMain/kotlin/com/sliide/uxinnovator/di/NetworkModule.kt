package com.sliide.uxinnovator.di

import com.sliide.uxinnovator.data.remote.api.UserApiService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnException(retryOnTimeout = true)
                exponentialDelay()
            }
        }
    }
    single { UserApiService(get()) }
}
