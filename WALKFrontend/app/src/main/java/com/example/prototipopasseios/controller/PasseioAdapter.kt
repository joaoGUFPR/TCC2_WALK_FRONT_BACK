package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.content.Context
import android.media.Image
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.ComunidadeAdapter.ComunidadeViewHolder
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import org.w3c.dom.Text

class PasseioAdapter(
    val passeios: List<PasseioComunidade>,
    private val context: Context,
    private val idComunidade: Int,
    private val click: (passeio: PasseioComunidade, position: Int) -> Unit,
    private val deleteClick: (passeio: PasseioComunidade, position: Int) -> Unit,
    private val imageClick: (passeio: PasseioComunidade, position: Int) -> Unit
) : RecyclerView.Adapter<PasseioAdapter.PasseioViewHolder>()  {

    inner class PasseioViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val passeioNome: TextView = itemView.findViewById(R.id.tVNomePerfilPasseio)
        val passeioDescricao: TextView = itemView.findViewById(R.id.tVDescricaoPasseio)
        val passeioHorario: TextView = itemView.findViewById(R.id.tVHorario)
        val passeioImagem: ImageView = itemView.findViewById(R.id.iVPerfilPasseio)
        val iconeMapa: ImageView = itemView.findViewById(R.id.ivMap) // Botão de localização
        val iconeBalao: ImageView = itemView.findViewById(R.id.ivBubble)
        val iVReticencia: ImageView = itemView.findViewById(R.id.iVReticencia)

        fun bind(passeio: PasseioComunidade) {
            passeioNome.text = passeio.nomePessoa
            passeioDescricao.text = passeio.descricaoPasseio
            passeioHorario.text = passeio.horario
            // Carrega a imagem com Glide se a URL estiver presente; caso contrário, usa um placeholder
            if (!passeio.imagem.isNullOrEmpty()) {
                Glide.with(context)
                    .load(passeio.imagem)
                    .into(passeioImagem)
            } else {
            }

            iVReticencia.setOnClickListener { view ->
                showPopupMenu(view, passeio)
            }

            // Listener para mostrar o popup ao clicar no ícone de mapa
            iconeMapa.setOnClickListener {
                showPopupLocalizacao(passeio)
            }

            iconeBalao.setOnClickListener {
                goToComentarioPasseioComunidade(passeio)
            }

            passeioImagem.setOnClickListener {
                imageClick(passeio, adapterPosition)
            }
        }

        private fun showPopupLocalizacao(passeio: PasseioComunidade) {
            val popupView = LayoutInflater.from(context).inflate(R.layout.dialog_localizacao, null)
            val tVLocalizacao: TextView = popupView.findViewById(R.id.tvLocaliza)
            tVLocalizacao.text = passeio.localizacao

            val popupWindow = android.widget.PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            val location = IntArray(2)
            iconeMapa.getLocationOnScreen(location)
            val x = location[0] - iconeMapa.height * 2
            val y = location[1] + iconeMapa.height
            popupWindow.showAtLocation(itemView.rootView, Gravity.NO_GRAVITY, x, y)
        }

        private fun goToComentarioPasseioComunidade(passeio: PasseioComunidade) {
            val fragment = ControllerComentarioPasseioComunidade()
            val bundle = Bundle()
            bundle.putParcelable("passeio", passeio)
            // Aqui, como o objeto passeio não contém o idComunidade,
            // usamos o valor passado para o adapter:
            bundle.putInt("idComunidade", idComunidade)
            bundle.putString("localizacao", passeio.localizacao)
            fragment.arguments = bundle

            val activity = context as AppCompatActivity
            val transaction = activity.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.frameLayout, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        private fun showPopupMenu(view: View, passeio: PasseioComunidade) {
            val popupMenu = PopupMenu(context, view)
            // Inflate do menu (pode criar um arquivo de menu semelhante ao das comunidades, ex.: menu_option_passeio.xml)
            popupMenu.inflate(R.menu.menu_passeio)  // Se o mesmo menu for usado, se adapte conforme necessário
            val activity = context as AppCompatActivity
            val vm = ViewModelProvider(activity).get(PessoaViewModel::class.java)
            val usuarioLogado = vm.pessoa.value?.usuario

            // Só exibe "Excluir" se for o criador do passeio
            popupMenu.menu.findItem(R.id.excluir)
                .isVisible = (usuarioLogado == passeio.usuario)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.excluir -> {
                        // Aciona o callback para exclusão
                        deleteClick(passeio, adapterPosition)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasseioViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_passeio, parent, false)
        return PasseioViewHolder(view)
    }

    override fun getItemCount(): Int {
        return passeios.size
    }

    override fun onBindViewHolder(holder: PasseioViewHolder, position: Int) {
        val passeio = passeios[position]
        holder.bind(passeio)
    }
}