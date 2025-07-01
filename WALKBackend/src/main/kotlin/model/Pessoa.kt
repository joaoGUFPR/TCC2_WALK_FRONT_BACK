package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class Pessoa(
    var usuario : String,
    var name: String,
    var description: String,
    var imageUrl: String? = null,
    var qtAmigos: Int,
    var qtPasseios: Int,
    var municipio: String,
    var senha: String,
    var email: String,
    var nascimento: String,
    var qtEmpresas: Int
)