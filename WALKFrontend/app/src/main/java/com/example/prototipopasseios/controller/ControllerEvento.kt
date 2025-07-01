package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class ControllerEvento : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var eventosRV: RecyclerView
    private lateinit var adapter: EventoAdapter
    private lateinit var fullList: MutableList<Evento>
    private lateinit var eventos: MutableList<Evento>
    private lateinit var iVFiltro: ImageView
    private lateinit var iVArrow: ImageView
    private lateinit var tVCidades: TextView
    private lateinit var pessoaViewModel: PessoaViewModel

    private var activeTag: String? = null
    private var activeMunicipio: String? = null
    private var activeSearchText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_eventos, container, false)
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        eventosRV = view.findViewById(R.id.EventoRV)
        searchView = view.findViewById(R.id.searchView)
        iVFiltro = view.findViewById(R.id.filterButton)
        iVArrow = view.findViewById(R.id.iVArrowDropButton)
        tVCidades = view.findViewById(R.id.tVCidade)

        // Configura cores da SearchView
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.BLACK)
        searchIcon.setColorFilter(Color.GRAY)
        closeIcon.setColorFilter(Color.GRAY)
        searchView.setOnClickListener {
            searchView.isIconified = false
            searchView.requestFocus()
        }

        iVFiltro.setOnClickListener { showTagSelectionDialog() }
        iVArrow.setOnClickListener { showMunicipioSearchDialog() }

        // Inicializa listas
        fullList = mutableListOf()
        eventos = mutableListOf()

        // Configura adapter com callbacks:
        adapter = EventoAdapter(
            eventos = eventos,
            context = requireContext(),

            // Clique simples: abre BottomSheet de detalhes
            click = { evento, position ->
                goNextActivityEvento(position, ArrayList(eventos))
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

        eventosRV.adapter = adapter
        eventosRV.layoutManager = LinearLayoutManager(requireContext())
        eventosRV.setHasFixedSize(true)

        // Configuração da pesquisa por texto
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                activeSearchText = newText?.toLowerCase(Locale.getDefault()).orEmpty()
                applyActiveFilters()
                return true
            }
        })

        // Busca inicial dos eventos
        fetchEventos()
        return view
    }

    private fun fetchEventos() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    pessoaApiService.getAllEventos()
                }
                if (!isAdded) return@launch

                if (response.isSuccessful) {
                    val eventosFromApi = response.body() ?: emptyList()
                    fullList.clear()
                    fullList.addAll(eventosFromApi)
                    eventos.clear()
                    eventos.addAll(eventosFromApi)
                    adapter.notifyDataSetChanged()
                } else {
                    if (!isAdded) return@launch
                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
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

    private fun showTagSelectionDialog() {
        val tags = listOf("Tags", "Esporte", "Festa", "Jogos de Tabuleiro")
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_tags, null)
        val listView = dialogView.findViewById<ListView>(R.id.tag_list_view)
        val adapterDialog = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tags)
        listView.adapter = adapterDialog

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            when (tags[position]) {
                "Tags" -> resetFiltersTag()
                "Esporte" -> filterByTag("esporte")
                "Festa" -> filterByTag("festa")
                "Jogos de Tabuleiro" -> filterByTag("jogos de tabuleiro")
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showMunicipioSearchDialog() {
        // 1) infla o layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_search_municipios, null)
        val sv = dialogView.findViewById<SearchView>(R.id.svMunicipio)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvMunicipios)

        // 2) cria o AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // 3) busca a lista do servidor
        viewLifecycleOwner.lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getMunicipios()
            }
            if (!response.isSuccessful) {
                return@launch
            }

            // 4) extrai só o nome antes do " - "
            val rawList = response.body().orEmpty()
            val cityOnlyList = rawList
                .map { it.nomeMunicipio.substringBefore(" - ").trim() }
                .distinct()

            // 5) configura RecyclerView + adapter
            rv.layoutManager = LinearLayoutManager(requireContext())
            val adapter = SimpleStringAdapter { selectedCity ->
                tVCidades.text = selectedCity
                activeMunicipio = selectedCity
                applyActiveFilters()
                dialog.dismiss()
            }
            rv.adapter = adapter
            adapter.submitList(cityOnlyList)

            // 6) configura SearchView interno
            sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    val filtered = cityOnlyList.filter {
                        it.startsWith(newText.orEmpty(), ignoreCase = true)
                    }
                    adapter.submitList(filtered)
                    return true
                }
            })
        }

        // 7) mostra o diálogo
        dialog.show()
    }

    private fun filterByTag(tag: String) {
        activeTag = tag
        applyActiveFilters()
    }

    private fun filterByMunicipio(municipio: String) {
        activeMunicipio = municipio
        applyActiveFilters()
    }

    private fun resetFiltersTag() {
        activeTag = null
        applyActiveFilters()
    }

    private fun resetFiltersMunicipio() {
        activeMunicipio = null
        applyActiveFilters()
    }

    private fun applyActiveFilters() {
        var filtered = fullList.toMutableList()
        activeTag?.let { tag ->
            filtered = filtered.filter {
                it.tags.any { t -> t.contains(tag, ignoreCase = true) }
            }.toMutableList()
        }
        activeMunicipio?.let { mun ->
            filtered = filtered.filter {
                it.municipios.any { m -> m.equals(mun, ignoreCase = true) }
            }.toMutableList()
        }
        activeSearchText?.let { text ->
            filtered = filtered.filter {
                it.name.startsWith(text, ignoreCase = true)
            }.toMutableList()
        }
        eventos.clear()
        eventos.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ControllerEvento()
    }
}
