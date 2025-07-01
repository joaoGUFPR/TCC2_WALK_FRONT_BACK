package br.com.model
import kotlinx.serialization.Serializable

@Serializable
class PasseioEvento(
    var idPasseioEvento: Int,
    var usuario: String,
    var imagem: String?     = null,
    var nomePessoa: String,      // renomeado
    var horario: String,
    var descricaoPasseio: String
) {
}