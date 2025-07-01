package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatPasseio(
    val idChat: Int,
    val usuarioAdministrador: String,
    val nome: String,          // pode ser nome do evento ou nome da comunidade
    val membros: List<String>,
    val tipo: String           // "EVENTO" ou "COMUNIDADE"
)