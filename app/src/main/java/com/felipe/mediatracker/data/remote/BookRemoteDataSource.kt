package com.felipe.mediatracker.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Fonte de dados remota de livros. Interface para permitir fakes nos testes. */
interface BookRemoteDataSource {
    /**
     * Busca livros pelo texto informado.
     * @throws IOException em falha de rede ou resposta HTTP diferente de 2xx.
     */
    suspend fun searchBooks(query: String): List<BookDto>
}

/**
 * Implementação usando a API pública da Open Library (não exige chave nem cadastro).
 * Usa apenas `HttpURLConnection` + `kotlinx.serialization`, sem bibliotecas HTTP extras.
 */
class OpenLibraryDataSource(
    private val json: Json,
    private val baseUrl: String = "https://openlibrary.org",
) : BookRemoteDataSource {

    override suspend fun searchBooks(query: String): List<BookDto> {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl/search.json?q=$encodedQuery&limit=$PAGE_SIZE&fields=$FIELDS"
        // runInterruptible: se a coroutine for cancelada (ex.: nova busca), a thread bloqueada é interrompida.
        val body = runInterruptible(Dispatchers.IO) { httpGet(url) }
        return json.decodeFromString(BookSearchResponseDto.serializer(), body).docs
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("HTTP $status")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT_MS = 10_000
        const val FIELDS = "key,title,author_name,first_publish_year,number_of_pages_median,cover_i"
        const val USER_AGENT = "MediaTracker/1.0 (projeto academico UNAERP)"
    }
}
