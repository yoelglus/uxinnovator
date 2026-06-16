package com.sliide.uxinnovator.di

import com.sliide.uxinnovator.data.repository.UserRepositoryImpl
import com.sliide.uxinnovator.domain.repository.UserRepository
import com.sliide.uxinnovator.domain.usecase.CreateUserUseCase
import com.sliide.uxinnovator.domain.usecase.DeleteUserUseCase
import com.sliide.uxinnovator.domain.usecase.FormatRelativeTimeUseCase
import com.sliide.uxinnovator.domain.usecase.GetUsersUseCase
import com.sliide.uxinnovator.domain.usecase.RefreshUsersUseCase
import org.koin.dsl.module

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single { GetUsersUseCase(get()) }
    single { RefreshUsersUseCase(get()) }
    single { CreateUserUseCase(get()) }
    single { DeleteUserUseCase(get()) }
    single { FormatRelativeTimeUseCase() }
}
