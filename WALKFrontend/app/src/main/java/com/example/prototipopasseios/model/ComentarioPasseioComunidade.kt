package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ComentarioPasseioComunidade(
    var idComentarioPasseio: Int,
    var imagem: String,
    var nomePessoa: String,
    val usuario: String,
    var horario: String,
    var descricaoComentario: String
): Parcelable