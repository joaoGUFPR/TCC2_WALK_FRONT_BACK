package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class Evento(
    var idEvento : Int,
    var imageUrl: String? = null,
    var name: String,
    var administratorUser: String,
    var administrator: String,
    var descricao: String,
    var dataEvento: String,
    val municipios: ArrayList<String>,
    val tags: ArrayList<String>,
    val local: String
)