// ControllerManterChat.kt
package com.example.prototipopasseios.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ChatPasseio
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerManterChat : Fragment() {

    private var idChat: Int = 0
    private lateinit var tvTituloPasseio: TextView
    private lateinit var rvMembros: RecyclerView
    private lateinit var rvAmigos: RecyclerView
    private lateinit var btnSalvar: Button

    private val membrosList = mutableListOf<Pessoa>()
    private val amigosList  = mutableListOf<Pessoa>()

    private lateinit var membrosAdapter: MembroAdapter
    private lateinit var amigosAdapter: AmigoChatAdapter

    private lateinit var pessoaViewModel: PessoaViewModel
    private val usuarioLogado: String?
        get() = pessoaViewModel.pessoa.value?.usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idChat = arguments?.getInt("idChat") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_controller_manter_chat, container, false)

        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        tvTituloPasseio = root.findViewById(R.id.tvNamePasseio)
        rvMembros = root.findViewById(R.id.ListaMebros)
        rvAmigos  = root.findViewById(R.id.ListaComunidades)
        btnSalvar = root.findViewById(R.id.btnSalvarChat)

        rvMembros.layoutManager = LinearLayoutManager(requireContext())
        rvAmigos .layoutManager = LinearLayoutManager(requireContext())

        // Adapter para membros do chat
        membrosAdapter = MembroAdapter(membrosList) { usuario ->
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.removePessoaChat(idChat, usuario)
                }
                fetchLists()
            }
        }

        // Adapter para adicionar amigos ao chat
        amigosAdapter = AmigoChatAdapter(amigosList) { usuario ->
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.addPessoaChat(idChat, usuario)
                }
                fetchLists()
            }
        }

        rvMembros.adapter = membrosAdapter
        rvAmigos.adapter  = amigosAdapter

        btnSalvar.setOnClickListener {
            Toast.makeText(requireContext(), "Membros atualizados!", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }
        fetchChatDetails()
        fetchLists()
        return root
    }

    private fun fetchChatDetails() {
        val user = usuarioLogado ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getChatByUsuario(user)
            }
            if (resp.isSuccessful) {
                // procura o chat com o idChat correto
                resp.body()
                    ?.find { it.idChat == idChat }
                    ?.let { chat ->
                        tvTituloPasseio.text = chat.nome
                    }
            }
        }
    }


    private fun fetchLists() {
        val user = usuarioLogado ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) pega usernames dos membros
            val respM = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getChatMembers(idChat)
            }
            // 2) pega todos os amigos
            val respA = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getFriendPessoas(user)
            }
            if (!isAdded) return@launch

            if (respM.isSuccessful && respA.isSuccessful) {
                val membrosUsuarios = respM.body().orEmpty()  // List<String>
                val todosAmigos      = respA.body().orEmpty() // List<Pessoa>

                // 3) busca detalhes de cada membro
                val detalhesMembros = mutableListOf<Pessoa>()
                membrosUsuarios.forEach { username ->
                    val respPessoa = withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.getPessoa(username)
                    }
                    if (respPessoa.isSuccessful) {
                        respPessoa.body()?.let { detalhesMembros.add(it) }
                    }
                }

                // 4) atualiza adapter de membros
                membrosList.clear()
                membrosList.addAll(detalhesMembros)
                membrosAdapter.notifyDataSetChanged()

                // 5) atualiza adapter de amigos (aqueles que não são membros)
                amigosList.clear()
                amigosList.addAll(
                    todosAmigos.filter { it.usuario !in membrosUsuarios }
                )
                amigosAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "Falha ao carregar listas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(idChat: Int) = ControllerManterChat().apply {
            arguments = Bundle().apply { putInt("idChat", idChat) }
        }
    }
}
