package com.example.prototipopasseios.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Empresa


class EmpresasAdapter(
    private val empresas: List<Empresa>,
    private val context: Context,
    private val onClick: (Empresa) -> Unit
) : RecyclerView.Adapter<EmpresasAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPerfil  = itemView.findViewById<ImageView>(R.id.iVPerfilAmigos)
        private val tvNome    = itemView.findViewById<TextView>(R.id.tVNomePerfilAmigo)
        private val tvDesc    = itemView.findViewById<TextView>(R.id.tVDescricaoAmigo)
        private val ivOptions = itemView.findViewById<ImageView>(R.id.iVReticenciaAmigo)

        fun bind(p: Empresa) {
            // Preenche nome e descrição
            tvNome.text = p.name
            tvDesc.text = p.description

            // Carrega imagem (ou placeholder)
            if (!p.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(p.imageUrl)
                    .circleCrop()
                    .into(ivPerfil)
            } else {
            }


            // Clique na foto: abre perfil do amigo
            ivPerfil.setOnClickListener {
                // Aqui passamos exatamente o campo `p.usuario` (que já inclui o "@")
                val frag = ControllerPerfilEmpresaSeguida.newInstance(p.usuario)
                (context as AppCompatActivity)
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_amigos, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = empresas.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(empresas[position])
    }
}
