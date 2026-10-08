package com.felipe.mediatracker.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.felipe.mediatracker.R
import com.felipe.mediatracker.databinding.ActivityViewsListBinding
import com.felipe.mediatracker.databinding.ItemViewsBookBinding

/** Tela 1 (Views XML): lista de livros em um RecyclerView. Tocar em um item abre a tela de detalhe. */
class ViewsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewsListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ViewsBookAdapter(ViewsMockData.books) { book -> openDetail(book) }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    /** Navegação com Intent explícita, passando o id do livro como dado extra. */
    private fun openDetail(book: ViewsBook) {
        val intent = Intent(this, ViewsDetailActivity::class.java)
            .putExtra(ViewsDetailActivity.EXTRA_BOOK_ID, book.id)
        startActivity(intent)
    }
}

class ViewsBookAdapter(
    private val books: List<ViewsBook>,
    private val onBookClick: (ViewsBook) -> Unit,
) : RecyclerView.Adapter<ViewsBookAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemViewsBookBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ItemViewsBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        val context = holder.itemView.context

        holder.binding.title.text = book.title
        // Campo opcional: usa um texto padrão quando não há autor.
        holder.binding.author.text = book.author ?: context.getString(R.string.views_author_unknown)
        holder.binding.pages.text = context.getString(R.string.views_pages_format, book.totalPages)
        holder.binding.root.setOnClickListener { onBookClick(book) }
    }

    override fun getItemCount(): Int = books.size
}
