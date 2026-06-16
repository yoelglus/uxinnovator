package com.sliide.uxinnovator.di

import com.sliide.uxinnovator.data.local.DatabaseDriverFactory
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    driverFactory: DatabaseDriverFactory,
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    modules(
        networkModule,
        databaseModule(driverFactory),
        repositoryModule,
        viewModelModule,
    )
}
