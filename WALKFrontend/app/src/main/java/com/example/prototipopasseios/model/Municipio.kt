// arquivo: app/src/main/java/com/example/prototipopasseios/model/Municipio.kt
package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Municipio(
    val nomeMunicipio: String,
    val estado: String,
    val pais: String
) : Parcelable
