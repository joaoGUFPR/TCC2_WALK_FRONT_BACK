package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class Empresa(
    var usuario : String,
    var name: String,
    var description: String,
    var imageUrl: String? = null,
    var senha: String,
    var email: String,
    var cnpj: String
)