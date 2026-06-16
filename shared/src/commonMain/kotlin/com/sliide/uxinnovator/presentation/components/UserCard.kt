package com.sliide.uxinnovator.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.model.UserStatus
import com.sliide.uxinnovator.domain.usecase.FormatRelativeTimeUseCase

private val formatRelativeTime = FormatRelativeTimeUseCase()

@Composable
fun UserCard(
    user: User,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusDot(status = user.status)
            }
            Text(user.email, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatRelativeTime(user.fetchedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusDot(status: UserStatus) {
    val dotColor = when (status) {
        UserStatus.ACTIVE -> Color(0xFF4CAF50)
        UserStatus.INACTIVE -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = dotColor)
        }
    }
}
