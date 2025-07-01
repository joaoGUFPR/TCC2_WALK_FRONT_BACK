package com.example.prototipopasseios.controller

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.PasseioEvento
import com.example.prototipopasseios.viewmodel.PessoaViewModel

class PasseioEventoAdapter(
    private val passeios: List<PasseioEvento>,
    private val context: Context,
    private val idEvento: Int,
    private val click: (passeio: PasseioEvento, position: Int) -> Unit,
    private val deleteClick: (passeio: PasseioEvento, position: Int) -> Unit,
    private val imageClick: (passeio: PasseioEvento, position: Int) -> Unit
) : RecyclerView.Adapter<PasseioEventoAdapter.PasseioEventoViewHolder>() {

    inner class PasseioEventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Use EXATAMENTE os IDs que estão no seu list_item_passeio_evento.xml:
        private val passeioNome: TextView =
            itemView.findViewById(R.id.tVNomePerfilPasseioEvento)
        private val passeioDescricao: TextView =
            itemView.findViewById(R.id.tVDescricaoPasseioEvento)
        private val passeioHorario: TextView =
            itemView.findViewById(R.id.tVHorarioEvento)
        private val passeioImagem: ImageView =
            itemView.findViewById(R.id.iVPerfilPasseioEvento)
        private val iconeBalao: ImageView =
            itemView.findViewById(R.id.ivBubbleEvento)
        private val iVReticencia: ImageView =
            itemView.findViewById(R.id.iVReticenciaEvento)

        fun bind(passeio: PasseioEvento) {
            passeioNome.text = passeio.nomePessoa
            passeioDescricao.text = passeio.descricaoPasseio
            passeioHorario.text = passeio.horario

            if (!passeio.imagem.isNullOrEmpty()) {
                Glide.with(context)
                    .load(passeio.imagem)
                    .into(passeioImagem)
            } else {
            }

            // Reticência (“⋮”) abre o menu de excluir somente se for o criador
            iVReticencia.setOnClickListener { view ->
                showPopupMenu(view, passeio)
            }

            // Balão de comentário
            iconeBalao.setOnClickListener {
                goToComentarioPasseioEvento(passeio)
            }

            // Clique na imagem leva ao perfil
            passeioImagem.setOnClickListener {
                imageClick(passeio, adapterPosition)
            }
        }

        // Dentro de PasseioEventoAdapter.goToComentarioPasseioEvento(...)
        private fun goToComentarioPasseioEvento(passeio: PasseioEvento) {
            val fragment = ControllerComentarioPasseioEvento()

            val bundle = Bundle().apply {
                putParcelable("passeio", passeio)      // passa o próprio objeto
                putInt("idEvento", idEvento)           // usa LITERAL "idEvento", não ARG_ID_EVENTO
            }
            fragment.arguments = bundle

            (context as AppCompatActivity).supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit()
        }

        private fun showPopupMenu(view: View, passeio: PasseioEvento) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menu_passeio)
            val activity = context as AppCompatActivity
            val vm = ViewModelProvider(activity).get(PessoaViewModel::class.java)
            val usuarioLogado = vm.pessoa.value?.usuario

            // só mostra “Excluir” se quem está logado for o criador do passeio
            popupMenu.menu.findItem(R.id.excluir).isVisible = (usuarioLogado == passeio.usuario)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.excluir) {
                    deleteClick(passeio, adapterPosition)
                    true
                } else {
                    false
                }
            }
            popupMenu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasseioEventoViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_passeio_evento, parent, false)
        return PasseioEventoViewHolder(view)
    }

    override fun getItemCount(): Int = passeios.size

    override fun onBindViewHolder(holder: PasseioEventoViewHolder, position: Int) {
        holder.bind(passeios[position])
    }
}
