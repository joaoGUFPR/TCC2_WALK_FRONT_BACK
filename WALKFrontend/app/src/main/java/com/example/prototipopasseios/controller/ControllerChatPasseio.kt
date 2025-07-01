package com.example.prototipopasseios.controller

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.ComentarioChat
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ControllerChatPasseio : Fragment() {

    private lateinit var adapter: ComentarioChatAdapter
    private lateinit var messages: MutableList<ComentarioChat>
    private lateinit var etComentarioChat: androidx.appcompat.widget.AppCompatEditText
    private lateinit var btnCriarComentarioChat: androidx.appcompat.widget.AppCompatButton

    // ViewModel com os dados do usuário
    private lateinit var pessoaViewModel: PessoaViewModel

    // idChat: identificador único do chat
    private var idChat: Int = 0

    // Propriedades do WebSocket
    private lateinit var webSocket: WebSocket
    private val webSocketUrl = "ws://192.168.0.89:8080/chat-socket"

    // Variável para gerar IDs de mensagem (para exibição local)
    private var nextMessageId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messages = mutableListOf()
        // Recupera o idChat dos argumentos
        idChat = arguments?.getInt("idChat") ?: 0
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = inflater.inflate(R.layout.fragment_controller_chat_passeio, container, false)

        // Inicializa o ViewModel
        pessoaViewModel = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)

        val recyclerView = view.findViewById<RecyclerView>(R.id.ComentarioChatRV)
        etComentarioChat = view.findViewById(R.id.etComentarioChat)
        btnCriarComentarioChat = view.findViewById(R.id.btnCriarComentarioChat)

        if (idChat == 0) {
        } else {
            // Carrega as mensagens históricas do chat
            fetchChatMensagens()
        }

        // Configura o RecyclerView e o Adapter
        adapter = ComentarioChatAdapter(
            messages,
            pessoaViewModel.pessoa.value?.usuario ?: "Desconhecido",
            requireContext()
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        // Estabelece a conexão via WebSocket para receber mensagens em tempo real
        connectWebSocket()

        // Envia mensagem quando o botão for clicado
        btnCriarComentarioChat.setOnClickListener {
            createComentarioChat()
        }

        return view
    }

    // Função para buscar as mensagens históricas do chat via Retrofit
    private fun fetchChatMensagens() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.getChatComentarios(idChat)
                }
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    val chatMensagens = response.body() ?: emptyList()
                    messages.clear()
                    messages.addAll(chatMensagens)
                    adapter.notifyDataSetChanged()
                } else {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), "Erro ao carregar mensagens históricas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Configura a conexão via WebSocket
    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(webSocketUrl)
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                activity?.runOnUiThread {

                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                activity?.runOnUiThread {
                    val mensagemRecebida = parseMessage(text)
                    messages.add(mensagemRecebida)
                    adapter.notifyItemInserted(messages.size - 1)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                activity?.runOnUiThread {
                }
            }
        })
    }

    // Envia uma mensagem via WebSocket
    private fun createComentarioChat() {
        val texto = etComentarioChat.text.toString().trim()
        if (TextUtils.isEmpty(texto)) return

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val horarioAtual = LocalTime.now().format(formatter)
        val usuarioLogado = pessoaViewModel.pessoa.value?.usuario ?: "Desconhecido"

        // Monta o JSON com os dados necessários
        val messageJson = """
            {
              "idChat": $idChat,
              "usuario": "$usuarioLogado",
              "descricaoComentario": "$texto"
            }
        """.trimIndent()

        webSocket.send(messageJson)
        etComentarioChat.text?.clear()
    }

    // Converte o texto recebido em um objeto ComentarioChat
    private fun parseMessage(text: String): ComentarioChat {
        // Supondo que o formato seja: "(usuario) [horario]: descricaoComentario"
        val regex = """\((.*?)\) \[(.*?)\]: (.*)""".toRegex()
        val match = regex.find(text)
        return if (match != null) {
            val (usuario, horario, descricao) = match.destructured
            ComentarioChat(
                idComentarioChat = nextMessageId++,
                usuario = usuario,
                nomePessoa = pessoaViewModel.pessoa.value?.name ?: "Desconhecido",
                horario = horario,
                descricaoComentario = descricao
            )
        } else {
            ComentarioChat(
                idComentarioChat = nextMessageId++,
                usuario = "Desconhecido",
                nomePessoa = "Desconhecido",
                horario = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                descricaoComentario = text
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webSocket.close(1000, "Fechando conexão")
    }

    companion object {
        @JvmStatic
        fun newInstance(idChat: Int) =
            ControllerChatPasseio().apply {
                arguments = Bundle().apply {
                    putInt("idChat", idChat)
                }
            }
    }
}

