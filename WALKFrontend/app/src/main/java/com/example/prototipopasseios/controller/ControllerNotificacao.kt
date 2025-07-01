package com.example.prototipopasseios.controller

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
import com.example.prototipopasseios.model.Notificacao
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class ControllerNotificacao : Fragment() {

    private lateinit var pessoaViewModel: PessoaViewModel
    private val listaNotificacoes = mutableListOf<Notificacao>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificacaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        return inflater.inflate(R.layout.fragment_controller_notificacao, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.ListaNotificacaoRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificacaoAdapter(requireContext(), listaNotificacoes) { notif, action, position ->
            responderNotificacao(notif, action, position)
        }
        recyclerView.adapter = adapter

        pessoaViewModel.pessoa.observe(viewLifecycleOwner) { pessoa ->
            pessoa?.usuario?.let { carregarNotificacoes(it) }
        }
    }

    private fun carregarNotificacoes(usuario: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getNotificacoes(usuario)
            }
            if (response.isSuccessful) {
                listaNotificacoes.clear()
                listaNotificacoes.addAll(response.body().orEmpty())
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(),
                    "Falha ao carregar notificações", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun responderNotificacao(
        notif: Notificacao,
        action: String,
        position: Int
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response: Response<Unit> = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.responderNotif(
                    notif.usuarioDestinado,
                    notif.usuarioRemetente,
                    mapOf("action" to action)
                )
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                if (response.isSuccessful) {
                    // 1) Atualiza contador de amigos no perfil
                    pessoaViewModel.pessoa.value?.usuario?.let { pessoaViewModel.buscarPessoa(it) }

                    // 2) Remove notificação da lista
                    adapter.removeAt(position)

                    Toast.makeText(
                        requireContext(),
                        "Notificação ${if (action == "ACCEPT") "aceita" else "recusada"}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Falha ao processar notificação",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
