package com.example.prototipopasseios.controller

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ChatPasseio

class ChatAdapter(
    private val chats: List<ChatPasseio>,
    private val context: Context,
    private val click: (chat: ChatPasseio, position: Int) -> Unit,
    private val deleteClick: (chat: ChatPasseio, position: Int) -> Unit,
    private val editClick: (chat: ChatPasseio, position: Int) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tVChat: TextView = itemView.findViewById(R.id.tVChat)
        private val tvNamePasseio: TextView = itemView.findViewById(R.id.tvNamePasseio)
        private val iVReticenciaChat: ImageView = itemView.findViewById(R.id.iVReticenciasChat)

        fun bind(chat: ChatPasseio) {
            // Exemplo de texto fixo ou você pode usar chat.titulo se tiver
            tVChat.text = "Chat"
            tvNamePasseio.text = chat.nome

            itemView.setOnClickListener {
                click(chat, adapterPosition)
            }

            iVReticenciaChat.setOnClickListener { view ->
                showPopupMenu(view, chat)
            }
        }

        private fun showPopupMenu(anchor: View, chat: ChatPasseio) {
            PopupMenu(context, anchor).apply {
                inflate(R.menu.menu_chat)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.excluirChat -> {
                            deleteClick(chat, adapterPosition)
                            true
                        }
                        R.id.editarChat -> {
                            editClick(chat, adapterPosition)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.list_item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size
}
