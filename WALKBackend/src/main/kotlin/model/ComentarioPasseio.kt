package br.com.model
import kotlinx.serialization.Serializable

@Serializable
class ComentarioPasseio(
    var idComentarioPasseio: Int,
    var imagem: String,
    var nomePessoa: String,
    var usuario: String,
    var horario: String,
    var descricaoComentario: String
) {}
