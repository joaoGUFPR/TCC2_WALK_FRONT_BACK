// MembroAdapter.kt
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
 * Adapter para listar os membros já vinculados ao chat.
 * Usa o XML list_item_membros_chat.xml, que contém:
 *   - ImageView @+id/iVPerfilMembrosChat
 *   - TextView  @+id/tVNomePerfilChat
 *   - ImageView @+id/iVDelete
 */
class MembroAdapter(
    private val items: List<Pessoa>,
    private val onRemoveClick: (usuario: String) -> Unit
) : RecyclerView.Adapter<MembroAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar  = view.findViewById<ImageView>(R.id.iVPerfilMembrosChat)
        private val tvName    = view.findViewById<TextView>(R.id.tVNomePerfilChat)
        private val tvDesc   = view.findViewById<TextView>(R.id.tVDescricaoChat)
        private val ivDelete  = view.findViewById<ImageView>(R.id.iVDelete)

        fun bind(p: Pessoa) {
            tvName.text = p.name
            tvDesc.text = p.description
            if (!p.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView).load(p.imageUrl).into(ivAvatar)
            }
            ivDelete.setOnClickListener {
                onRemoveClick(p.usuario)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_membros_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
