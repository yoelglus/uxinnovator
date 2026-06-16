package com.sliide.uxinnovator.di

import com.sliide.uxinnovator.data.local.DatabaseDriverFactory
import com.sliide.uxinnovator.data.local.UserDatabase
import org.koin.dsl.module

fun databaseModule(driverFactory: DatabaseDriverFactory) = module {
    single { driverFactory }
    single { UserDatabase(driverFactory.createDriver()) }
}
