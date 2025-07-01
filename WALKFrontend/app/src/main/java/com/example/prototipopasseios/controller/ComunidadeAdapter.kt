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

class ComunidadeAdapter(
    private val comunidades: List<Comunidade>,
    private val context: Context,
    private val favoriteClick: (comunidade: Comunidade) -> Unit,
    private val click: (comunidade: Comunidade, position: Int) -> Unit,
    private val deleteClick: (comunidade: Comunidade, position: Int) -> Unit,
    private val updateClick: (comunidade: Comunidade, position: Int) -> Unit
) : RecyclerView.Adapter<ComunidadeAdapter.ComunidadeViewHolder>() {

    inner class ComunidadeViewHolder(private val itemView: View): RecyclerView.ViewHolder(itemView) {
        val comunidadeNome: TextView = itemView.findViewById(R.id.tVNome)
        val comunidadeImagem: ImageView = itemView.findViewById(R.id.iVComunidade)
        val iVReticencia: ImageView =
            itemView.findViewById(R.id.iVReticencia) // Botão de reticências
        val ivStar: ImageView = itemView.findViewById(R.id.iVStar)
        fun bind(comunidade: Comunidade) {
            comunidadeNome.text = if (comunidade.name.isEmpty()) "Sem nome" else comunidade.name
            if (!comunidade.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(comunidade.imageUrl)
                    .into(comunidadeImagem)
            } else {
            }

            // Configura o clique no item inteiro
            itemView.setOnClickListener {
                click(comunidade, adapterPosition)
            }

            // Configura o clique no botão de reticências
            iVReticencia.setOnClickListener { view ->
                showPopupMenu(view, comunidade)
            }

            ivStar.setOnClickListener {
                favoriteClick(comunidade)
            }
        }

        // Método para exibir o PopupMenu
        // Dentro do método showPopupMenu da classe ComunidadeViewHolder:
        private fun showPopupMenu(view: View, comunidade: Comunidade) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.optiondescricao)
            val vm = ViewModelProvider(context as AppCompatActivity)
                .get(PessoaViewModel::class.java)
            val usuarioLogado = vm.pessoa.value?.usuario

            val isAdmin = usuarioLogado == comunidade.administratorUser
            popupMenu.menu.findItem(R.id.excluir).isVisible = isAdmin
            popupMenu.menu.findItem(R.id.atualizar).isVisible = isAdmin

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.descricao -> {
                        Toast.makeText(
                            context,
                            "Descrição de ${comunidade.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        val bottomSheet = ComunidadeBottomSheetFragment(
                            nomeAdministrador = comunidade.administrator
                                ?: "Administrador não definido",
                            user = comunidade.administratorUser ?: "Usuário não definido",
                            descricao = comunidade.descricao ?: "Sem descrição",
                            regras = comunidade.regras ?: "Sem regras"
                        )
                        bottomSheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            bottomSheet.tag
                        )
                        true
                    }

                    R.id.excluir -> {
                        deleteClick(comunidade, adapterPosition)
                        true
                    }

                    R.id.atualizar -> {
                        // Chama o callback para atualização
                        // Por exemplo, se você definir um novo callback chamado updateClick
                        updateClick(comunidade, adapterPosition)
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }
    }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComunidadeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ComunidadeViewHolder(view)
    }

    override fun getItemCount(): Int {
        return comunidades.size
    }

    override fun onBindViewHolder(holder: ComunidadeViewHolder, position: Int) {
        val comunidade = comunidades[position]
        holder.bind(comunidade)

    }
}
