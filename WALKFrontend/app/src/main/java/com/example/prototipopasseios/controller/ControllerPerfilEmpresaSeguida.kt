package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.viewmodel.PerfilEmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ControllerPerfilEmpresaSeguida : Fragment() {

    companion object {
        private const val ARG_EMPRESA = "empresa_usuario"
        @JvmStatic
        fun newInstance(empresaUsuario: String) =
            ControllerPerfilEmpresaSeguida().apply {
                arguments = Bundle().apply {
                    // mantém o @
                    putString(ARG_EMPRESA, empresaUsuario)
                }
            }
    }
    private lateinit var pessoaViewModel: PessoaViewModel
    private var empresaSeguida: Empresa? = null
    // Views
    private lateinit var imgPerfil: ImageView
    private lateinit var tvNome: TextView
    private lateinit var tvUser: TextView
    private lateinit var tvDescricao: TextView
    private lateinit var btnSeguir: Button
    private lateinit var rvEventos: RecyclerView



    // Lista de eventos
    private val eventos = mutableListOf<Evento>()
    private lateinit var adapter: EventoAdapter

    // Estado de seguimento
    private var jaSegue = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_controller_perfil_empresa_seguida, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val usuarioEmpresa = arguments?.getString(ARG_EMPRESA) ?: return

        imgPerfil   = view.findViewById(R.id.iVPerfilSeguida)
        tvNome      = view.findViewById(R.id.tVNomeSeguida)
        tvUser      = view.findViewById(R.id.tVUserSeguida)
        tvDescricao = view.findViewById(R.id.tVDescricaoPerfilSeguida)
        btnSeguir   = view.findViewById(R.id.buttonSeguir)
        rvEventos   = view.findViewById(R.id.rvMeusEvetosSeguidos)

        rvEventos.layoutManager = LinearLayoutManager(requireContext())
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
        rvEventos.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                pessoaApiService.getEmpresaByUsuario(usuarioEmpresa)
            }
            if (resp.isSuccessful) {
                empresaSeguida = resp.body()
                empresaSeguida?.let { e ->
                    preencherDados(e)
                    carregarEventos(e.usuario)
                    checkFollowing(e.usuario)
                }
            } else {
                Toast.makeText(requireContext(),
                    "Falha ao carregar empresa (${resp.code()})",
                    Toast.LENGTH_SHORT
                ).show()
            }


        btnSeguir.setOnClickListener { toggleFollow() }
    }

        btnSeguir.setOnClickListener { toggleFollow() }
    }

    private fun preencherDados(e: Empresa) {
        tvNome.text = e.name
        tvUser.text = e.usuario
        tvDescricao.text = e.description
        Glide.with(requireContext())
            .load(e.imageUrl.takeIf { !it.isNullOrBlank() })
            .into(imgPerfil)
    }

    private fun carregarEventos(usuario: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                pessoaApiService.getEventosByUsuario(usuario)
            }
            if (resp.isSuccessful) {
                eventos.clear()
                eventos.addAll(resp.body().orEmpty())
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "Falha ao buscar eventos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkFollowing(empresaUsuario: String) {
        val usuarioLogado = pessoaViewModel.pessoa.value?.usuario ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                pessoaApiService.isFollowingEmpresa(empresaUsuario, usuarioLogado)
            }
            jaSegue = resp.body()?.get("following") ?: false
            atualizarBotao()
        }
    }

    private fun atualizarBotao() {
        btnSeguir.text = if (jaSegue) "Deixar de seguir" else "Seguir"
    }

    private fun toggleFollow() {
        val usuarioEmpresa = empresaSeguida?.usuario ?: return
        val usuarioLogado  = pessoaViewModel.pessoa.value?.usuario ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val call = withContext(Dispatchers.IO) {
                if (jaSegue)
                    pessoaApiService.unfollowEmpresa(usuarioEmpresa, usuarioLogado)
                else
                    pessoaApiService.followEmpresa(usuarioEmpresa, usuarioLogado)
            }
            if (call.isSuccessful) {
                jaSegue = !jaSegue
                atualizarBotao()
                // só recarrega qtEmpresas do usuário logado:
                pessoaViewModel.reloadPessoa()
                Toast.makeText(
                    requireContext(),
                    if (jaSegue) "Agora você segue $usuarioEmpresa"
                    else          "Deixou de seguir $usuarioEmpresa",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Erro: ${call.code()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goNextActivityEvento(position: Int, eventos: ArrayList<Evento>) {
        val selectedEvento = eventos[position]
        val fragment = ControllerManterPasseioEvento.newInstance("", "", selectedEvento.idEvento)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }
}