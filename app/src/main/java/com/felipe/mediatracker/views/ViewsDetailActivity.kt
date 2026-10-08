package com.felipe.mediatracker.views

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.felipe.mediatracker.R
import com.felipe.mediatracker.databinding.ActivityViewsDetailBinding
import kotlin.math.roundToInt

/**
 * Tela 2 (Views XML): detalhe do livro recebido por Intent.
 * Interação: o botão "Li mais 10 páginas" atualiza o texto e a barra de progresso.
 */
class ViewsDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        private const val STATE_PAGES_READ = "state_pages_read"
        private const val PAGES_PER_CLICK = 10
    }

    private lateinit var binding: ActivityViewsDetailBinding
    private var book: ViewsBook? = null
    private var pagesRead = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Dado recebido pela Intent. Se o id for inválido, encerra a tela em vez de quebrar.
        book = ViewsMockData.findById(intent.getStringExtra(EXTRA_BOOK_ID))
        val currentBook = book ?: run {
            finish()
            return
        }

        // Restaura o progresso após rotação.
        pagesRead = savedInstanceState?.getInt(STATE_PAGES_READ, 0) ?: 0

        bindBook(currentBook)
        binding.readMoreButton.setOnClickListener {
            pagesRead = (pagesRead + PAGES_PER_CLICK).coerceAtMost(currentBook.totalPages)
            updateProgress(currentBook)
        }
        binding.resetButton.setOnClickListener {
            pagesRead = 0
            updateProgress(currentBook)
        }
        updateProgress(currentBook)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGES_READ, pagesRead)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun bindBook(book: ViewsBook) {
        binding.title.text = book.title
        binding.author.text = book.author ?: getString(R.string.views_author_unknown)
        binding.synopsis.text = book.synopsis ?: getString(R.string.views_synopsis_missing)

        // Ano é opcional: o campo some quando não existe.
        binding.year.isVisible = book.year != null
        book.year?.let { binding.year.text = getString(R.string.views_year_format, it) }
    }

    private fun updateProgress(book: ViewsBook) {
        val percent = if (book.totalPages > 0) {
            (pagesRead.toFloat() / book.totalPages * 100).roundToInt()
        } else {
            0
        }
        binding.progressText.text = getString(R.string.views_read_pages_format, pagesRead, book.totalPages, percent)
        binding.progressBar.progress = percent
        binding.status.text = getString(
            when {
                pagesRead >= book.totalPages -> R.string.views_finished
                pagesRead > 0 -> R.string.views_in_progress
                else -> R.string.views_not_started
            }
        )
        binding.readMoreButton.isEnabled = pagesRead < book.totalPages
        binding.resetButton.isEnabled = pagesRead > 0
    }
}
