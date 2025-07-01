package model

import kotlinx.serialization.Serializable

@Serializable
data class Notificacao(
    val usuarioDestinado: String,
    val usuarioRemetente: String,
    val descricao: String,
    val horario: String,
    val lido: Boolean
)