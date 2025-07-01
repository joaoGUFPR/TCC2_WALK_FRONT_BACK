package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Pessoa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerUsuarioADM : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var adapter: UsuarioADMAdapter

    private val fullList = mutableListOf<Pessoa>()
    private val displayList = mutableListOf<Pessoa>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_usuario_a_d_m, container, false)

        searchView  = view.findViewById(R.id.searchViewUsuarioADM)
        rvUsuarios  = view.findViewById(R.id.ComunidadeRV) // id reusado no XML

        adapter = UsuarioADMAdapter(
            usuarios    = displayList,
            context     = requireContext(),
            deleteClick = { pessoa, pos -> confirmDelete(pessoa, pos) }
        )

        rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        rvUsuarios.adapter        = adapter

        // Expande e formata a SearchView
        searchView.setOnClickListener { searchView.isIconified = false }
        val et = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        et.setTextColor(resources.getColor(R.color.black, null))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also { searchView.clearFocus() }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })

        fetchUsuarios()
        return view
    }

    private fun fetchUsuarios() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getAllPessoas()
                }
                if (resp.isSuccessful) {
                    fullList.clear()
                    fullList.addAll(resp.body().orEmpty())
                    displayList.apply {
                        clear()
                        addAll(fullList)
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Erro ao carregar usuários", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Falha: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterList(q: String) {
        val filtered = if (q.isBlank()) fullList
        else fullList.filter { it.name.startsWith(q, ignoreCase = true) }
        displayList.apply {
            clear()
            addAll(filtered)
        }
        adapter.notifyDataSetChanged()
    }

    private fun confirmDelete(pessoa: Pessoa, pos: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Usuário")
            .setMessage("Deseja excluir \"${pessoa.usuario}\"?")
            .setPositiveButton("Sim") { _, _ ->
                lifecycleScope.launch {
                    val resp = withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.deletePessoa(pessoa.usuario)
                    }
                    if (resp.isSuccessful) {
                        displayList.removeAt(pos)
                        adapter.notifyItemRemoved(pos)
                        Toast.makeText(requireContext(), "Usuário excluído", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Falha ao excluir", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    companion object {
        @JvmStatic fun newInstance() = ControllerUsuarioADM()
    }
}
