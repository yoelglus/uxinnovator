package com.sliide.uxinnovator

import app.cash.turbine.test
import com.sliide.uxinnovator.domain.model.User
import com.sliide.uxinnovator.domain.model.UserStatus
import com.sliide.uxinnovator.domain.repository.UserRepository
import com.sliide.uxinnovator.domain.usecase.DeleteUserUseCase
import com.sliide.uxinnovator.domain.usecase.GetUsersUseCase
import com.sliide.uxinnovator.domain.usecase.RefreshUsersUseCase
import com.sliide.uxinnovator.presentation.userlist.UserListAction
import com.sliide.uxinnovator.presentation.userlist.UserListEvent
import com.sliide.uxinnovator.presentation.userlist.UserListState
import com.sliide.uxinnovator.presentation.userlist.UserListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

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
    fun `initial state is Loading before any users emitted`() = runTest {
        val vm = makeNonEmittingViewModel()
        vm.state.test {
            assertIs<UserListState.Loading>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emitting users to repo produces Success state`() = runTest {
        val repo = FakeUserRepository()
        val vm = makeViewModel(repo)
        val user = makeUser(1)
        vm.state.test {
            awaitItem()
            repo.users.value = listOf(user)
            val successState = awaitItem()
            assertIs<UserListState.Success>(successState)
            assertEquals(listOf(user), successState.users)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `RequestDelete emits ShowDeleteConfirmation event`() = runTest {
        val repo = FakeUserRepository()
        val vm = makeViewModel(repo)
        val user = makeUser(1)
        repo.users.value = listOf(user)

        vm.events.test {
            vm.onAction(UserListAction.RequestDelete(user))
            val event = awaitItem()
            assertIs<UserListEvent.ShowDeleteConfirmation>(event)
            assertEquals(user, event.user)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ConfirmDelete removes user optimistically and emits ShowUndoSnackbar`() = runTest {
        val repo = FakeUserRepository()
        val vm = makeViewModel(repo)
        val user1 = makeUser(1)
        val user2 = makeUser(2)
        repo.users.value = listOf(user1, user2)

        vm.events.test {
            vm.state.test {
                var s = awaitItem()
                while (s !is UserListState.Success || s.users.size < 2) {
                    s = awaitItem()
                }

                vm.onAction(UserListAction.ConfirmDelete(user1))

                val optimisticState = awaitItem()
                assertIs<UserListState.Success>(optimisticState)
                assertTrue(optimisticState.users.none { it.id == user1.id })

                cancelAndIgnoreRemainingEvents()
            }

            val event = awaitItem()
            assertIs<UserListEvent.ShowUndoSnackbar>(event)
            assertEquals(user1, event.user)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UndoDelete re-adds user and cancels deletion`() = runTest {
        val repo = FakeUserRepository()
        val vm = makeViewModel(repo)
        val user1 = makeUser(1)
        val user2 = makeUser(2)
        repo.users.value = listOf(user1, user2)

        vm.state.test {
            var state = awaitItem()
            while (state !is UserListState.Success || state.users.size < 2) {
                state = awaitItem()
            }

            vm.onAction(UserListAction.ConfirmDelete(user1))
            val afterDelete = awaitItem()
            assertIs<UserListState.Success>(afterDelete)
            assertTrue(afterDelete.users.none { it.id == user1.id })

            vm.onAction(UserListAction.UndoDelete(user1))
            val afterUndo = awaitItem()
            assertIs<UserListState.Success>(afterUndo)
            assertTrue(afterUndo.users.any { it.id == user1.id })

            advanceTimeBy(5_000L)
            assertTrue(repo.deletedIds.isEmpty(), "Delete should not have been called after undo")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh error with no cached data produces Error state`() = runTest {
        val vm = makeNonEmittingViewModel(refreshError = Exception("Network error"))

        vm.state.test {
            var sawError = false
            var item = awaitItem()
            if (item is UserListState.Error) sawError = true
            if (!sawError) {
                item = awaitItem()
                if (item is UserListState.Error) sawError = true
            }
            assertTrue(sawError, "Expected an Error state but got $item")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh error when cached data exists stays as Success with stale data`() = runTest {
        val repo = FakeUserRepository()
        val user = makeUser(1)
        repo.users.value = listOf(user)
        val vm = makeViewModel(repo)

        vm.state.test {
            var state = awaitItem()
            while (state !is UserListState.Success) {
                state = awaitItem()
            }
            assertEquals(listOf(user), state.users)

            repo.refreshError = Exception("Network error")
            vm.onAction(UserListAction.Refresh)

            expectNoEvents()
            val current = vm.state.value
            assertIs<UserListState.Success>(current)
            assertEquals(listOf(user), current.users)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete error restores original list and stays in Success state`() = runTest {
        val repo = FakeUserRepository()
        repo.deleteError = Exception("Delete failed")
        val user1 = makeUser(1)
        val user2 = makeUser(2)
        repo.users.value = listOf(user1, user2)
        val vm = makeViewModel(repo)

        vm.state.test {
            var state = awaitItem()
            while (state !is UserListState.Success || state.users.size < 2) {
                state = awaitItem()
            }

            vm.onAction(UserListAction.ConfirmDelete(user1))

            val optimisticState = awaitItem()
            assertIs<UserListState.Success>(optimisticState)
            assertTrue(optimisticState.users.none { it.id == user1.id })

            advanceTimeBy(4_001L)

            val restoredState = awaitItem()
            assertIs<UserListState.Success>(restoredState)
            assertEquals(listOf(user1, user2), restoredState.users)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Refresh action after Error state transitions to Success`() = runTest {
        val usersFlow = MutableSharedFlow<List<User>>(replay = 1)
        var refreshCount = 0
        val repo = object : UserRepository {
            override fun getUsers(): Flow<List<User>> = usersFlow
            override suspend fun refreshUsers() {
                refreshCount++
                if (refreshCount == 1) throw Exception("Network error")
                usersFlow.emit(listOf(makeUser(1)))
            }
            override suspend fun createUser(name: String, email: String) =
                User(99L, name, email, UserStatus.ACTIVE, Clock.System.now())
            override suspend fun deleteUser(id: Long) {}
        }
        val vm = UserListViewModel(
            getUsers = GetUsersUseCase(repo),
            refreshUsers = RefreshUsersUseCase(repo),
            deleteUser = DeleteUserUseCase(repo),
        )

        vm.state.test {
            var state = awaitItem()
            while (state !is UserListState.Error) {
                state = awaitItem()
            }
            assertIs<UserListState.Error>(state)

            vm.onAction(UserListAction.Refresh)

            state = awaitItem()
            while (state !is UserListState.Success) {
                state = awaitItem()
            }
            assertIs<UserListState.Success>(state)
            assertEquals(1, state.users.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeUser(id: Long, name: String = "User $id") =
        User(id, name, "user$id@test.com", UserStatus.ACTIVE, Clock.System.now())

    private fun makeViewModel(repo: FakeUserRepository): UserListViewModel {
        return UserListViewModel(
            getUsers = GetUsersUseCase(repo),
            refreshUsers = RefreshUsersUseCase(repo),
            deleteUser = DeleteUserUseCase(repo),
        )
    }

    private fun makeNonEmittingViewModel(
        refreshError: Exception? = null,
        onRefresh: suspend () -> Unit = { refreshError?.let { throw it } }
    ): UserListViewModel {
        val repo = object : UserRepository {
            override fun getUsers(): Flow<List<User>> = MutableSharedFlow()
            override suspend fun refreshUsers() = onRefresh()
            override suspend fun createUser(name: String, email: String) =
                User(99L, name, email, UserStatus.ACTIVE, Clock.System.now())
            override suspend fun deleteUser(id: Long) {}
        }
        return UserListViewModel(
            getUsers = GetUsersUseCase(repo),
            refreshUsers = RefreshUsersUseCase(repo),
            deleteUser = DeleteUserUseCase(repo),
        )
    }
}

class FakeUserRepository : UserRepository {
    val users = MutableStateFlow<List<User>>(emptyList())
    var refreshError: Exception? = null
    var deleteError: Exception? = null
    val deletedIds = mutableListOf<Long>()

    override fun getUsers(): Flow<List<User>> = users

    override suspend fun refreshUsers() {
        refreshError?.let { throw it }
    }

    override suspend fun createUser(name: String, email: String): User =
        User(99L, name, email, UserStatus.ACTIVE, Clock.System.now())

    override suspend fun deleteUser(id: Long) {
        deleteError?.let { throw it }
        deletedIds += id
    }
}
