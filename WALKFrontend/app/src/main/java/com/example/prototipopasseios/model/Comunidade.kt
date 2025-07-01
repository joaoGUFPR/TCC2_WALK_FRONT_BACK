package com.example.prototipopasseios.model

import android.os.Parcelable
import androidx.compose.ui.text.intl.Locale
import com.example.prototipopasseios.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Comunidade(
    var idComunidade : Int,
    var imageUrl: String? = null,
    var name: String,
    var administratorUser: String,
    var administrator: String,
    var descricao: String,
    var regras: String,
    val municipio: String,
    val tags: ArrayList<String> // Inicializa a lista de tags
): Parcelable