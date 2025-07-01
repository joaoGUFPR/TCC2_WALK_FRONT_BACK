package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ChatPasseio
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerListarChat : Fragment() {
    private lateinit var pessoaViewModel: PessoaViewModel
    private val usuario: String?
        get() = pessoaViewModel.pessoa.value?.usuario

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chats: MutableList<ChatPasseio> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_listar_chat, container, false)

        recyclerView = view.findViewById(R.id.ListaChatsRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        chatAdapter = ChatAdapter(
            chats = chats,
            context = requireContext(),
            click = { chat, _ ->
                // Abre o chat
                val fragment = ControllerChatPasseio.newInstance(chat.idChat)
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            deleteClick = { chat, pos ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Chat")
                    .setMessage("Deseja realmente excluir este chat?")
                    .setPositiveButton("Sim") { _, _ ->
                        val admin = pessoaViewModel.pessoa.value?.usuario ?: return@setPositiveButton
                        viewLifecycleOwner.lifecycleScope.launch {
                            val resp = withContext(Dispatchers.IO) {
                                RetrofitClient.pessoaApiService.deleteChat(chat.idChat, admin)
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    chats.removeAt(pos)
                                    chatAdapter.notifyItemRemoved(pos)
                                    Toast.makeText(requireContext(), "Chat excluído", Toast.LENGTH_SHORT).show()
                                } else if (resp.code() == 403) {
                                    Toast.makeText(requireContext(), "Apenas o administrador pode excluir", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(requireContext(), "Erro ao excluir chat", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            editClick = { chat, _ ->
                // Passa só o idChat num Bundle
                val frag = ControllerManterChat().apply {
                    arguments = Bundle().apply {
                        putInt("idChat", chat.idChat)
                    }
                }
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            }
        )

        recyclerView.adapter = chatAdapter
        fetchChats()
        return view
    }

    private fun fetchChats() {
        if (usuario.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Usuário não informado", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getChatByUsuario(usuario!!)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val list = response.body().orEmpty()
                    chats.clear()
                    chats.addAll(list)
                    chatAdapter.notifyDataSetChanged()
                    if (list.isEmpty()) {
                        Toast.makeText(requireContext(), "Nenhum chat encontrado", Toast.LENGTH_LONG).show()
                    }
                } else {

                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
