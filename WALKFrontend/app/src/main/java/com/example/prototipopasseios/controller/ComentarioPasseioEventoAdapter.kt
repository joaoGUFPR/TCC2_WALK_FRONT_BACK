// ComentarioPasseioEventoAdapter.kt
package com.example.prototipopasseios.controller

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ComentarioPasseioEvento
import com.example.prototipopasseios.viewmodel.PessoaViewModel

class ComentarioPasseioEventoAdapter(
    val comentarios: List<ComentarioPasseioEvento>,
    private val context: Context,
    private val click: (comentario: ComentarioPasseioEvento, position: Int) -> Unit,
    private val deleteClick: (comentario: ComentarioPasseioEvento, position: Int) -> Unit,
    private val imageClick: (comentario: ComentarioPasseioEvento, position: Int) -> Unit
) : RecyclerView.Adapter<ComentarioPasseioEventoAdapter.ComentarioPasseioEventoViewHolder>() {

    init {
        setHasStableIds(true)
    }

    var tracker: SelectionTracker<Long>? = null
    var currentUserImageUrl: String? = null

    override fun getItemId(position: Int): Long {
        return comentarios[position].idComentarioPasseioEvento?.toLong() ?: position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComentarioPasseioEventoViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_comentario_comunidade, parent, false)
        return ComentarioPasseioEventoViewHolder(view)
    }

    override fun getItemCount(): Int = comentarios.size

    override fun onBindViewHolder(holder: ComentarioPasseioEventoViewHolder, position: Int) {
        val comentario = comentarios[position]
        val isSelected = tracker?.isSelected(getItemId(position)) ?: false
        holder.bind(comentario, isSelected)
        holder.itemView.setOnClickListener {
            click(comentario, position)
        }
    }

    inner class ComentarioPasseioEventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val autorTextView: TextView = itemView.findViewById(R.id.tVNomePerfilComentario)
        private val comentarioTextView: TextView = itemView.findViewById(R.id.tVDescricaoComentario)
        private val horarioTextView: TextView = itemView.findViewById(R.id.tVHorarioComentario)
        private val imagemPerfil: ImageView = itemView.findViewById(R.id.iVPerfilComentario)
        private val iVReticenciaComentario: ImageView = itemView.findViewById(R.id.iVReticenciaComentario)
        private val comentarioImagem: ImageView = itemView.findViewById(R.id.iVPerfilComentario)

        fun bind(comentario: ComentarioPasseioEvento, isSelected: Boolean) {
            autorTextView.text = comentario.nomePessoa
            comentarioTextView.text = comentario.descricaoComentario
            horarioTextView.text = comentario.horario

            val imageToLoad = if (!comentario.imagem.isNullOrEmpty()) comentario.imagem else currentUserImageUrl
            if (!imageToLoad.isNullOrEmpty()) {
                Glide.with(context)
                    .load(imageToLoad)
                    .into(imagemPerfil)
            } else {
            }

            itemView.setBackgroundColor(if (isSelected) Color.LTGRAY else Color.WHITE)

            iVReticenciaComentario.setOnClickListener { view ->
                showPopupMenu(view, comentario)
            }

            comentarioImagem.setOnClickListener {
                imageClick(comentario, adapterPosition)
            }
        }

        private fun showPopupMenu(view: View, comentario: ComentarioPasseioEvento) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.comentario_passeio_comunidade)
            val vm = ViewModelProvider(context as AppCompatActivity).get(PessoaViewModel::class.java)
            val usuarioLogado = vm.pessoa.value?.usuario

            popupMenu.menu.findItem(R.id.excluir)
                .isVisible = (usuarioLogado == comentario.usuario)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.excluir -> {
                        deleteClick(comentario, adapterPosition)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long? = this@ComentarioPasseioEventoAdapter.getItemId(adapterPosition)
            }
    }
}
