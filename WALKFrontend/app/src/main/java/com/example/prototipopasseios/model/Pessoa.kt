package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pessoa(
    val usuario: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val qtAmigos: Int,
    val qtPasseios: Int,
    val municipio: String,
    var senha: String,
    var email: String,
    var nascimento: String,
    val qtEmpresas: Int
): Parcelable
