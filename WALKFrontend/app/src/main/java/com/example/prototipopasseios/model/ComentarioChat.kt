package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComentarioChat(
    var idComentarioChat: Int,
    var usuario: String,       // Novo campo: identificador único do usuário
    var nomePessoa: String,    // Nome para exibição (pode vir de uma consulta JOIN)
    var horario: String,
    var descricaoComentario: String
) : Parcelable










