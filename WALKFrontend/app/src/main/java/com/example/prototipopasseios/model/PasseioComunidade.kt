package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PasseioComunidade(
    var idPasseioComunidade: Int,
    var imagem: String? = null,
    var nomePessoa: String,
    var usuario: String,
    var horario: String,
    var descricaoPasseio: String,
    var localizacao: String
): Parcelable