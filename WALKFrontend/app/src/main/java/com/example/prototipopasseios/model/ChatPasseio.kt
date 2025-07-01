package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ChatPasseio(
    var idChat: Int,
    var usuarioAdministrador: String,
    var nome: String,
    var ListaUsuarios: ArrayList<String>,
    val tipo: String
): Parcelable