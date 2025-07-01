package com.example.prototipopasseios.controller

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.prototipopasseios.R

class ControllerEscolhaTipo : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_escolha_tipo, container, false)

        // Corrigidos os IDs para corresponder ao XML: btnPessoa e btnEmpresa
        val btnManterPessoa = view.findViewById<Button>(R.id.btnPessoa)
        val btnManterEmpresa = view.findViewById<Button>(R.id.btnEmpresa)

        btnManterPessoa.setOnClickListener {
            goToManterPessoa()
        }

        btnManterEmpresa.setOnClickListener {
            goToManterEmpresa()
        }

        return view
    }

    private fun goToManterPessoa() {
        // Substitui o fragment atual por ControllerManterPessoa
        (activity as? MainActivity)?.replaceFragment(ControllerManterPessoa(), addToBackStack = true)
    }

    private fun goToManterEmpresa() {
        // Substitui o fragment atual por ControllerManterEmpresa
        (activity as? MainActivity)?.replaceFragment(ControllerManterEmpresa(), addToBackStack = true)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ControllerEscolhaTipo()
    }
}
