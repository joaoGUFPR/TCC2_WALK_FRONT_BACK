package br.com.model

import kotlinx.serialization.Serializable

@Serializable
class ComentarioChat(
    var idComentarioChat: Int,
    var usuario: String,
    var nomePessoa: String,
    var horario: String,
    var descricaoComentario: String
) {

}

