package com.sliide.uxinnovator.data.remote

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException

object NetworkErrorMapper {
    fun map(e: Exception): NetworkError = when (e) {
        is ConnectTimeoutException -> NetworkError.NoInternet
        is SocketTimeoutException -> NetworkError.Timeout
        is ResponseException -> NetworkError.ServerError(e.response.status.value)
        else -> NetworkError.Unknown
    }
}

sealed class NetworkError {
    object NoInternet : NetworkError()
    object Timeout : NetworkError()
    data class ServerError(val code: Int) : NetworkError()
    object Unknown : NetworkError()

    fun toMessage(): String = when (this) {
        is NoInternet -> "No internet connection"
        is Timeout -> "Request timed out"
        is ServerError, is Unknown -> "Something went wrong. Please try again."
    }
}
