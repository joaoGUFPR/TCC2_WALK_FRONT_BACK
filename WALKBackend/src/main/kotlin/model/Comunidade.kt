package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class Comunidade(
    var idComunidade : Int,
    var imageUrl: String? = null,
    var name: String,
    var administratorUser: String,
    var administrator: String,
    var descricao: String,
    var regras: String,
    val municipio: String,
    val tags: ArrayList<String>
)