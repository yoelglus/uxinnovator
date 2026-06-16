package com.sliide.uxinnovator.di

import com.sliide.uxinnovator.presentation.adduserform.AddUserViewModel
import com.sliide.uxinnovator.presentation.userlist.UserListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { UserListViewModel(get(), get(), get()) }
    viewModel { AddUserViewModel(get()) }
}
