package com.felipe.mediatracker

import com.felipe.mediatracker.data.DefaultMediaRepository
import com.felipe.mediatracker.domain.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class DefaultMediaRepositoryTest {

    private var now = 1_000L
    private val local = FakeLibraryDataSource()
    private val remote = FakeBookRemoteDataSource(listOf(sampleBookDto()))

    private fun repository(ttl: Long = 60_000L) =
        DefaultMediaRepository(local = local, remote = remote, clock = { now }, cacheTtlMs = ttl)

    @Test
    fun searchBooks_mapsDtoToDomain() = runTest {
        val items = repository().searchBooks("casmurro")

        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("ol:/works/OL1W", item.id)
        assertEquals(MediaType.BOOK, item.type)
        assertEquals("Machado de Assis", item.creator)
        assertEquals(256, item.totalUnits)
        assertTrue(item.coverUrl!!.endsWith("123-M.jpg"))
    }

    @Test
    fun searchBooks_dropsResultsWithoutTitle() = runTest {
        remote.result = listOf(sampleBookDto(key = "a", title = null), sampleBookDto(key = "b", title = "  "))
        assertTrue(repository().searchBooks("x1").isEmpty())
    }

    @Test
    fun searchBooks_usesCacheUntilTtlExpires() = runTest {
        val repo = repository(ttl = 60_000L)

        repo.searchBooks("Casmurro")
        repo.searchBooks("  casmurro ") // mesma busca, normalizada
        assertEquals(1, remote.calls)

        now += 61_000L
        repo.searchBooks("casmurro")
        assertEquals(2, remote.calls)
    }

    @Test
    fun searchBooks_doesNotCacheFailures() = runTest {
        val repo = repository()
        remote.failWith = networkError()
        try {
            repo.searchBooks("abc")
            fail("Deveria lançar IOException")
        } catch (expected: IOException) {
            // esperado: a falha de rede chega até o chamador
        }

        remote.failWith = null
        assertEquals(1, repo.searchBooks("abc").size)
        assertEquals(2, remote.calls)
    }

    @Test
    fun searchBooks_rejectsBlankQuery() = runTest {
        try {
            repository().searchBooks("   ")
            fail("Deveria lançar IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // esperado: busca vazia não chega à rede
        }
        assertEquals(0, remote.calls)
    }

    @Test
    fun addToLibrary_isIdempotent() = runTest {
        val repo = repository()
        val item = sampleItem(id = "ol:1")

        repo.addToLibrary(item)
        now += 5_000L
        repo.addToLibrary(item) // segunda vez não duplica nem altera a data

        val library = repo.library.first()
        assertEquals(1, library.size)
        assertEquals(1_000L, library.single().addedAt)
    }

    @Test
    fun library_isSortedByMostRecentlyAdded() = runTest {
        val repo = repository()
        repo.addToLibrary(sampleItem(id = "old"))
        now += 10L
        repo.addToLibrary(sampleItem(id = "new"))

        assertEquals(listOf("new", "old"), repo.library.first().map { it.id })
    }

    @Test
    fun updateProgress_clampsAndPersists() = runTest {
        val repo = repository()
        repo.save(sampleItem(id = "a", total = 10))

        repo.updateProgress("a", 999)
        assertEquals(10, repo.observeItem("a").first()!!.currentUnit)
    }

    @Test
    fun delete_removesItem_andObserveReturnsNull() = runTest {
        val repo = repository()
        repo.save(sampleItem(id = "a"))
        repo.delete("a")

        assertNull(repo.observeItem("a").first())
    }
}
