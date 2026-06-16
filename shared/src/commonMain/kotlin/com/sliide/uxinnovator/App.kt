package com.sliide.uxinnovator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sliide.uxinnovator.presentation.adduserform.AddUserSheet
import com.sliide.uxinnovator.presentation.theme.AppTheme
import com.sliide.uxinnovator.presentation.userlist.UserListScreen

@Composable
fun App() {
    AppTheme {
        var showAddUser by remember { mutableStateOf(false) }
        var userAddedCount by remember { mutableIntStateOf(0) }

        UserListScreen(
            onAddUser = { showAddUser = true },
            scrollToTopTrigger = userAddedCount,
        )

        if (showAddUser) {
            AddUserSheet(
                onDismiss = { showAddUser = false },
                onUserAdded = { showAddUser = false; userAddedCount++ },
            )
        }
    }
}
