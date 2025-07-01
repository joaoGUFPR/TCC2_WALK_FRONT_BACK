package com.example.prototipopasseios.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Pessoa

class UsuarioADMAdapter(
    private val usuarios: List<Pessoa>,
    private val context: Context,
    private val deleteClick: (Pessoa, Int) -> Unit
) : RecyclerView.Adapter<UsuarioADMAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.iVUsuarioADM)
        private val nome:   TextView  = itemView.findViewById(R.id.tVNomeUsuarioADM)
        private val desc:   TextView  = itemView.findViewById(R.id.tVDescricaoUsuarioADM)
        private val del:    ImageView = itemView.findViewById(R.id.iVDeleteUsuarioADM)

        fun bind(p: Pessoa) {
            // exibe imagem de perfil (ou placeholder)
            if (!p.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(p.imageUrl)
                    .into(avatar)
            } else {
            }
            nome.text = p.name
            desc.text = p.description

            del.setOnClickListener {
                deleteClick(p, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_usuario_adm, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(usuarios[position])
    }

    override fun getItemCount(): Int = usuarios.size
}
