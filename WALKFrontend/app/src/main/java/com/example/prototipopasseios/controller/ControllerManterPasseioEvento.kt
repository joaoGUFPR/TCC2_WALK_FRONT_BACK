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
import com.example.prototipopasseios.model.PasseioEvento
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.app.AlertDialog
import android.view.Gravity
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService

class ControllerManterPasseioEvento : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private var idEvento: Int = 0

    private lateinit var adapter: PasseioEventoAdapter
    private var passeios: MutableList<PasseioEvento> = mutableListOf()
    private lateinit var passeioRV: RecyclerView
    private lateinit var pessoaViewModel: PessoaViewModel

    private lateinit var etDescricaoPasseio: EditText
    private lateinit var btnCriarPasseio: Button
    private lateinit var ivMap: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("param1")
            param2 = it.getString("param2")
            idEvento = it.getInt("idEvento", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_manter_passeio_evento, container, false)
        passeioRV = view.findViewById(R.id.PasseioEventoRV)
        etDescricaoPasseio = view.findViewById(R.id.etDescricaoPasseioEvento)
        btnCriarPasseio = view.findViewById(R.id.btnCriarPasseioEvento)
        ivMap             = view.findViewById(R.id.iVPerfilPasseioEvento)

        passeioRV.layoutManager = LinearLayoutManager(requireContext())
        adapter = PasseioEventoAdapter(passeios, requireContext(), idEvento,
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
                                    RetrofitClient.pessoaApiService.deletePasseioEvento(passeio.idPasseioEvento)
                                }
                                if (!isAdded) return@launch
                                // Volta para Main para atualizar UI/VM
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        // 1) Atualiza o ViewModel para buscar o qtPasseios decrementado
                                        pessoaViewModel.reloadPessoa()

                                        // 2) Atualiza a lista local e notifica o Adapter
                                        this@ControllerManterPasseioEvento.passeios.removeAt(position)
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

        // Busque os passeios já cadastrados para esta Evento
        fetchPasseios()

        btnCriarPasseio.setOnClickListener {
            createPasseioEvento()
        }

        ivMap.setOnClickListener { showPopupLocalizacao() }
        return view
    }

    private fun showPopupLocalizacao() {
        lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                pessoaApiService.getEventoById(idEvento)
            }
            if (!isAdded) return@launch
            if (resp.isSuccessful) {
                val evento = resp.body()!!
                // concatena municípios e local
                val locText = (evento.municipios.joinToString(", ")
                    .takeIf { it.isNotBlank() }?.let { "$it - ${evento.local}" }
                    ?: evento.local)

                val popupView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_localizacao, null)
                val tVLocaliza: TextView = popupView.findViewById(R.id.tvLocaliza)
                tVLocaliza.text = locText

                val popupWindow = PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                )
                // posiciona próximo ao ícone
                val location = IntArray(2)
                ivMap.getLocationOnScreen(location)
                val x = location[0]
                val y = location[1] + ivMap.height
                popupWindow.showAtLocation(ivMap.rootView, Gravity.NO_GRAVITY, x, y)
            } else {
                Toast.makeText(requireContext(),
                    "Não foi possível obter localização", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPasseioEvento() {
        val descricao = etDescricaoPasseio.text.toString().trim()
        if (descricao.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val pessoa = pessoaViewModel.pessoa.value
        if (pessoa == null) {
            Toast.makeText(requireContext(), "Dados do usuário não disponíveis", Toast.LENGTH_SHORT).show()
            return
        }

        // Hora no formato “HH:mm”
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val horarioAtual = LocalTime.now().format(formatter)

        // Monta os RequestBody
        val idEventoBody         = idEvento.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val usuarioBody          = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val horarioBody          = horarioAtual.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoPasseioBody = descricao.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.createPasseioEvento(
                        idEventoBody,
                        usuarioBody,
                        horarioBody,
                        descricaoPasseioBody
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Passeio criado com sucesso!", Toast.LENGTH_SHORT).show()
                    etDescricaoPasseio.text.clear()
                    pessoaViewModel.reloadPessoa()
                    fetchPasseios()
                } else {
                    // MOSTRE O CÓDIGO E O ERROR-BODY PRA ENTENDER MELHOR
                    val err = response.errorBody()?.string() ?: "Desconhecido"
                    Toast.makeText(requireContext(), "Erro ao criar passeio (HTTP ${response.code()}): $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchPasseios() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getPasseiosByEvento(idEvento)
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
        fun newInstance(param1: String, param2: String, idEvento: Int) =
            ControllerManterPasseioEvento().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                    putInt("idEvento", idEvento)
                }
            }
    }
}