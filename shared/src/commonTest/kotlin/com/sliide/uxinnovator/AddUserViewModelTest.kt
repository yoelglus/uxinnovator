package com.sliide.uxinnovator

import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.model.UserStatus
import com.sliide.uxinnovator.domain.repository.UserRepository
import com.sliide.uxinnovator.domain.usecase.CreateUserUseCase
import com.sliide.uxinnovator.presentation.adduserform.AddUserAction
import com.sliide.uxinnovator.presentation.adduserform.AddUserViewModel
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AddUserViewModelTest {

    private val addUserViewModel = makeViewModel()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty name on submit produces nameError`() = runTest {
        addUserViewModel.state.test {
            awaitItem() // initial state
            addUserViewModel.onAction(AddUserAction.Submit)
            val errorState = awaitItem()
            assertNotNull(errorState.nameError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invalid email format on submit produces emailError`() = runTest {
        addUserViewModel.state.test {
            awaitItem() // initial state
            // Set a valid name first
            addUserViewModel.onAction(AddUserAction.UpdateName("Valid Name"))
            awaitItem() // name updated, no errors

            // Set invalid email — emailError set inline (not blank + doesn't match)
            addUserViewModel.onAction(AddUserAction.UpdateEmail("not-an-email"))
            val stateWithEmailError = awaitItem()
            assertNotNull(stateWithEmailError.emailError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valid name and email on submit results in isSuccess true`() = runTest {
        addUserViewModel.state.test {
            awaitItem() // initial state
            addUserViewModel.onAction(AddUserAction.UpdateName("Valid Name"))
            awaitItem() // name updated
            addUserViewModel.onAction(AddUserAction.UpdateEmail("valid@email.com"))
            awaitItem() // email updated, no error

            addUserViewModel.onAction(AddUserAction.Submit)
            // With UnconfinedTestDispatcher the coroutine runs eagerly:
            // isLoading = true and isSuccess = true may merge into one emission
            // Collect until we see isSuccess = true
            var seenSuccess = false
            var iterations = 0
            while (!seenSuccess && iterations < 5) {
                val s = awaitItem()
                if (s.isSuccess) seenSuccess = true
                iterations++
            }
            assertTrue(seenSuccess, "Expected isSuccess to become true")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `typing invalid email shows error then fixing it clears error`() = runTest {
        addUserViewModel.state.test {
            awaitItem() // initial state
            addUserViewModel.onAction(AddUserAction.UpdateEmail("bad-email"))
            val stateWithError = awaitItem()
            assertNotNull(stateWithError.emailError)
            addUserViewModel.onAction(AddUserAction.UpdateEmail("good@email.com"))
            val stateFixed = awaitItem()
            assertNull(stateFixed.emailError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeViewModel(
        createResult: Result<User> = Result.success(
            User(1L, "Test User", "test@test.com", UserStatus.ACTIVE, Clock.System.now())
        )
    ): AddUserViewModel {
        val repo = object : UserRepository {
            override fun getUsers(): Flow<List<User>> = flowOf(emptyList())
            override suspend fun refreshUsers() {}
            override suspend fun createUser(name: String, email: String): User =
                createResult.getOrThrow()
            override suspend fun deleteUser(id: Long) {}
        }
        return AddUserViewModel(CreateUserUseCase(repo))
    }
}
