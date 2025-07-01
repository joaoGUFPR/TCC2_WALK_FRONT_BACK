package com.example.prototipopasseios.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ComentarioChat

class ComentarioChatAdapter(
    val messages: MutableList<ComentarioChat>,
    private val currentUser: String,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_OUTGOING = 1
        const val VIEW_TYPE_INCOMING = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        // Compara o campo "usuario" com o currentUser (ignora espaços e caixa)
        return if (message.usuario.trim().equals(currentUser.trim(), ignoreCase = true))
            VIEW_TYPE_OUTGOING
        else
            VIEW_TYPE_INCOMING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_OUTGOING) {
            val view = LayoutInflater.from(context).inflate(R.layout.list_item_chat_right, parent, false)
            OutgoingChatViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.list_item_chat_left, parent, false)
            IncomingChatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is OutgoingChatViewHolder) {
            holder.bind(message)
        } else if (holder is IncomingChatViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class OutgoingChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        fun bind(message: ComentarioChat) {
            tvName.text = message.nomePessoa
            tvMessage.text = message.descricaoComentario
            tvTime.text = message.horario
        }
    }

    inner class IncomingChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        fun bind(message: ComentarioChat) {
            tvName.text = message.nomePessoa
            tvMessage.text = message.descricaoComentario
            tvTime.text = message.horario
        }
    }
}
