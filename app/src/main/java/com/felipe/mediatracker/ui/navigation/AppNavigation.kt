package com.felipe.mediatracker.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.felipe.mediatracker.ui.detail.DetailRoute
import com.felipe.mediatracker.ui.detail.DetailViewModel
import com.felipe.mediatracker.ui.detailViewModelFactory
import com.felipe.mediatracker.ui.form.MediaFormRoute
import com.felipe.mediatracker.ui.form.MediaFormViewModel
import com.felipe.mediatracker.ui.formViewModelFactory
import com.felipe.mediatracker.ui.library.LibraryRoute
import com.felipe.mediatracker.ui.library.LibraryViewModel
import com.felipe.mediatracker.ui.libraryViewModelFactory
import com.felipe.mediatracker.ui.rememberRepository
import com.felipe.mediatracker.ui.search.SearchRoute
import com.felipe.mediatracker.ui.search.SearchViewModel
import com.felipe.mediatracker.ui.searchViewModelFactory
import com.felipe.mediatracker.views.ViewsListActivity
import kotlinx.serialization.Serializable

// Rotas do app (Navigation 3). Cada rota é um NavKey @Serializable, o que permite salvar o back stack
// e restaurá-lo depois de rotação ou de o sistema encerrar o processo.

@Serializable
data object LibraryKey : NavKey

@Serializable
data object SearchKey : NavKey

/** Argumento da rota: o id do item a exibir. */
@Serializable
data class DetailKey(val id: String) : NavKey

/** `id == null` cria um item novo; com id, edita o item existente. */
@Serializable
data class FormKey(val id: String? = null) : NavKey

@Composable
fun AppNavigation() {
    val repository = rememberRepository()
    val context = LocalContext.current
    val backStack = rememberNavBackStack(LibraryKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Guarda o estado de rememberSaveable de cada entrada e escopa ViewModels por entrada:
            // ao sair da tela (pop) o ViewModel é descartado.
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<LibraryKey> {
                val viewModel: LibraryViewModel = viewModel(factory = libraryViewModelFactory(repository))
                LibraryRoute(
                    viewModel = viewModel,
                    onOpenItem = { id -> backStack.add(DetailKey(id)) },
                    onOpenSearch = { backStack.add(SearchKey) },
                    onAddManual = { backStack.add(FormKey()) },
                    onOpenViewsDemo = {
                        context.startActivity(Intent(context, ViewsListActivity::class.java))
                    },
                )
            }

            entry<SearchKey> {
                val viewModel: SearchViewModel = viewModel(factory = searchViewModelFactory(repository))
                SearchRoute(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onOpenItem = { id -> backStack.add(DetailKey(id)) },
                )
            }

            entry<DetailKey> { key ->
                val viewModel: DetailViewModel = viewModel(
                    key = "detail-${key.id}",
                    factory = detailViewModelFactory(key.id, repository),
                )
                DetailRoute(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { id -> backStack.add(FormKey(id)) },
                )
            }

            entry<FormKey> { key ->
                val viewModel: MediaFormViewModel = viewModel(
                    key = "form-${key.id ?: "new"}",
                    factory = formViewModelFactory(key.id, repository),
                )
                MediaFormRoute(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onSaved = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
