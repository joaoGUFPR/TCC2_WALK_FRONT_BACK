package com.example.prototipopasseios.controller

import android.content.Context
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
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.viewmodel.EmpresaViewModel

class EventoAdapterADM(
    private val eventos: List<Evento>,
    private val context: Context,
    private val click: (evento: Evento, position: Int) -> Unit,
    private val deleteClick: (evento: Evento, position: Int) -> Unit,
    private val updateClick: (evento: Evento, position: Int) -> Unit
) : RecyclerView.Adapter<EventoAdapterADM.EventoViewHolderADM>() {

    inner class EventoViewHolderADM(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventoNome: TextView      = itemView.findViewById(R.id.tVNome)
        private val eventoImagem: ImageView   = itemView.findViewById(R.id.iVEvento)
        private val iVReticencia: ImageView   = itemView.findViewById(R.id.iVReticencia)
        private val ivStar: ImageView         = itemView.findViewById(R.id.iVStarEvento)

        fun bind(evento: Evento) {
            eventoNome.text = evento.name.ifEmpty { "Sem nome" }
            if (!evento.imageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(evento.imageUrl)
                    .into(eventoImagem)
            } else {
            }

            itemView.setOnClickListener {
                click(evento, adapterPosition)
            }

            iVReticencia.setOnClickListener { view ->
                showPopupMenu(view, evento)
            }
        }

        private fun showPopupMenu(view: View, evento: Evento) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menu_evento_adm)

            // Usa apenas o EmpresaViewModel para saber qual empresa está logada
            val vm = ViewModelProvider(context as AppCompatActivity)
                .get(EmpresaViewModel::class.java)
            val empresaLogada = vm.empresa.value?.usuario

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.descricao -> {
                        // BottomSheet de detalhes
                        val bottomSheet = EventoBottomSheetFragment(
                            nomeEmpresa = evento.name,
                            user        = evento.administratorUser,
                            descricao   = evento.descricao,
                            dataEvento  = evento.dataEvento,
                            localEvento = evento.local
                        )
                        bottomSheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            bottomSheet.tag
                        )
                        true
                    }
                    R.id.excluir -> {
                        deleteClick(evento, adapterPosition)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolderADM {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_evento, parent, false)
        return EventoViewHolderADM(view)
    }

    override fun getItemCount(): Int = eventos.size

    override fun onBindViewHolder(holder: EventoViewHolderADM, position: Int) {
        holder.bind(eventos[position])
    }
}
