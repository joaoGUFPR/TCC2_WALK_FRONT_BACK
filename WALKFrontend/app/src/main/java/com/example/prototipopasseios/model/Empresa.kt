package com.example.prototipopasseios.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Empresa(
    val usuario : String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val senha: String,
    val email: String,
    val cnpj: String
): Parcelable