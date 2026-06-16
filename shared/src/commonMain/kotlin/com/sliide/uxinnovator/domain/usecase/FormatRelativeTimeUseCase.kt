package com.sliide.uxinnovator.domain.usecase

import kotlin.time.Clock
import kotlin.time.Instant

class FormatRelativeTimeUseCase {
    operator fun invoke(instant: Instant): String {
        val diffSecs = Clock.System.now().epochSeconds - instant.epochSeconds
        return when {
            diffSecs < 60 -> "just now"
            diffSecs < 3_600 -> "${diffSecs / 60}m ago"
            diffSecs < 86_400 -> "${diffSecs / 3_600}h ago"
            diffSecs < 2_592_000 -> "${diffSecs / 86_400}d ago"
            else -> "${diffSecs / 2_592_000}mo ago"
        }
    }
}
