package com.example.prototipopasseios.controller

import android.os.Bundle
import android.app.AlertDialog
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ComentarioPasseioComunidade
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_ID_PASSEIO = "idPasseio"
private const val ARG_ID_COMUNIDADE = "idComunidade"

class ControllerComentarioPasseioComunidade : Fragment() {
    private var localPasseio: String? = null
    private var idPasseio: Int = 0
    private var param1: String? = null
    private var param2: String? = null
    private var passeio: PasseioComunidade? = null
    private lateinit var adapter: ComentarioPasseioAdapter
    private var idComunidade: Int = 0
    private lateinit var comentarios: MutableList<ComentarioPasseioComunidade>
    private lateinit var pessoaViewModel: PessoaViewModel
    private lateinit var etComentario: EditText

    // Referências para os componentes de perfil na barra superior
    private lateinit var perfilPasseio: ImageView
    private lateinit var nomePerfil: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idPasseio = it.getInt(ARG_ID_PASSEIO, 0)
            idComunidade = it.getInt(ARG_ID_COMUNIDADE, 0)
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            passeio = it.getParcelable("passeio")
            idComunidade = it.getInt("idComunidade", 0)
            localPasseio = it.getString("localizacao")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_comentario_passeio_comunidade, container, false)

        // Inicializando os componentes da UI
        val comentariosRV = view.findViewById<RecyclerView>(R.id.ComentarioRV)
        perfilPasseio = view.findViewById(R.id.ivPerfilPasseio)
        nomePerfil = view.findViewById(R.id.tvNomePerfil)
        val descricao = view.findViewById<TextView>(R.id.tvDescricao)
        val horarioPasseio = view.findViewById<TextView>(R.id.tVHorarioComentario)
        val btnCriarComentario = view.findViewById<Button>(R.id.btnCriarComentario)
        etComentario = view.findViewById(R.id.etComentario)
        val iVReticenciaComentario: ImageView = view.findViewById(R.id.iVReticenciaBarraPasseio)
        iVReticenciaComentario.setOnClickListener { view ->
            showPopupMenuReticenciaBarra(view)
        }

        val ivMapComentario = view.findViewById<ImageView>(R.id.ivMapComentario)
        ivMapComentario.setOnClickListener {
            // mostra popup igual no adapter, mas usando localPasseio
            val popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_localizacao, null)
            val tVLocaliza: TextView = popupView.findViewById(R.id.tvLocaliza)
            tVLocaliza.text = localPasseio ?: "Local não informado"

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            val loc = IntArray(2)
            ivMapComentario.getLocationOnScreen(loc)
            val x = loc[0] - ivMapComentario.width
            val y = loc[1] + ivMapComentario.height
            popupWindow.showAtLocation(ivMapComentario.rootView, Gravity.NO_GRAVITY, x, y)
        }
        passeio?.let {
            nomePerfil.text = it.nomePessoa
            if (isAdded) {
                if (!it.imagem.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.imagem)
                        .into(perfilPasseio)
                } else {
                }
            }
            descricao.text = it.descricaoPasseio
            horarioPasseio.text = it.horario
            perfilPasseio.setOnClickListener { _ ->
                val currentUser = pessoaViewModel.pessoa.value?.usuario
                if (it.usuario == currentUser) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerPerfil())
                        .addToBackStack(null)
                        .commit()
                } else {
                    val frag = ControllerPerfilAmigo.newInstance(it.usuario)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, frag)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        pessoaViewModel = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)

        comentarios = mutableListOf()

        // Configurar RecyclerView e Adapter
        adapter = ComentarioPasseioAdapter(comentarios, requireContext(),
            click = { comentario, position ->
                // Ação ao clicar no comentário (se necessário)
            },
            deleteClick = { comentario, position ->
                // Exibe um diálogo de confirmação antes de excluir
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Comentário")
                    .setMessage("Deseja realmente excluir este comentário?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService.deleteComentario(comentario.idComentarioPasseio ?: 0)
                                }
                                if (response.isSuccessful) {
                                    if (!isAdded) return@launch
                                    comentarios.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                } else {
                                    if (!isAdded) return@launch
                                    Toast.makeText(requireContext(), "Falha ao excluir comentário", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {

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

        comentariosRV.adapter = adapter
        comentariosRV.layoutManager = LinearLayoutManager(requireContext())
        comentariosRV.setHasFixedSize(true)

        // Configura o SelectionTracker (código inalterado)
        val tracker = SelectionTracker.Builder<Long>(
            "comentarioSelection",
            comentariosRV,
            ComentarioKeyProvider(adapter),
            ComentarioDetailsLookup(comentariosRV),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        adapter.tracker = tracker

        // Configurar dados do passeio, se disponíveis
        passeio?.let { passeio ->
            descricao.text = passeio.descricaoPasseio
            horarioPasseio.text = passeio.horario
        }

        btnCriarComentario.setOnClickListener {
            createComentario()
        }



        fetchComentarios()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe os dados do usuário para atualizar a imagem e o nome do perfil

    }

    private fun fetchComentarios() {
        val idPasseioComunidade = passeio?.idPasseioComunidade
        if (idPasseioComunidade == null) {
            Toast.makeText(requireContext(), "Passeio não informado", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getComentariosByPasseio(idPasseioComunidade)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val comentariosFromApi = response.body() ?: emptyList()
                    comentarios.clear()
                    comentarios.addAll(comentariosFromApi)
                    adapter.notifyDataSetChanged()
                    if (comentariosFromApi.isEmpty()) {
                    }
                } else {

                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createComentario() {
        val textoComentario = etComentario.text.toString().trim()
        if (textoComentario.isEmpty()) {
            Toast.makeText(requireContext(), "Digite um comentário!", Toast.LENGTH_SHORT).show()
            return
        }
        val pessoa = pessoaViewModel.pessoa.value
        if (pessoa == null) {
            Toast.makeText(requireContext(), "Dados do usuário não disponíveis", Toast.LENGTH_SHORT).show()
            return
        }
        val idPasseioComunidade = passeio?.idPasseioComunidade
        if (idPasseioComunidade == 0) {
            Toast.makeText(requireContext(), "Passeio não informado", Toast.LENGTH_SHORT).show()
            return
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val horarioAtual = LocalTime.now().format(formatter)

        val idPasseioBody = idPasseioComunidade.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idComunidadeBody = idComunidade.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val usuarioBody = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val horarioBody = horarioAtual.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoComentarioBody = textoComentario.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.createComentario(
                        idPasseioBody,
                        idComunidadeBody,
                        usuarioBody,
                        horarioBody,
                        descricaoComentarioBody
                    )
                }
                if (response.isSuccessful) {
                    etComentario.text.clear()
                    fetchComentarios()
                } else {
                    Toast.makeText(requireContext(), "Erro ao criar comentário", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPopupMenuReticenciaBarra(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menubarracomentario)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.criarPasseio -> {
                    createChatComUsuarios()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun goToChat() {
        val usuarioLogado = pessoaViewModel.pessoa.value?.usuario
        if (usuarioLogado.isNullOrEmpty()) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Chama a API para obter a lista de chats do usuário logado
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getChatByUsuario(usuarioLogado)
                }
                if (response.isSuccessful) {
                    val chats = response.body() ?: emptyList()
                    if (chats.isNotEmpty()) {
                        // Considerando que o último chat criado é o último da lista.
                        // Se a ordem for diferente, você pode precisar ordenar ou buscar o chat com maior ID.
                        val ultimoChat = chats.last()

                        // Instancia o fragmento do chat utilizando o idChat do último chat encontrado
                        val chatFragment = ControllerChatPasseio.newInstance(ultimoChat.idChat)
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, chatFragment)
                            .addToBackStack(null)
                            .commit()
                    } else {

                    }
                } else {
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createChatComUsuarios() {
        val selectedIds = adapter.tracker?.selection ?: return
        if (selectedIds.isEmpty()) {
            Toast.makeText(requireContext(), "Selecione pelo menos um comentário", Toast.LENGTH_SHORT).show()
            return
        }

        // Pega os usuários selecionados nos comentários
        val selectedUsers = selectedIds.mapNotNull { selectedId ->
            adapter.comentarios.find { it.idComentarioPasseio?.toLong() == selectedId }?.usuario
        }.distinct().toMutableList()

        // Inclui o próprio criador do chat
        val pessoa = pessoaViewModel.pessoa.value ?: run {
            return
        }
        if (!selectedUsers.contains(pessoa.usuario)) {
            selectedUsers.add(pessoa.usuario)
        }

        val idPasseio = passeio?.idPasseioComunidade ?: run {
            Toast.makeText(requireContext(), "Passeio não informado", Toast.LENGTH_SHORT).show()
            return
        }

        // Monta o corpo das partes Multipart
        val idPasseioBody           = idPasseio.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val tipoBody                = "COMUNIDADE".toRequestBody("text/plain".toMediaTypeOrNull())   // <--- novo campo
        val usuarioAdministradorBody = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val usuariosStr             = selectedUsers.joinToString(separator = ",")
        val usuariosBody            = usuariosStr.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // AGORA enviamos 4 partes: idPasseio, tipo, usuarioAdministrador e lista de usuarios
                    RetrofitClient.pessoaApiService.createChatComUsuarios(
                        idPasseioBody,
                        tipoBody,
                        usuarioAdministradorBody,
                        usuariosBody
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Chat criado com sucesso!", Toast.LENGTH_SHORT).show()
                    goToChat()
                } else {
                    Toast.makeText(requireContext(), "Erro ao criar chat", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ControllerComentarioPasseioComunidade().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // Classes auxiliares para SelectionTracker (sem alterações)
    class ComentarioKeyProvider(private val adapter: ComentarioPasseioAdapter) :
        ItemKeyProvider<Long>(SCOPE_CACHED) {
        override fun getKey(position: Int): Long? {
            return adapter.getItemId(position)
        }

        class ComentarioDetailsLookup(private val recyclerView: RecyclerView) :
            ItemDetailsLookup<Long>() {
            override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
                val view = recyclerView.findChildViewUnder(e.x, e.y)
                if (view != null) {
                    val holder =
                        recyclerView.getChildViewHolder(view) as ComentarioPasseioAdapter.ComentarioPasseioViewHolder
                    return holder.getItemDetails()
                }
                return null
            }
        }

        override fun getPosition(key: Long): Int {
            for (i in 0 until adapter.itemCount) {
                if (adapter.getItemId(i) == key) return i
            }
            return RecyclerView.NO_POSITION
        }
    }
}