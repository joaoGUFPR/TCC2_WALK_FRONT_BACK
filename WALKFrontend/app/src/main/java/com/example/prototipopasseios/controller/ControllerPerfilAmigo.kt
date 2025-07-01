package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient
import com.example.prototipopasseios.model.Notificacao
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.prototipopasseios.controller.MixedPasseioAdapter
import com.example.prototipopasseios.controller.PasseioItem
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import kotlinx.coroutines.async

class ControllerPerfilAmigo : Fragment() {

    companion object {
        private const val ARG_USUARIO = "usuario_visitado"
        @JvmStatic
        fun newInstance(usuario: String) =
            ControllerPerfilAmigo().apply {
                arguments = Bundle().apply { putString(ARG_USUARIO, usuario) }
            }
    }

    private var visitedUser: String? = null
    private lateinit var pessoaViewModel: PessoaViewModel

    // Views
    private lateinit var imgPerfil: ImageView
    private lateinit var tvNome: TextView
    private lateinit var tvUser: TextView
    private lateinit var tvDescricao: TextView
    private lateinit var tvNAmizades: TextView
    private lateinit var tvNPasseios: TextView
    private lateinit var btnAdd: Button
    private lateinit var rvPasseios: RecyclerView
    private val mixedItems = mutableListOf<PasseioItem>()
    private lateinit var mixedAdapter: MixedPasseioAdapter

