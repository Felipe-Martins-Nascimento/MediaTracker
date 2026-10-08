package com.felipe.mediatracker

import com.felipe.mediatracker.data.DefaultMediaRepository
import com.felipe.mediatracker.domain.FormError
import com.felipe.mediatracker.ui.form.MediaFormViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val local = FakeLibraryDataSource()
    private val repository = DefaultMediaRepository(local, FakeBookRemoteDataSource())

    private fun newForm(itemId: String? = null) =
        MediaFormViewModel(itemId, repository, clock = { 42L }, currentYear = 2026)

    @Test
    fun invalidForm_showsErrors_andDoesNotSave() = runTest {
        val form = newForm()

        form.onSave()

        val errors = form.uiState.value.errors
        assertEquals(FormError.REQUIRED, errors.title)
        assertEquals(FormError.REQUIRED, errors.totalUnits)
        assertTrue(repository.library.first().isEmpty())
    }

    @Test
    fun typingInAField_clearsOnlyThatFieldError() = runTest {
        val form = newForm()
        form.onSave()

        form.onTitleChange("Dom Casmurro")

        assertNull(form.uiState.value.errors.title)
        assertEquals(FormError.REQUIRED, form.uiState.value.errors.totalUnits)
    }

    @Test
    fun numericFields_ignoreNonDigits() = runTest {
        val form = newForm()

        form.onTotalChange("12a3")
        form.onYearChange("20x26")

        assertEquals("123", form.uiState.value.input.totalUnits)
        assertEquals("2026", form.uiState.value.input.year)
    }

    @Test
    fun validForm_savesItem_andEmitsEvent() = runTest {
        val form = newForm()
        form.onTitleChange("  Vidas Secas ")
        form.onTotalChange("176")
        form.onRatingChange("9")

        form.onSave()

        val saved = repository.library.first().single()
        assertEquals("Vidas Secas", saved.title) // espaços removidos
        assertEquals(176, saved.totalUnits)
        assertEquals(9, saved.rating)
        assertEquals(42L, saved.addedAt)
    }

    @Test
    fun editing_loadsExistingItem_andKeepsProgress() = runTest {
        repository.save(sampleItem(id = "a", title = "Antigo", total = 100, current = 60))

        val form = newForm("a")
        assertEquals("Antigo", form.uiState.value.input.title)

        form.onTitleChange("Novo")
        form.onTotalChange("50") // total diminui: o progresso precisa caber no novo total
        form.onSave()

        val saved = repository.observeItem("a").first()!!
        assertEquals("Novo", saved.title)
        assertEquals(50, saved.totalUnits)
        assertEquals(50, saved.currentUnit)
    }

    @Test
    fun editingMissingItem_marksNotFound() = runTest {
        val form = newForm("nao-existe")
        assertTrue(form.uiState.value.notFound)
    }
}
