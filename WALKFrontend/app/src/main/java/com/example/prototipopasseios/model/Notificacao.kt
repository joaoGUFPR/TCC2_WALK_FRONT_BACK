package com.example.prototipopasseios.model

data class Notificacao(
    val usuarioDestinado: String,
    val usuarioRemetente: String,
    val descricao: String,
    val horario: String,
    val lido: Boolean
)