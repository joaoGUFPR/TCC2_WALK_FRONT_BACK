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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.viewmodel.PessoaViewModel

class ControllerListarMinhasComunidades : Fragment() {
    private lateinit var pessoaViewModel: PessoaViewModel
    private val usuario: String?
        get() = pessoaViewModel.pessoa.value?.usuario
    private lateinit var recyclerView: RecyclerView
    private lateinit var comunidadeAdapter: ComunidadeAdapter
    private var comunidades: MutableList<Comunidade> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Supondo que o usuário logado seja passado como argumento deste fragmento.
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_listar_minhas_comunidades, container, false)
        recyclerView = view.findViewById(R.id.ListaMinhasComunidadesRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Instancia o adapter com os dois lambdas:
        // favoriteClick: para tratar o clique no botão "star" (favoritar)
        // click: para tratar o clique no item, que navega para a tela de gerenciamento de passeio.
        comunidadeAdapter = ComunidadeAdapter(
            comunidades = comunidades,
            context = requireContext(),
            favoriteClick = { comunidade ->
                favoritarComunidade(comunidade)
            },
            click = { comunidade, _ ->
                val fragment = ControllerManterPasseio.newInstance("param1", "param2", comunidade.idComunidade)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            deleteClick = { comunidade, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Comunidade")
                    .setMessage("Deseja realmente excluir a comunidade ${comunidade.name}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService.deleteComunidade(comunidade.idComunidade)
                                }
                                if (response.isSuccessful) {
                                    Toast.makeText(requireContext(), "Comunidade excluída", Toast.LENGTH_SHORT).show()
                                    comunidades.removeAt(position)
                                    comunidadeAdapter.notifyItemRemoved(position)
                                } else {
                                    Toast.makeText(requireContext(), "Falha ao excluir comunidade", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            updateClick = { comunidade, position ->
                // Navega para o fragmento de manutenção/edição, passando os dados da comunidade
                val fragment = ControllerManterComunidade.newInstance("param1", "param2")
                // Adiciona a comunidade no bundle para edição
                val bundle = Bundle()
                bundle.putParcelable("comunidade", comunidade)
                fragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        recyclerView.adapter = comunidadeAdapter

        fetchComunidades()
        return view
    }

    // Busca as comunidades utilizando o endpoint getComunidadesByUsuario da API
    private fun fetchComunidades() {
        if (usuario.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Usuário não informado", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getComunidadesByUsuario(usuario!!)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val minhasComunidades = response.body() ?: emptyList()
                    comunidades.clear()
                    comunidades.addAll(minhasComunidades)
                    comunidadeAdapter.notifyDataSetChanged()
                    if (minhasComunidades.isEmpty()) {
                        Toast.makeText(requireContext(), "Nenhuma comunidade encontrada", Toast.LENGTH_LONG).show()
                    }
                } else {

                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Chamada para favoritar uma comunidade
    private fun favoritarComunidade(comunidade: Comunidade) {
        val usuarioBody = usuario!!.toRequestBody("text/plain".toMediaTypeOrNull())
        val idComunidadeBody = comunidade.idComunidade.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.favoritarComunidade(usuarioBody, idComunidadeBody)
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Comunidade favoritada com sucesso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Erro ao favoritar comunidade", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(usuario: String) =
            ControllerListarMinhasComunidades().apply {
                arguments = Bundle().apply {
                    putString("usuario", usuario)
                }
            }
    }
}
