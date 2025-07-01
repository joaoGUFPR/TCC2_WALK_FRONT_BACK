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
import com.example.prototipopasseios.model.Comunidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerComunidadeADM : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rvComunidades: RecyclerView
    private lateinit var adapter: ComunidadeAdapterADM

    private val fullList = mutableListOf<Comunidade>()
    private val displayList = mutableListOf<Comunidade>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_comunidade_a_d_m, container, false)

        searchView    = view.findViewById(R.id.searchViewComunidadeADM)
        rvComunidades = view.findViewById(R.id.ComunidadeRV)

        adapter = ComunidadeAdapterADM(
            comunidades = displayList,
            context      = requireContext(),
            click        = { _, _ -> /* detalhe opcional */ },
            deleteClick  = { com, pos -> confirmDelete(com, pos) },
            updateClick  = { com, _   -> goToEdit(com) }
        )
        rvComunidades.layoutManager = LinearLayoutManager(requireContext())
        rvComunidades.adapter        = adapter

        // expande e formata SearchView
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

        fetchComunidades()
        return view
    }

    private fun fetchComunidades() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getComunidades()
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
                    Toast.makeText(requireContext(), "Erro ao carregar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Falha: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterList(q: String) {
        // agora usando startsWith em vez de contains
        val filtered = if (q.isBlank()) {
            fullList
        } else {
            fullList.filter { it.name.startsWith(q, ignoreCase = true) }
        }
        displayList.apply {
            clear()
            addAll(filtered)
        }
        adapter.notifyDataSetChanged()
    }

    private fun confirmDelete(com: Comunidade, pos: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Comunidade")
            .setMessage("Deseja excluir \"${com.name}\"?")
            .setPositiveButton("Sim") { _, _ ->
                lifecycleScope.launch {
                    val resp = withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.deleteComunidade(com.idComunidade)
                    }
                    if (resp.isSuccessful) {
                        displayList.removeAt(pos)
                        adapter.notifyItemRemoved(pos)
                        Toast.makeText(requireContext(), "Excluída", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Falha ao excluir", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun goToEdit(com: Comunidade) {
        val fragment = ControllerManterComunidade().apply {
            arguments = Bundle().apply {
                putParcelable("comunidade", com)
            }
        }
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic fun newInstance() = ControllerComunidadeADM()
    }
}
