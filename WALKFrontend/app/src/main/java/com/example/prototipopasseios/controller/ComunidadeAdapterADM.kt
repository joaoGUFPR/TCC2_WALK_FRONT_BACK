// ComunidadeAdapterADM.kt
package com.example.prototipopasseios.controller

import android.content.Context
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
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.viewmodel.PessoaViewModel

class ComunidadeAdapterADM(
    private val comunidades: List<Comunidade>,
    private val context: Context,
    private val click: (comunidade: Comunidade, position: Int) -> Unit,
    private val deleteClick: (comunidade: Comunidade, position: Int) -> Unit,
    private val updateClick: (comunidade: Comunidade, position: Int) -> Unit
) : RecyclerView.Adapter<ComunidadeAdapterADM.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgComunidade: ImageView = itemView.findViewById(R.id.iVComunidadeADM)
        private val star: ImageView          = itemView.findViewById(R.id.iVStarADM)
        private val nome: TextView           = itemView.findViewById(R.id.tVNomeADM)
        private val more: ImageView          = itemView.findViewById(R.id.iVReticenciaADM)

        fun bind(c: Comunidade) {
            nome.text = c.name
            if (!c.imageUrl.isNullOrEmpty()) {
                Glide.with(context).load(c.imageUrl).into(imgComunidade)
            } else {
            }

            // clique no card inteiro
            itemView.setOnClickListener { click(c, adapterPosition) }

            // sem listener na estrela!!!

            // menu de editar/excluir
            more.setOnClickListener { v ->
                PopupMenu(context, v).apply {
                    inflate(R.menu.optioncomunidadeadm) // descriçao, excluir, atualizar
                    setOnMenuItemClickListener { mi ->
                        when (mi.itemId) {
                            R.id.excluir -> {
                                deleteClick(c, adapterPosition)
                                true
                            }
                            R.id.descricao -> {
                                // Mostra um Toast e o BottomSheet com os detalhes
                                val bottomSheet = ComunidadeBottomSheetFragment(
                                    nomeAdministrador   = c.administrator ?: "Administrador não definido",
                                    user                = c.administratorUser ?: "Usuário não definido",
                                    descricao           = c.descricao ?: "Sem descrição",
                                    regras              = c.regras ?: "Sem regras"
                                )
                                bottomSheet.show(
                                    (context as AppCompatActivity).supportFragmentManager,
                                    bottomSheet.tag
                                )
                                true
                            }
                            R.id.excluir -> {
                                deleteClick(c, position)
                                true
                            }
                            R.id.atualizar -> {
                                updateClick(c, position)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    private fun showPopupMenu(anchor: View, comunidade: Comunidade, position: Int) {
        PopupMenu(context, anchor).apply {
            inflate(R.menu.optioncomunidadeadm) // deve conter "descricao", "excluir" e "atualizar"
            setOnMenuItemClickListener { mi ->
                when (mi.itemId) {
                    R.id.descricao -> {
                        // Apenas mostra um bottom sheet com os detalhes
                        val bottomSheet = ComunidadeBottomSheetFragment(
                            nomeAdministrador   = comunidade.administrator ?: "Administrador não definido",
                            user                = comunidade.administratorUser ?: "Usuário não definido",
                            descricao           = comunidade.descricao ?: "Sem descrição",
                            regras              = comunidade.regras ?: "Sem regras"
                        )
                        bottomSheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            bottomSheet.tag
                        )
                        true
                    }
                    R.id.excluir -> {
                        deleteClick(comunidade, position)
                        true
                    }
                    R.id.atualizar -> {
                        updateClick(comunidade, position)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(context)
            .inflate(R.layout.list_item_comunidade_adm, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(comunidades[position])
    }

    override fun getItemCount() = comunidades.size
}
