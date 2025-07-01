package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Evento(
    var idEvento : Int,
    var imageUrl: String? = null,
    var name: String,
    var administratorUser: String,
    var administrator: String,
    var descricao: String,
    var dataEvento: String,
    val municipios: ArrayList<String>,
    val tags: ArrayList<String>,
    val local: String
): Parcelable