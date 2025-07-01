package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import android.graphics.Color
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ControllerComunidade.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControllerComunidade : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var comunidadesRV: RecyclerView
    private lateinit var adapter: ComunidadeAdapter
    private lateinit var searchList: ArrayList<Comunidade>
    private lateinit var iVFiltro: ImageView
    private lateinit var iVArrow: ImageView
    private lateinit var fullList: MutableList<Comunidade>
    private lateinit var tVCidades: TextView
    private var activeTag: String? = null
    private var activeMunicipio: String? = null
    private var activeSearchText: String? = null
    private lateinit var comunidades: MutableList<Comunidade>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comunidades, container, false)
        comunidadesRV = view.findViewById<RecyclerView>(R.id.ComunidadeRV)
        searchView = view.findViewById<SearchView>(R.id.searchView)
        iVFiltro = view.findViewById<ImageView>(R.id.filterButton)
        iVArrow = view.findViewById<ImageView>(R.id.iVArrowDropButton)
        tVCidades = view.findViewById<TextView>(R.id.tVCidade)
        searchView.setOnClickListener {
            searchView.isIconified = false  // Faz a SearchView expandir se estiver minimizada
            searchView.requestFocus()       // Solicita o foco na SearchView para abrir o teclado
        }
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        val pessoaViewModel = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)
        val userMunicipio = pessoaViewModel.pessoa.value?.municipio

        if (!userMunicipio.isNullOrBlank()) {
            activeMunicipio = userMunicipio
            tVCidades.text = userMunicipio
        }
// Definindo a cor do texto para preto
        searchEditText.setTextColor(Color.BLACK)  // Texto digitado
