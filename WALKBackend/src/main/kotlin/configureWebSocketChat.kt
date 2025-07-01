import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@kotlinx.serialization.Serializable
data class ChatMessageRequest(
    val idChat: Int,
    val usuario: String,
    val descricaoComentario: String
)

fun Application.configureWebSocketChat() {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val comentarioChatDAO = br.com.database.ComentarioChatDAO()
    val connections = mutableListOf<DefaultWebSocketSession>()

    routing {
        webSocket("/chat-socket") {
            connections.add(this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        try {
                            val messageRequest = Json.decodeFromString<ChatMessageRequest>(receivedText)
                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                            val horarioAtual = LocalTime.now().format(formatter)
                            val comentario = br.com.model.ComentarioChat(
                                idComentarioChat = 0,
                                usuario =messageRequest.usuario,
                                nomePessoa = "",
                                horario = horarioAtual,
                                descricaoComentario = messageRequest.descricaoComentario
                            )
                            // Insere o comentário usando somente idChat e usuário
                            val inserted = comentarioChatDAO.insertComentarioChat(
                                comentario,
                                messageRequest.idChat,
                                messageRequest.usuario
                            )
                            if (inserted) {
                                val broadcastMessage = "(${messageRequest.usuario}) [$horarioAtual]: ${messageRequest.descricaoComentario}"
                                connections.forEach { session ->
                                    session.send(broadcastMessage)
                                }
                            } else {
                                send("Erro ao salvar a mensagem no banco.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            send("Erro ao processar mensagem: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connections.remove(this)
            }
        }
    }
}
