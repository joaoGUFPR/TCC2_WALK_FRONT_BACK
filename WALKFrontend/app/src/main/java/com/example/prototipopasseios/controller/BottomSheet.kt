package com.example.prototipopasseios.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.prototipopasseios.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ComunidadeBottomSheetFragment(
    private val nomeAdministrador: String,
    private val user: String,
    private val descricao: String,
    private val regras: String
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheetbottom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var tVNomeADM = view.findViewById<TextView>(R.id.tvAdministrador)
        var tvUser = view.findViewById<TextView>(R.id.tvUser)
        var tvDescricao = view.findViewById<TextView>(R.id.tvDescricao)
        var tvRegras = view.findViewById<TextView>(R.id.tvRegras)

        // Configurar os textos do Bottom Sheet
        tVNomeADM.text = nomeAdministrador
        tvUser.text = user
        tvDescricao.text = descricao
        tvRegras.text = regras
    }
}
