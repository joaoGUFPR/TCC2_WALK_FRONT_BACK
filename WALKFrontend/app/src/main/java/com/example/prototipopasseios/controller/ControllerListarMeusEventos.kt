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
import com.example.prototipopasseios.model.Evento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ControllerListarMeusEventos : Fragment() {
    private lateinit var empresaViewModel: EmpresaViewModel
    private val usuario: String?
        get() = empresaViewModel.empresa.value?.usuario

    private lateinit var recyclerView: RecyclerView
    private lateinit var eventoAdapter: EventoAdapter
    private lateinit var pessoaViewModel: PessoaViewModel
    private var eventos: MutableList<Evento> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        empresaViewModel = ViewModelProvider(requireActivity())[EmpresaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_listar_meus_eventos, container, false)
        recyclerView = view.findViewById(R.id.listarMeusEventosRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        // Inicializa o adapter corretamente:
        eventoAdapter = EventoAdapter(
            eventos = eventos,
            context = requireContext(),

            // click, deleteClick e updateClick conforme você já havia definido…
            click = { evento, position ->
                goNextActivityEvento(position, ArrayList(eventos))
            },
            deleteClick = { evento, position ->
                AlertDialog.Builder(requireContext())
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
                                        eventoAdapter.notifyItemRemoved(position)
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

        recyclerView.adapter = eventoAdapter

        fetchEventos()
        return view
    }

    private fun fetchEventos() {
        if (usuario.isNullOrEmpty()) {

            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    pessoaApiService.getEventosByUsuario(usuario!!)
                }
                if (!isAdded) return@launch

                if (response.isSuccessful) {
                    val eventosFromApi = response.body() ?: emptyList()
                    eventos.clear()
                    eventos.addAll(eventosFromApi)
                    // AVISO IMPORTANTE: use eventoAdapter, NÃO "adapter"
                    eventoAdapter.notifyDataSetChanged()

                    // (Opcional) mostrar um toast caso venha vazio:
                    if (eventosFromApi.isEmpty()) {

                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao obter eventos (código ${response.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goNextActivityEvento(position: Int, eventos: ArrayList<Evento>) {
        val selectedEvento = eventos[position]
        val fragment = ControllerManterPasseioEvento.newInstance("", "", selectedEvento.idEvento)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance(usuario: String) =
            ControllerListarMeusEventos().apply {
                arguments = Bundle().apply {
                    putString("usuario", usuario)
                }
            }
    }
}