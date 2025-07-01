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
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ControllerListarComunidadeFavorita : Fragment() {
    private lateinit var pessoaViewModel: PessoaViewModel
    private val usuario: String?
        get() = pessoaViewModel.pessoa.value?.usuario
    private lateinit var recyclerView: RecyclerView
    private lateinit var comunidadeAdapter: ComunidadeAdapter
    private var comunidades: MutableList<Comunidade> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recupera o usuário a partir dos argumentos passados para o fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_listar_comunidade_favorita, container, false)
        recyclerView = view.findViewById(R.id.ListaComunidadesFavoritasRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Instancia o adapter com os lambdas para tratamento de clique:
        // "favoriteClick" pode ser utilizado para desfavoritar ou tratar algum evento adicional,
        // "click" navega para a tela de detalhes ou gerenciamento da comunidade.
        comunidadeAdapter = ComunidadeAdapter(
            comunidades = comunidades,
            context = requireContext(),
            favoriteClick = { comunidade ->
                desfavoritarComunidade(comunidade)
            },
            click = { comunidade, _ ->
                val fragment = ControllerManterPasseio.newInstance("param1", "param2", comunidade.idComunidade)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            deleteClick = { comunidade, position ->
                // Exibe diálogo de confirmação
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

        fetchComunidadesFavoritas()
        return view
    }

    // Busca as comunidades favoritas utilizando o endpoint específico da API.
    private fun fetchComunidadesFavoritas() {
        if (usuario.isNullOrEmpty()) {

            context?.let {
                Toast.makeText(it, "Usuário não informado", Toast.LENGTH_SHORT).show()
            }
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Chama o método da API que retorna as comunidades favoritas, implementado no endpoint
                    RetrofitClient.pessoaApiService.getComunidadeFavoritaByUsuario(usuario!!)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val comunidadesFavoritas = response.body() ?: emptyList()
                    comunidades.clear()
                    comunidades.addAll(comunidadesFavoritas)
                    comunidadeAdapter.notifyDataSetChanged()
                    if (comunidadesFavoritas.isEmpty()) {

                    }
                } else {

                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