// Defina a cor dos ícones como preta ou escura
        searchIcon.setColorFilter(Color.GRAY)
        closeIcon.setColorFilter(Color.GRAY)

        iVArrow.setOnClickListener {
            showMunicipioSearchDialog()
        }

        iVFiltro.setOnClickListener {
            showTagSelectionDialog()
        }
        val novaComunidade = arguments?.getParcelable<Comunidade>("novaComunidade")

        // Lista original de comunidades
        fullList = mutableListOf()
        comunidades = mutableListOf()

        // Configuração do Adapter
        adapter = ComunidadeAdapter(
            comunidades = comunidades, // Certifique-se de usar uma MutableList
            context = requireContext(),
            favoriteClick = { comunidade ->
                val usuario = pessoaViewModel.pessoa.value?.usuario
                if (usuario.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                    return@ComunidadeAdapter
                }
                val requestUsuario = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestIdComunidade = comunidade.idComunidade.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.favoritarComunidade(requestUsuario, requestIdComunidade)
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
            },
            click = { comunidade, position ->
                goNextActivity(position, ArrayList(comunidades))
            },
            deleteClick = { comunidade, position ->
                // Exibe um diálogo de confirmação
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Comunidade")
                    .setMessage("Deseja realmente excluir a comunidade ${comunidade.name}?")
                    .setPositiveButton("Sim") { _, _ ->
                        // Toast para indicar que o processo de exclusão começou
                        Toast.makeText(requireContext(), "Iniciando exclusão...", Toast.LENGTH_SHORT).show()
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                // Toast para indicar que a chamada à API foi disparada
                                Toast.makeText(requireContext(), "Chamando a API para excluir...", Toast.LENGTH_SHORT).show()
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService.deleteComunidade(comunidade.idComunidade)
                                }
                                // Toast para verificar se a API retornou com sucesso
                                if (response.isSuccessful) {
                                    Toast.makeText(requireContext(), "Comunidade excluída com sucesso", Toast.LENGTH_SHORT).show()
                                    // Remove da lista e atualiza o adapter
                                    comunidades.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                } else {
                                    // Toast para quando a API retorna uma resposta não-sucedida
                                    Toast.makeText(requireContext(), "Falha ao excluir a comunidade. Código: ${response.code()}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                // Toast para exibir a exceção, se ocorrer
                                Toast.makeText(requireContext(), "Erro na exclusão: ${e.message}", Toast.LENGTH_LONG).show()
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

        comunidadesRV.adapter = adapter
        comunidadesRV.layoutManager = LinearLayoutManager(requireContext())
        comunidadesRV.setHasFixedSize(true)

        // Configuração da barra de busca
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

        fetchComunidades()
        return view
    }


    private fun fetchComunidades() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getComunidades()
                }

                if (!isAdded) return@launch

                if (response.isSuccessful) {
                    val comunidadesFromApi = response.body() ?: emptyList()
                    fullList.clear()
                    fullList.addAll(comunidadesFromApi)
                    comunidades.clear()
                    comunidades.addAll(comunidadesFromApi)
                    adapter.notifyDataSetChanged()
                } else {

                }
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun filterByTag(tag: String) {
        activeTag = tag
        applyActiveFilters()
    }



    private fun filterByMunicipio(municipio: String) {
        if (municipio == "Cidades") {
            resetFiltersMunicipio() // Limpa todos os filtros e exibe a lista completa
        } else {
            activeMunicipio = municipio // Define o município ativo
            applyActiveFilters() // Aplica os filtros acumulativos
        }
    }



    private fun resetFilters() {
        activeTag = null
        activeMunicipio = null
        activeSearchText = null
        applyActiveFilters() // Reaplica os filtros (sem filtros ativos = lista completa)
    }

    private fun resetFiltersMunicipio() {
        activeMunicipio = null
        applyActiveFilters() // Reaplica os filtros (sem filtros ativos = lista completa)
    }

    private fun resetFiltersTag() {
        activeTag = null
        applyActiveFilters() // Reaplica os filtros (sem filtros ativos = lista completa)
    }

    private fun showMunicipioSearchDialog() {
        // 1) infla o layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_search_municipios, null)
        val sv = dialogView.findViewById<SearchView>(R.id.svMunicipio)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvMunicipios)

        // 2) cria o AlertDialog e já marca para fechar depois
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
                dialog.dismiss()  // fecha corretamente
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

        // 7) mostra o diálogo depois de toda a configuração acima
        dialog.show()
    }







    private fun applyActiveFilters() {
        // Começa com a lista original
        var filtered = fullList.toMutableList()


        // Aplica o filtro de tags, se ativo
        activeTag?.let { tag ->
            filtered = filtered.filter { it.tags.any { t -> t.contains(tag, ignoreCase = true) } }.toMutableList()
        }

        // Aplica o filtro de município, se ativo
        activeMunicipio?.let { municipio ->
            filtered = filtered.filter { it.municipio.contains(municipio, ignoreCase = true) }.toMutableList()
        }

        // Aplica o filtro de texto, se ativo
        activeSearchText?.let { searchText ->
            filtered = filtered.filter { it.name.startsWith(searchText, ignoreCase = true) }.toMutableList()
        }

        // Atualiza a lista exibida
        comunidades.clear()
        comunidades.addAll(filtered)
        adapter.notifyDataSetChanged()
    }




    private fun showTagSelectionDialog() {
        // Crie uma lista com os nomes das tags
        val tags = listOf("Tags","Esporte", "Festa", "Jogos de Tabuleiro")

        // Use um AlertDialog para exibir o layout do diálogo
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_tags, null)
        val listView = dialogView.findViewById<ListView>(R.id.tag_list_view)

        // Adapte as tags para o ListView

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tags)
        listView.adapter = adapter

        // Crie o AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Adicione ação de clique nos itens
        listView.setOnItemClickListener { _, _, position, _ ->
            when (tags[position]) {
                "Tags" -> resetFiltersTag()
                "Esporte" -> filterByTag("esporte")
                "Festa" -> filterByTag("festa")
                "Jogos de Tabuleiro" -> filterByTag("jogos de tabuleiro")
            }
            dialog.dismiss() // Feche o diálogo após selecionar
        }

        dialog.show()
    }

    private fun showMunicipioSelectionDialog() {
        // Crie uma lista com os nomes das municipios
        val municipios = listOf("Cidades", "Curitiba", "Londrina", "São Paulo")

        // Use um AlertDialog para exibir o layout do diálogo
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_municipios, null)
        val listView = dialogView.findViewById<ListView>(R.id.municipios_list_view)

        // Adapte as municipios para o ListView

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, municipios)
        listView.adapter = adapter

        // Crie o AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Adicione ação de clique nos itens
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedMunicipio = municipios[position]

            // Atualiza o texto do TextView
            tVCidades.text = selectedMunicipio

            // Aplica o filtro baseado no município selecionado
            filterByMunicipio(selectedMunicipio)

            dialog.dismiss() // Feche o diálogo após selecionar
        }

        dialog.show()
    }




    private fun goNextActivity(position: Int, comunidades: ArrayList<Comunidade>) {
        val selectedComunidade = comunidades[position]
        // Cria uma instância de ControllerManterPasseio passando o id da comunidade
        val fragment = ControllerManterPasseio.newInstance("param1", "param2", selectedComunidade.idComunidade)
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }





    override fun onResume()

    {

        super.onResume()

    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Comunidades.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ControllerComunidade().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}