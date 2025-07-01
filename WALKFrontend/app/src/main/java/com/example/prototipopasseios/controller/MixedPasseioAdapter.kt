package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.PasseioEvento
import com.example.prototipopasseios.viewmodel.PessoaViewModel

sealed class PasseioItem {
    data class Comunidade(val passeio: PasseioComunidade) : PasseioItem()
    data class Evento(val passeio: PasseioEvento)         : PasseioItem()
}

class MixedPasseioAdapter(
    private val items: List<PasseioItem>,
    private val context: Context,
    private val onComunidadeClick:    (PasseioComunidade, Int) -> Unit,
    private val onComunidadeDelete:   (PasseioComunidade, Int) -> Unit,
    private val onComunidadeImage:    (PasseioComunidade, Int) -> Unit,
    private val onComunidadeComment:  (PasseioComunidade) -> Unit,
    private val onEventoClick:        (PasseioEvento, Int) -> Unit,
    private val onEventoDelete:       (PasseioEvento, Int) -> Unit,
    private val onEventoImage:        (PasseioEvento, Int) -> Unit,
    private val onEventoComment:      (PasseioEvento) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_COMUNIDADE = 0
        private const val TYPE_EVENTO     = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is PasseioItem.Comunidade -> TYPE_COMUNIDADE
            is PasseioItem.Evento     -> TYPE_EVENTO
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            TYPE_COMUNIDADE -> ComunidadeViewHolder(
                inflater.inflate(R.layout.list_item_passeio, parent, false)
            )
            TYPE_EVENTO -> EventoViewHolder(
                inflater.inflate(R.layout.list_item_passeio_evento, parent, false)
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            holder is ComunidadeViewHolder && items[position] is PasseioItem.Comunidade -> {
                holder.bind((items[position] as PasseioItem.Comunidade).passeio)
            }
            holder is EventoViewHolder && items[position] is PasseioItem.Evento -> {
                holder.bind((items[position] as PasseioItem.Evento).passeio)
            }
        }
    }

    private fun showPopupMenuComunidade(anchor: View, passeio: PasseioComunidade, pos: Int) {
        val popup = PopupMenu(context, anchor)
        popup.inflate(R.menu.menu_passeio)
        val activity = context as AppCompatActivity
        val vm = ViewModelProvider(activity).get(PessoaViewModel::class.java)
        val usuarioLogado = vm.pessoa.value?.usuario

        popup.menu.findItem(R.id.excluir).isVisible = usuarioLogado == passeio.usuario
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.excluir) {
                onComunidadeDelete(passeio, pos)
                true
            } else false
        }
        popup.show()
    }

    private fun showPopupMenuEvento(anchor: View, passeio: PasseioEvento, pos: Int) {
        val popup = PopupMenu(context, anchor)
        popup.inflate(R.menu.menu_passeio)
        val activity = context as AppCompatActivity
        val vm = ViewModelProvider(activity).get(PessoaViewModel::class.java)
        val usuarioLogado = vm.pessoa.value?.usuario

        popup.menu.findItem(R.id.excluir).isVisible = usuarioLogado == passeio.usuario
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.excluir) {
                onEventoDelete(passeio, pos)
                true
            } else false
        }
        popup.show()
    }

    inner class ComunidadeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nome        = itemView.findViewById<TextView>(R.id.tVNomePerfilPasseio)
        private val descricao   = itemView.findViewById<TextView>(R.id.tVDescricaoPasseio)
        private val horario     = itemView.findViewById<TextView>(R.id.tVHorario)
        private val imagem      = itemView.findViewById<ImageView>(R.id.iVPerfilPasseio)
        private val ivMap       = itemView.findViewById<ImageView>(R.id.ivMap)
        private val ivBubble    = itemView.findViewById<ImageView>(R.id.ivBubble)
        private val ivMenu      = itemView.findViewById<ImageView>(R.id.iVReticencia)

        fun bind(passeio: PasseioComunidade) {
            nome.text      = passeio.nomePessoa
            descricao.text = passeio.descricaoPasseio
            horario.text   = passeio.horario

            if (!passeio.imagem.isNullOrEmpty()) {
                Glide.with(context)
                    .load(passeio.imagem)
                    .into(imagem)
            } else {
            }

            // clique geral, delete e imagem
            itemView.setOnClickListener  { onComunidadeClick(passeio, adapterPosition) }
            ivMenu.setOnClickListener    { showPopupMenuComunidade(it, passeio, adapterPosition) }
            imagem.setOnClickListener    { onComunidadeImage(passeio, adapterPosition) }

            // balão de comentário
            ivBubble.setOnClickListener  { onComunidadeComment(passeio) }

            // mapa: mostra popup
            ivMap.setOnClickListener {
                val popupView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_localizacao, null)
                popupView.findViewById<TextView>(R.id.tvLocaliza).text = passeio.localizacao

                val popupWindow = android.widget.PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                // posicionamento próximo ao ícone
                val location = IntArray(2)
                ivMap.getLocationOnScreen(location)
                val x = location[0] - ivMap.width
                val y = location[1] + ivMap.height
                popupWindow.showAtLocation(itemView.rootView, Gravity.NO_GRAVITY, x, y)
            }
        }
    }

    inner class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nome        = itemView.findViewById<TextView>(R.id.tVNomePerfilPasseioEvento)
        private val descricao   = itemView.findViewById<TextView>(R.id.tVDescricaoPasseioEvento)
        private val horario     = itemView.findViewById<TextView>(R.id.tVHorarioEvento)
        private val imagem      = itemView.findViewById<ImageView>(R.id.iVPerfilPasseioEvento)
        private val ivBubble    = itemView.findViewById<ImageView>(R.id.ivBubbleEvento)
        private val ivMenu      = itemView.findViewById<ImageView>(R.id.iVReticenciaEvento)

        fun bind(passeio: PasseioEvento) {
            nome.text      = passeio.nomePessoa
            descricao.text = passeio.descricaoPasseio
            horario.text   = passeio.horario

            if (!passeio.imagem.isNullOrEmpty()) {
                Glide.with(context)
                    .load(passeio.imagem)
                    .into(imagem)
            } else {
            }

            // clique geral, delete e imagem
            itemView.setOnClickListener  { onEventoClick(passeio, adapterPosition) }
            ivMenu.setOnClickListener    { showPopupMenuEvento(it, passeio, adapterPosition) }
            imagem.setOnClickListener    { onEventoImage(passeio, adapterPosition) }

            // balão de comentário de evento
            ivBubble.setOnClickListener  { onEventoComment(passeio) }
        }


    }
}
