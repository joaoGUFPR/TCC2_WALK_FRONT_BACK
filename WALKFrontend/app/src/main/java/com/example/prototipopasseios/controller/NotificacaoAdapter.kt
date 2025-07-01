package com.example.prototipopasseios.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Notificacao

class NotificacaoAdapter(
    private val context: Context,
    private val notificacoes: MutableList<Notificacao>,
    private val onAction: (notificacao: Notificacao, action: String, position: Int) -> Unit
) : RecyclerView.Adapter<NotificacaoAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView  = itemView.findViewById(R.id.tvNotificationText)
        private val tvTime: TextView  = itemView.findViewById(R.id.tvNotificationTime)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(notif: Notificacao, position: Int) {
            tvText.text = notif.descricao
            tvTime.text = notif.horario

            if (notif.lido) {
                // já lida: esconde botões e dá um tom mais apagado
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                itemView.alpha = 0.5f
            } else {
                btnAccept.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                itemView.alpha = 1f

                btnAccept.setOnClickListener {
                    onAction(notif, "ACCEPT", position)
                }
                btnReject.setOnClickListener {
                    onAction(notif, "REJECT", position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_notification, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = notificacoes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(notificacoes[position], position)
    }

    /**
     * Remove a notificação da lista e notifica o RecyclerView.
     */
    fun removeAt(position: Int) {
        notificacoes.removeAt(position)
        notifyItemRemoved(position)
    }
}
