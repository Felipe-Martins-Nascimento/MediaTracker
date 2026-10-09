package com.felipe.mediatracker

import com.felipe.mediatracker.data.DefaultMediaRepository
import com.felipe.mediatracker.ui.search.SearchError
import com.felipe.mediatracker.ui.search.SearchState
import com.felipe.mediatracker.ui.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val local = FakeLibraryDataSource()
    private val remote = FakeBookRemoteDataSource(listOf(sampleBookDto()))
    // lazy: o ViewModel só nasce dentro do teste, depois que a regra já trocou o Dispatchers.Main.
    private val viewModel by lazy { SearchViewModel(DefaultMediaRepository(local, remote)) }

    // O uiState usa WhileSubscribed: só atualiza com alguém coletando.
    // Coletar com dispatcher "unconfined" faz a coleta começar na hora, antes do primeiro assert.
    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun initialState_isIdle() = runTest {
        collectUiState()
        assertEquals(SearchState.Idle, viewModel.uiState.value.state)
    }

    @Test
    fun shortQuery_doesNotHitTheNetwork() = runTest {
        collectUiState()

        viewModel.search("a")

        assertEquals(SearchState.QueryTooShort, viewModel.uiState.value.state)
        assertEquals(0, remote.calls)
    }

    @Test
    fun successfulSearch_emitsResults() = runTest {
        collectUiState()

        viewModel.search("casmurro")

        val state = viewModel.uiState.value.state
        assertTrue(state is SearchState.Results)
        assertEquals(1, (state as SearchState.Results).items.size)
    }

    @Test
    fun emptyResponse_emitsEmpty() = runTest {
        collectUiState()
        remote.result = emptyList()

        viewModel.search("zzzz")

        assertEquals(SearchState.Empty("zzzz"), viewModel.uiState.value.state)
    }

    @Test
    fun networkFailure_emitsNetworkError_andRetryRecovers() = runTest {
        collectUiState()
        remote.failWith = networkError()

        viewModel.search("casmurro")
        assertEquals(SearchState.Error(SearchError.NETWORK, "casmurro"), viewModel.uiState.value.state)

        remote.failWith = null
        viewModel.search("casmurro")
        assertTrue(viewModel.uiState.value.state is SearchState.Results)
    }

    @Test
    fun addedItem_isReportedAsSaved() = runTest {
        collectUiState()
        viewModel.search("casmurro")
        val item = (viewModel.uiState.value.state as SearchState.Results).items.single()

        viewModel.onAdd(item)

        assertTrue(item.id in viewModel.uiState.value.savedIds)
    }
}
