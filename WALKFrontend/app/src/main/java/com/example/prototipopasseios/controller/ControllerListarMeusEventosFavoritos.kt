package com.example.prototipopasseios.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Comunidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ControllerListarEventosFavoritos : Fragment() {
    private lateinit var pessoaViewModel: PessoaViewModel
    private val usuario: String?
        get() = pessoaViewModel.pessoa.value?.usuario
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventoAdapter
    private var eventos: MutableList<Evento> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recupera o usuário a partir dos argumentos passados para o fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_listar_meus_eventos_favoritos, container, false)
        recyclerView = view.findViewById(R.id.listarMeusEventosFavoritosRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Instancia o adapter com os lambdas para tratamento de clique:
        // "favoriteClick" pode ser utilizado para desfavoritar ou tratar algum evento adicional,
        // "click" navega para a tela de detalhes ou gerenciamento da comunidade.
        adapter = EventoAdapter(
            eventos = eventos,
            context = requireContext(),

            // Clique simples: abre BottomSheet de detalhes
            click = { evento, position ->
                goNextActivityEvento(position, ArrayList(eventos))
            },

            // deleteClick: confirma exclusão, chama API e atualiza lista
            deleteClick = { evento, position ->
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Evento")
                    .setMessage("Deseja realmente excluir o evento “${evento.name}”?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    pessoaApiService.deleteEvento(evento.idEvento)
                                }
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Evento excluído com sucesso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        eventos.removeAt(position)
                                        adapter.notifyItemRemoved(position)
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Falha ao excluir evento (código ${response.code()})",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Erro ao excluir: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },

            // updateClick: abre fragment de edição, passando o Evento no Bundle
            updateClick = { evento, position ->
                val fragment = ControllerManterEvento.newInstance(evento)
                val bundle = Bundle().apply {
                    putParcelable("eventoParaEditar", evento)
                }
                fragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            favoriteClick = { evento ->
                val usuario = pessoaViewModel.pessoa.value?.usuario
                if (usuario.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                    return@EventoAdapter
                }
                val requestUsuario = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestIdEvento = evento.idEvento.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.favoritarEventos(requestUsuario, requestIdEvento)
                        }
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Comunidade favoritada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Falha ao favoritar comunidade", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        recyclerView.adapter = adapter

        fetchComunidadesFavoritas()
        return view
    }

    // Busca as comunidades favoritas utilizando o endpoint específico da API.
    private fun fetchComunidadesFavoritas() {
        if (usuario.isNullOrEmpty()) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Chama o método da API que retorna as comunidades favoritas, implementado no endpoint
                    RetrofitClient.pessoaApiService.getEventosFavoritosByUsuario(usuario!!)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val eventosFavoritos = response.body() ?: emptyList()
                    eventos.clear()
                    eventos.addAll(eventosFavoritos)
                    adapter.notifyDataSetChanged()
                    if (eventosFavoritos.isEmpty()) {
                    }
                } else {

                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goNextActivityEvento(position: Int, eventos: ArrayList<Evento>) {
        val selectedEvento = eventos[position]
        val fragment = ControllerManterPasseioEvento.newInstance("","", selectedEvento.idEvento)
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }


    // Exemplo de método para desfavoritar uma comunidade (opcional, conforme a lógica do app)
    private fun desfavoritarComunidade(comunidade: Comunidade) {
        // Aqui seria implementada a chamada para desfavoritar a comunidade, se esse comportamento for necessário.
        // Como exemplo, apenas exibe um Toast.
        Toast.makeText(requireContext(), "Desfavoritar comunidade: ${comunidade.name}", Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun newInstance(usuario: String) =
            ControllerListarComunidadeFavorita().apply {
                arguments = Bundle().apply {
                    putString("usuario", usuario)
                }
            }
    }
}
