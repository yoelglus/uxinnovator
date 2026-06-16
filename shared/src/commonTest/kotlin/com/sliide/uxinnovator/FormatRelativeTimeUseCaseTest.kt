package com.sliide.uxinnovator

import com.sliide.uxinnovator.domain.usecase.FormatRelativeTimeUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatRelativeTimeUseCaseTest {

    private val useCase = FormatRelativeTimeUseCase()

    @Test
    fun `30 seconds ago returns just now`() {
        val instant = Clock.System.now() - 30.seconds
        assertEquals("just now", useCase(instant))
    }

    @Test
    fun `90 seconds ago returns 1m ago`() {
        val instant = Clock.System.now() - 90.seconds
        assertEquals("1m ago", useCase(instant))
    }

    @Test
    fun `2 hours ago returns 2h ago`() {
        val instant = Clock.System.now() - 2.hours
        assertEquals("2h ago", useCase(instant))
    }

    @Test
    fun `3 days ago returns 3d ago`() {
        val instant = Clock.System.now() - 3.days
        assertEquals("3d ago", useCase(instant))
    }

    @Test
    fun `35 days ago returns 1mo ago`() {
        val instant = Clock.System.now() - 35.days
        assertEquals("1mo ago", useCase(instant))
    }
}
