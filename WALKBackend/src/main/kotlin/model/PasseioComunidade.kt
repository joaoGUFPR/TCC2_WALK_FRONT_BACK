package br.com.model
import kotlinx.serialization.Serializable

@Serializable
class PasseioComunidade(
    var idPasseioComunidade: Int,
    var usuario: String,
    var imagem: String? = null,
    var nomePessoa: String,
    var horario: String,
    var descricaoPasseio: String,
    var localizacao: String
) {
}