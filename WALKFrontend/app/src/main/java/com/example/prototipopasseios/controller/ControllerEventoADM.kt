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
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Evento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ControllerEventoADM : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var rvEventos: RecyclerView
    private lateinit var adapter: EventoAdapterADM

    private val fullList = mutableListOf<Evento>()
    private val displayList = mutableListOf<Evento>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_evento_a_d_m, container, false)

        searchView    = view.findViewById(R.id.searchViewEventoADM)
        rvEventos = view.findViewById(R.id.EventoRV)

        adapter = EventoAdapterADM(
            eventos = fullList,
            context = requireContext(),

            // Clique simples: abre BottomSheet de detalhes
            click = { _, _ -> /* detalhe opcional */
            },

            // deleteClick: confirma exclusão, chama API e atualiza lista
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
                                        fullList.removeAt(position)
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
            }
        )
        rvEventos.layoutManager = LinearLayoutManager(requireContext())
        rvEventos.adapter        = adapter

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

        fetchEventos()
        return view
    }

    private fun fetchEventos() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getAllEventos()
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

    private fun confirmDelete(com: Evento, pos: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Evento")
            .setMessage("Deseja excluir \"${com.name}\"?")
            .setPositiveButton("Sim") { _, _ ->
                lifecycleScope.launch {
                    val resp = withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.deleteEvento(com.idEvento)
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

    private fun goToEdit(com: Evento) {
        val fragment = ControllerManterEvento().apply {
            arguments = Bundle().apply {
                putParcelable("Evento", com)
            }
        }
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic fun newInstance() = ControllerEventoADM()
    }
}
