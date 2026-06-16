package com.sliide.uxinnovator

import android.app.Application
import com.sliide.uxinnovator.data.local.DatabaseDriverFactory
import com.sliide.uxinnovator.di.initKoin
import org.koin.android.ext.koin.androidContext

class UXInnovatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(DatabaseDriverFactory(this)) {
            androidContext(this@UXInnovatorApp)
        }
    }
}
