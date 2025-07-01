// ControllerComentarioPasseioEvento.kt
package com.example.prototipopasseios.controller

import android.os.Bundle
import android.app.AlertDialog
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
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
import com.example.prototipopasseios.model.ComentarioPasseioEvento
import com.example.prototipopasseios.model.PasseioEvento
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
private const val ARG_ID_EVENTO = "idEvento"

class ControllerComentarioPasseioEvento : Fragment() {
    private var idPasseio: Int = 0
    private var param1: String? = null
    private var param2: String? = null
    private var passeio: PasseioEvento? = null
    private lateinit var adapter: ComentarioPasseioEventoAdapter
    private var idEvento: Int = 0
    private lateinit var comentarios: MutableList<ComentarioPasseioEvento>
    private lateinit var pessoaViewModel: PessoaViewModel
    private lateinit var etComentario: EditText

    private lateinit var perfilPasseio: ImageView
    private lateinit var nomePerfil: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idPasseio = it.getInt(ARG_ID_PASSEIO, 0)
            idEvento = it.getInt(ARG_ID_EVENTO, 0)
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            passeio = it.getParcelable("passeio")
            idEvento = it.getInt("idEvento", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_comentario_passeio_evento, container, false)

        val comentariosRV = view.findViewById<RecyclerView>(R.id.ComentarioPasseioEventoRV)
        perfilPasseio = view.findViewById(R.id.ivPerfilPasseioEvento)
        nomePerfil = view.findViewById(R.id.tvNomePerfilPasseioEvento)
        val descricao = view.findViewById<TextView>(R.id.tvDescricaoPasseioEvento)
        val horarioPasseio = view.findViewById<TextView>(R.id.tVHorarioComentarioPasseioEvento)
        val btnCriarComentario = view.findViewById<Button>(R.id.btnCriarComentarioPasseioEvento)
        etComentario = view.findViewById(R.id.etComentarioPasseioEvento)
        val iVReticenciaComentario: ImageView = view.findViewById(R.id.iVReticenciaBarraPasseioEvento)
        iVReticenciaComentario.setOnClickListener { v ->
            showPopupMenuReticenciaBarra(v)
        }

        passeio?.let {
            nomePerfil.text = it.nomePessoa
            if (!it.imagem.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(it.imagem)
                    .into(perfilPasseio)
            } else {
            }
            descricao.text = it.descricaoPasseio
            horarioPasseio.text = it.horario
            perfilPasseio.setOnClickListener {
                val currentUser = pessoaViewModel.pessoa.value?.usuario
                if (passeio?.usuario == currentUser) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerPerfil())
                        .addToBackStack(null)
                        .commit()
                } else {
                    val frag = ControllerPerfilAmigo.newInstance(passeio?.usuario ?: "")
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, frag)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        pessoaViewModel = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)

        comentarios = mutableListOf()

        adapter = ComentarioPasseioEventoAdapter(
            comentarios,
            requireContext(),
            click = { _, _ -> },
            deleteClick = { comentario, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Comentário")
                    .setMessage("Deseja realmente excluir este comentário?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.pessoaApiService
                                        .deleteComentarioEvento(comentario.idComentarioPasseioEvento ?: 0)
                                }
                                if (response.isSuccessful) {
                                    comentarios.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                } else {

                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            imageClick = { comentario, _ ->
                val currentUser = pessoaViewModel.pessoa.value?.usuario
                if (comentario.usuario == currentUser) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerPerfil())
                        .addToBackStack(null)
                        .commit()
                } else {
                    val frag = ControllerPerfilAmigo.newInstance(comentario.usuario)
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

        val tracker = SelectionTracker.Builder<Long>(
            "comentarioEventoSelection",
            comentariosRV,
            ComentarioKeyProvider(adapter),
            ComentarioDetailsLookup(comentariosRV),
            StorageStrategy.createLongStorage()
        )
            .withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        adapter.tracker = tracker

        btnCriarComentario.setOnClickListener {
            createComentario()
        }

        fetchComentarios()
        return view
    }

    private fun fetchComentarios() {
        val idPasseioEvento = passeio?.idPasseioEvento
        if (idPasseioEvento == null) {
            Toast.makeText(requireContext(), "Passeio não informado", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getComentariosByPasseioEvento(idPasseioEvento)
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
            return
        }
        val pessoa = pessoaViewModel.pessoa.value
        if (pessoa == null) {
            return
        }
        val idPasseioEvento = passeio?.idPasseioEvento
        if (idPasseioEvento == 0) {
            Toast.makeText(requireContext(), "Passeio não informado", Toast.LENGTH_SHORT).show()
            return
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val horarioAtual = LocalTime.now().format(formatter)

        val idPasseioBody = idPasseioEvento.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idEventoBody = idEvento.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val usuarioBody = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val horarioBody = horarioAtual.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoComentarioBody = textoComentario.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.createComentarioEvento(
                        idPasseioBody,
                        idEventoBody,
                        usuarioBody,
                        horarioBody,
                        descricaoComentarioBody
                    )
                }
                if (response.isSuccessful) {
                    etComentario.text.clear()
                    fetchComentarios()
                } else {
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
            adapter.comentarios.find { it.idComentarioPasseioEvento?.toLong() == selectedId }?.usuario
        }.distinct().toMutableList()

        // Inclui o próprio criador do chat
        val pessoa = pessoaViewModel.pessoa.value
        if (pessoa == null) {
            return
        }
        if (!selectedUsers.contains(pessoa.usuario)) {
            selectedUsers.add(pessoa.usuario)
        }

        val idPasseio = passeio?.idPasseioEvento
        if (idPasseio == null || idPasseio == 0) {
            return
        }

        // --- Aqui começamos as mudanças: criar também o campo "tipo" = "EVENTO" ---
        val idPasseioBody           = idPasseio.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val tipoBody                = "EVENTO".toRequestBody("text/plain".toMediaTypeOrNull())
        val usuarioAdministradorBody = pessoa.usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val usuariosStr             = selectedUsers.joinToString(separator = ",")
        val usuariosBody            = usuariosStr.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    // Agora enviamos 4 partes: idPasseio, tipo, usuarioAdministrador e lista de usuarios
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
            ControllerComentarioPasseioEvento().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // Ajustado para usar ComentarioPasseioEventoAdapter.ViewHolder
    class ComentarioKeyProvider(private val adapter: ComentarioPasseioEventoAdapter) :
        ItemKeyProvider<Long>(SCOPE_CACHED) {
        override fun getKey(position: Int): Long? {
            return adapter.getItemId(position)
        }

        override fun getPosition(key: Long): Int {
            for (i in 0 until adapter.itemCount) {
                if (adapter.getItemId(i) == key) return i
            }
            return RecyclerView.NO_POSITION
        }
    }

    class ComentarioDetailsLookup(private val recyclerView: RecyclerView) :
        ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            val view = recyclerView.findChildViewUnder(e.x, e.y)
            if (view != null) {
                val holder = recyclerView.getChildViewHolder(view)
                        as ComentarioPasseioEventoAdapter.ComentarioPasseioEventoViewHolder
                return holder.getItemDetails()
            }
            return null
        }
    }
}