    // Estados de amizade
    private var ehAmigo = false
    private var pedidoEnviado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        visitedUser = arguments?.getString(ARG_USUARIO)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_controller_perfil_amigo, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]

        imgPerfil   = view.findViewById(R.id.iVPerfilAmigo)
        tvNome      = view.findViewById(R.id.tVNomeAmigo)
        tvUser      = view.findViewById(R.id.tVUserAmigo)
        tvDescricao = view.findViewById(R.id.tVDescricaoPerfilAmigo)
        tvNAmizades = view.findViewById(R.id.tVNAmizades)
        tvNPasseios = view.findViewById(R.id.tVNPasseiosAmigo)
        btnAdd      = view.findViewById(R.id.buttonAdicionarAmigo)
        rvPasseios  = view.findViewById(R.id.rvMeusPasseiosAmigo)

        // Setup RecyclerView
        rvPasseios.layoutManager = LinearLayoutManager(requireContext())
        mixedAdapter = MixedPasseioAdapter(
            items               = mixedItems,
            context             = requireContext(),

            // ==== Comunidade ====
            onComunidadeClick   = { passeio, pos ->
                Toast.makeText(
                    requireContext(),
                    "Clicou na comunidade de ${passeio.nomePessoa}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onComunidadeDelete  = { passeio, pos ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Passeio")
                    .setMessage("Deseja realmente excluir o passeio de ${passeio.nomePessoa}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val resp = withContext(Dispatchers.IO) {
                                RetrofitClient.pessoaApiService
                                    .deletePasseio(passeio.idPasseioComunidade)
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    pessoaViewModel.reloadPessoa()
                                    mixedItems.removeAt(pos)
                                    mixedAdapter.notifyItemRemoved(pos)
                                    Toast.makeText(
                                        requireContext(),
                                        "Passeio de comunidade excluído",
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
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            onComunidadeImage   = { passeio, pos ->
                val current = pessoaViewModel.pessoa.value?.usuario
                val frag = if (passeio.usuario == current)
                    ControllerPerfil.newInstance()
                else
                    ControllerPerfilAmigo.newInstance(passeio.usuario)

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },
            onComunidadeComment = { passeio ->
                // balão: navega para comentários de comunidade
                val frag = ControllerComentarioPasseioComunidade().apply {
                    arguments = Bundle().apply {
                        putParcelable("passeio", passeio)
                        putInt("idComunidade", passeio.idPasseioComunidade)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },

            // ==== Evento ====
            onEventoClick       = { evento, pos ->
                Toast.makeText(
                    requireContext(),
                    "Clicou no evento de ${evento.nomePessoa}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEventoDelete      = { evento, pos ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Passeio de Evento")
                    .setMessage("Deseja realmente excluir o passeio de ${evento.nomePessoa}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val resp = withContext(Dispatchers.IO) {
                                RetrofitClient.pessoaApiService
                                    .deletePasseioEvento(evento.idPasseioEvento)
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    pessoaViewModel.reloadPessoa()
                                    mixedItems.removeAt(pos)
                                    mixedAdapter.notifyItemRemoved(pos)
                                    Toast.makeText(
                                        requireContext(),
                                        "Passeio de evento excluído",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Falha ao excluir passeio de evento",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            onEventoImage       = { evento, pos ->
                val current = pessoaViewModel.pessoa.value?.usuario
                val frag = if (evento.usuario == current)
                    ControllerPerfil.newInstance()
                else
                    ControllerPerfilAmigo.newInstance(evento.usuario)

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },
            onEventoComment     = { evento ->
                // balão: navega para comentários de evento
                val frag = ControllerComentarioPasseioEvento().apply {
                    arguments = Bundle().apply {
                        putParcelable("passeio", evento)
                        putInt("idEvento", evento.idPasseioEvento)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            }
        )
        rvPasseios.adapter = mixedAdapter

        // Carrega perfil, passeios e estado de amizade
        preencherPerfilEPasseios()
        atualizarAmizadeEstado()

        btnAdd.setOnClickListener {
            val currentUser = pessoaViewModel.pessoa.value?.usuario ?: return@setOnClickListener
            val targetUser  = visitedUser ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                when {
                    ehAmigo -> {
                        // desfazer amizade
                        val resp = withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.removeFriend(currentUser, targetUser)
                        }
                        withContext(Dispatchers.Main) {
                            if (resp.isSuccessful) {
                                Toast.makeText(requireContext(), "Amizade desfeita", Toast.LENGTH_SHORT).show()
                                ehAmigo = false
                                pedidoEnviado = false

                                // recarrega dados do perfil amigo
                                val perfilResp = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService.getPessoa(targetUser)
                                }
                                if (perfilResp.isSuccessful) {
                                    preencherDados(perfilResp.body()!!)
                                }
                                // atualiza estado do botão
                                atualizarBotao()

                                // recarrega contagem do usuário logado no ViewModel
                                pessoaViewModel.reloadPessoa()
                            } else {
                                Toast.makeText(requireContext(), "Erro ao desfazer amizade", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    pedidoEnviado -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Pedido já pendente", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        // envia pedido de amizade
                        val horarioAtual = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        val resp = withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.addFriend(
                                currentUser,
                                mapOf("amigo" to visitedUser!!, "horario" to horarioAtual)
                            )
                        }
                        withContext(Dispatchers.Main) {
                            if (resp.isSuccessful) {
                                pedidoEnviado = true
                                Toast.makeText(requireContext(),
                                    "Pedido enviado!", Toast.LENGTH_SHORT).show()
                                atualizarBotao()
                            } else {
                                Toast.makeText(requireContext(),
                                    "Falha ao enviar pedido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-checa amizade sempre que retornar a este fragment
        atualizarAmizadeEstado()
    }

    private fun preencherPerfilEPasseios() {
        val alvo = visitedUser ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) fetch perfil do visitado
            val profileResp = withContext(Dispatchers.IO) {
                pessoaApiService.getPessoa(alvo)
            }
            if (profileResp.isSuccessful) preencherDados(profileResp.body()!!)

            // 2) fetch de passeios em paralelo
            val cDef = async(Dispatchers.IO) { pessoaApiService.getPasseiosByUsuario(alvo) }
            val eDef = async(Dispatchers.IO) { pessoaApiService.getPasseiosEventoByUsuario(alvo) }
            val rCom = cDef.await()
            val rEvt = eDef.await()

            if (!isAdded) return@launch
            if (rCom.isSuccessful || rEvt.isSuccessful) {
                mixedItems.clear()
                rCom.body().orEmpty().forEach { mixedItems.add(PasseioItem.Comunidade(it)) }
                rEvt.body().orEmpty().forEach { mixedItems.add(PasseioItem.Evento(it)) }
                mixedItems.sortBy {
                    when (it) {
                        is PasseioItem.Comunidade -> it.passeio.horario
                        is PasseioItem.Evento     -> it.passeio.horario
                    }
                }
                mixedAdapter.notifyDataSetChanged()
                tvNPasseios.text = mixedItems.size.toString()
            } else {
                Toast.makeText(requireContext(),
                    "Falha ao buscar passeios", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun atualizarAmizadeEstado() {
        val alvo = visitedUser ?: return
        val currentUser = pessoaViewModel.pessoa.value?.usuario ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            // checa se são amigos
            val respFriend: Response<Map<String,Boolean>> = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.isFriend(currentUser, alvo)
            }
            ehAmigo = respFriend.body()?.get("amigo") ?: false

            // se não amigos, checa pedido pendente
            if (!ehAmigo) {
                // 2) Busca notificações que você enviou ao alvo
                val respSent = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getNotificacoes(alvo)
                }
                val pendenteEnviado = respSent.body()
                    ?.any { it.usuarioRemetente == currentUser && !it.lido }
                    ?: false

                // 3) Busca notificações que o alvo enviou a você
                val respReceived = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getNotificacoes(currentUser)
                }
                val pendenteRecebido = respReceived.body()
                    ?.any { it.usuarioRemetente == alvo && !it.lido }
                    ?: false

                pedidoEnviado = pendenteEnviado || pendenteRecebido
            }

            withContext(Dispatchers.Main) {
                atualizarBotao()
            }
        }
    }

    private fun atualizarBotao() {
        when {
            ehAmigo -> {
                btnAdd.text = "Desfazer amizade"
                btnAdd.isEnabled = true // back end não implementou remoção ainda
            }
            pedidoEnviado -> {
                btnAdd.text = "Solicitado"
                btnAdd.isEnabled = false
            }
            else -> {
                btnAdd.text = "Adicionar"
                btnAdd.isEnabled = true
            }
        }
    }

    private fun preencherDados(p: Pessoa) {
        tvNome.text      = p.name
        tvUser.text      = "${p.usuario}"
        tvDescricao.text = p.description
        tvNAmizades.text = p.qtAmigos.toString()
        tvNPasseios.text = p.qtPasseios.toString()
        if (!p.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(p.imageUrl)
                .into(imgPerfil)
        } else {
        }
    }
}