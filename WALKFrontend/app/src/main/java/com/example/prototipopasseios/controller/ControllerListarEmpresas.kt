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
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerListarEmpresas : Fragment() {

    private lateinit var pessoaViewModel: PessoaViewModel

    // Retira o '@' ao buscar o usuário logado
    private val usuarioComArroba: String?
        get() = pessoaViewModel.pessoa.value?.usuario

    private lateinit var recyclerView: RecyclerView
    private lateinit var empresasAdapter: EmpresasAdapter
    private val listaEmpresa = mutableListOf<Empresa>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        return inflater.inflate(R.layout.fragment_controller_listar_empresas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.ListaEmpresasRV)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Adapter: ao clicar num amigo, navega para o perfil sem o '@'
        empresasAdapter = EmpresasAdapter(listaEmpresa, requireContext()) { amigo ->
            val amigoSemArroba = amigo.usuario.removePrefix("@")
            val frag = ControllerPerfilAmigo.newInstance(amigoSemArroba)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, frag)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = empresasAdapter

        fetchAmigos()
    }

    private fun fetchAmigos() {
        val u = usuarioComArroba
        if (u.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getEmpresasSeguidas(usuarioComArroba!!)
            }
            if (!isAdded) return@launch

            if (resp.isSuccessful) {
                listaEmpresa.clear()
                // Cada Pessoa.usuario já vem com '@', mas exibiremos sem
                listaEmpresa.addAll(resp.body().orEmpty())
                empresasAdapter.notifyDataSetChanged()
                if (listaEmpresa.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Você não segue nenhuma empresa",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {

            }
        }
    }
}
