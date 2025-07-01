package com.example.prototipopasseios.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.R
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EventoBottomSheetFragment(
    private val nomeEmpresa: String,
    private val user: String,
    private val descricao: String,
    private val dataEvento: String,
    private val localEvento: String

) : BottomSheetDialogFragment() {
    private lateinit var empresaViewModel: EmpresaViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheetbottom_evento, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empresaViewModel = ViewModelProvider(requireActivity())[EmpresaViewModel::class.java]
        val tVNomeEmpresa = view.findViewById<TextView>(R.id.tvEmpresa)
        val tvUser = view.findViewById<TextView>(R.id.tvUserEvento)
        val tvDescricao = view.findViewById<TextView>(R.id.tvDescricaoEvento)
        val tvDataEvento = view.findViewById<TextView>(R.id.tvDataEvento)
        val tvLocalEvento = view.findViewById<TextView>(R.id.tvLocalEvento)
        tVNomeEmpresa.text = nomeEmpresa
        tvUser.text = user
        tvDescricao.text = descricao
        tvDataEvento.text = dataEvento
        tvLocalEvento.text = localEvento

        // Navega para o perfil da empresa seguida ao clicar no usuário
                tvUser.setOnClickListener {
            // Pega o usuário da empresa logada (se houver)
            val empresaLogada = empresaViewModel.empresa.value?.usuario

            val frag = if (user == empresaLogada) {
                // Se for a própria empresa, abre ControllerPerfilEmpresa
                ControllerPerfilEmpresa.newInstance(user)
            } else {
                // Caso contrário, abre ControllerPerfilEmpresaSeguida
                ControllerPerfilEmpresaSeguida.newInstance(user)
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, frag)
                .addToBackStack(null)
                .commit()

            dismiss()
        }
    }
}
