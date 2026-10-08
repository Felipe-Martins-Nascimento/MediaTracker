package com.felipe.mediatracker.views

/**
 * Modelo imutável das telas em Views. `author`, `synopsis` e `year` são opcionais (nullable)
 * para mostrar o tratamento de valores ausentes na interface.
 */
data class ViewsBook(
    val id: String,
    val title: String,
    val totalPages: Int,
    val author: String? = null,
    val synopsis: String? = null,
    val year: Int? = null,
)

/** Dados simulados (mocks): esta etapa não usa API nem banco de dados. */
object ViewsMockData {
    val books: List<ViewsBook> = listOf(
        ViewsBook(
            id = "mock-1",
            title = "Dom Casmurro",
            totalPages = 256,
            author = "Machado de Assis",
            synopsis = "Bento Santiago narra a própria vida e tenta decidir se Capitu o traiu com o melhor amigo.",
            year = 1899,
        ),
        ViewsBook(
            id = "mock-2",
            title = "O Cortiço",
            totalPages = 288,
            author = "Aluísio Azevedo",
            synopsis = "A vida em um cortiço do Rio de Janeiro e a ambição de seus moradores e do dono.",
            year = 1890,
        ),
        ViewsBook(
            id = "mock-3",
            title = "Vidas Secas",
            totalPages = 176,
            author = "Graciliano Ramos",
            synopsis = "Uma família de retirantes tenta sobreviver à seca no sertão nordestino.",
            year = 1938,
        ),
        // Sem autor, sinopse e ano: exercita os campos opcionais.
        ViewsBook(id = "mock-4", title = "Livro sem informações", totalPages = 120),
    )

    fun findById(id: String?): ViewsBook? = books.firstOrNull { it.id == id }
}
