// AmigoChatAdapter.kt
package com.example.prototipopasseios.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Pessoa

/**
 * Adapter para listar os amigos disponíveis para adicionar ao chat.
 * Usa o XML list_item_amigos_chat.xml, que contém:
 *   - ImageView @+id/iVPerfilAmigosChat
 *   - TextView  @+id/tVNomePerfilAChat
 *   - ImageView @+id/iVCheck
 */
class AmigoChatAdapter(
    private val items: List<Pessoa>,
    private val onAddClick: (usuario: String) -> Unit
) : RecyclerView.Adapter<AmigoChatAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar = view.findViewById<ImageView>(R.id.iVPerfilAmigosChat)
        private val tvName   = view.findViewById<TextView>(R.id.tVNomePerfilAChat)
        private val ivCheck  = view.findViewById<ImageView>(R.id.iVCheck)

        fun bind(p: Pessoa) {
            tvName.text = p.name
            if (!p.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView).load(p.imageUrl).into(ivAvatar)
            }
            ivCheck.setOnClickListener {
                onAddClick(p.usuario)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_amigos_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
