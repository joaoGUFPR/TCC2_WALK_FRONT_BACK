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
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerPasseios : Fragment() {

    private lateinit var pessoaViewModel: PessoaViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PasseioAdapter
    private val listaPasseios = mutableListOf<PasseioComunidade>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_controller_passeios, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]

        recyclerView = view.findViewById(R.id.ListaPasseiosRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PasseioAdapter(
            passeios     = listaPasseios,
            context      = requireContext(),
            idComunidade = 0,
            click        = { passeio, _ ->
                Toast.makeText(requireContext(),
                    "Clicou no passeio de ${passeio.nomePessoa}",
                    Toast.LENGTH_SHORT).show()
            },
            deleteClick = { passeio, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Passeio")
                    .setMessage("Deseja realmente excluir o passeio de ${passeio.nomePessoa}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                // Chama o endpoint DELETE em IO
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService.deletePasseio(passeio.idPasseioComunidade)
                                }
                                // Volta para Main para atualizar UI/VM
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        // 1) Atualiza o ViewModel para buscar o qtPasseios decrementado
                                        pessoaViewModel.reloadPessoa()

                                        // 2) Atualiza a lista local e notifica o Adapter
                                        this@ControllerPasseios.listaPasseios.removeAt(position)
                                        adapter.notifyItemRemoved(position)

                                        Toast.makeText(
                                            requireContext(),
                                            "Passeio excluído com sucesso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Falha ao excluir passeio",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Erro: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            imageClick   = { passeio, _ ->
                // implemente navegação se quiser
            }
        )
        recyclerView.adapter = adapter

        // Aguarda o usuário no ViewModel e então carrega apenas os passeios dele
        pessoaViewModel.pessoa.observe(viewLifecycleOwner) { pessoa ->
            if (pessoa != null) {
                fetchPasseiosDoUsuario(pessoa.usuario)
            } else {
                Toast.makeText(requireContext(),
                    "Usuário não logado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchPasseiosDoUsuario(usuario: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getPasseiosByUsuario(usuario)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val body = response.body().orEmpty()
                    listaPasseios.clear()
                    listaPasseios.addAll(body)
                    adapter.notifyDataSetChanged()
                    if (body.isEmpty()) {
                        Toast.makeText(requireContext(),
                            "Você não tem passeios ainda", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(),
                        "Erro ao carregar seus passeios (${response.code()})",
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Erro de rede: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
