package com.example.prototipopasseios.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.app.AlertDialog

class ControllerManterPasseio : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var idComunidade: Int = 0

    private lateinit var adapter: PasseioAdapter
    private var passeios: MutableList<PasseioComunidade> = mutableListOf()
    private lateinit var passeioRV: RecyclerView
    private lateinit var pessoaViewModel: PessoaViewModel

    private lateinit var edtLocal: EditText
    private lateinit var etDescricaoPasseio: EditText
    private lateinit var btnCriarPasseio: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("param1")
            param2 = it.getString("param2")
            idComunidade = it.getInt("idComunidade", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_manter_passeio, container, false)
        passeioRV = view.findViewById(R.id.PasseioRV)
        edtLocal = view.findViewById(R.id.edtLocal)
        etDescricaoPasseio = view.findViewById(R.id.etDescricaoPasseio)
        btnCriarPasseio = view.findViewById(R.id.btnCriarPasseio)

        passeioRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = PasseioAdapter(passeios, requireContext(), idComunidade,
            click = { passeio, position ->
                // Ação de clique no item, se necessário.
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
                                if (!isAdded) return@launch
                                // Volta para Main para atualizar UI/VM
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        // 1) Atualiza o ViewModel para buscar o qtPasseios decrementado
                                        pessoaViewModel.reloadPessoa()

                                        // 2) Atualiza a lista local e notifica o Adapter
                                        this@ControllerManterPasseio.passeios.removeAt(position)
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
            imageClick = { passeio, _ ->
                val currentUser = pessoaViewModel.pessoa.value?.usuario
                if (passeio.usuario == currentUser) {
                    // navega para o próprio perfil
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerPerfil())
                        .addToBackStack(null)
                        .commit()
                } else {
                    // navega para perfil de amigo
                    val frag = ControllerPerfilAmigo.newInstance(passeio.usuario)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, frag)
                        .addToBackStack(null)
                        .commit()
                }
            }
        )
        passeioRV.adapter = adapter
        passeioRV.setHasFixedSize(true)

        pessoaViewModel = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)

        // Busque os passeios já cadastrados para esta comunidade
        fetchPasseios()

        btnCriarPasseio.setOnClickListener {
            createPasseio()
        }
        return view
    }

    private fun createPasseio() {
        val local = edtLocal.text.toString().trim()
        val descricao = etDescricaoPasseio.text.toString().trim()

        if (local.isEmpty() || descricao.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
        } else {
            // Use o valor atual da pessoa; evite criar um novo observador no clique
            val pessoa = pessoaViewModel.pessoa.value
            if (pessoa == null) {
                Toast.makeText(requireContext(), "Dados do usuário não disponíveis", Toast.LENGTH_SHORT).show()
                return
            }
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val horarioAtual = LocalTime.now().format(formatter)

            // Cria os RequestBody para cada campo
            val idComunidadeBody = idComunidade.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val usuarioBody = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
            val horarioBody = horarioAtual.toRequestBody("text/plain".toMediaTypeOrNull())
            val descricaoPasseioBody = descricao.toRequestBody("text/plain".toMediaTypeOrNull())
            val localBody = local.toRequestBody("text/plain".toMediaTypeOrNull())

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.createPasseio(
                            idComunidadeBody,
                            usuarioBody,
                            horarioBody,
                            descricaoPasseioBody,
                            localBody
                        )
                    }
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Passeio criado com sucesso!", Toast.LENGTH_SHORT).show()
                        edtLocal.text.clear()
                        etDescricaoPasseio.text.clear()
                        pessoaViewModel.reloadPessoa()
                        fetchPasseios() // Atualiza imediatamente a lista de passeios
                    } else {
                        Toast.makeText(requireContext(), "Erro ao criar passeio", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchPasseios() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getPasseiosByComunidade(idComunidade)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val passeiosFromApi = response.body() ?: emptyList()
                    passeios.clear()
                    passeios.addAll(passeiosFromApi)
                    adapter.notifyDataSetChanged()
                    if (passeiosFromApi.isEmpty()) {
                        context?.let {
                            Toast.makeText(it, "Nenhum passeio encontrado", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {

                }
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String, idComunidade: Int) =
            ControllerManterPasseio().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                    putInt("idComunidade", idComunidade)
                }
            }
    }
}