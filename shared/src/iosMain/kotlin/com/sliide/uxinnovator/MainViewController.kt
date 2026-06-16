package com.sliide.uxinnovator

import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.uxinnovator.data.local.DatabaseDriverFactory
import com.sliide.uxinnovator.di.initKoin

private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        initKoin(DatabaseDriverFactory())
        koinInitialized = true
    }
    App()
}