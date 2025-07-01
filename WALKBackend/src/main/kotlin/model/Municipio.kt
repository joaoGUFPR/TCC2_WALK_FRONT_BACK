package br.com.model

import kotlinx.serialization.Serializable

@Serializable
data class Municipio(
    val nomeMunicipio: String,
    val estado: String,
    val pais: String
)
