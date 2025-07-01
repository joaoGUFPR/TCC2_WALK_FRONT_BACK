package com.example.prototipopasseios.controller

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.example.prototipopasseios.R
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ControllerADM.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControllerADM : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var btnUsuarioADM: Button
    private lateinit var btnComunidadeADM: Button
    private lateinit var btnEventoADM: Button
    private lateinit var btnEmpresaADM: Button
    private lateinit var imageViewFuncoes: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_controller_a_d_m, container, false)

        btnUsuarioADM = root.findViewById(R.id.btnUsuarioADM)
        btnComunidadeADM = root.findViewById(R.id.btnComunidadeADM)
        btnEventoADM = root.findViewById(R.id.btnEventoADM)
        btnEmpresaADM = root.findViewById(R.id.btnEmpresaADM)
        imageViewFuncoes = root.findViewById(R.id.iVFuncoesPerfilAmigo)


        imageViewFuncoes.setOnClickListener { view ->
            showPopupMenu(view)
        }

        btnUsuarioADM.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ControllerUsuarioADM.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnComunidadeADM.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ControllerComunidadeADM.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnEmpresaADM.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ControllerEmpresaADM.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnEventoADM.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ControllerEventoADM.newInstance())
                .addToBackStack(null)
                .commit()
        }


        return root
    }


    private fun showPopupMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.functions_adm)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.SairPerfil -> {
                        realizarLogout()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun realizarLogout() {
        // limpa LiveData, etc.
        ViewModelProvider(requireActivity())[PessoaViewModel::class.java].clearPessoa()
        ViewModelProvider(requireActivity())[EmpresaViewModel::class.java].clearEmpresa()
        // esvazia todo o back-stack
        val fm = requireActivity().supportFragmentManager
        fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // chama o replaceFragment da Activity, com addToBackStack = false
        (requireActivity() as MainActivity).replaceFragment(
            ControllerLogin.newInstance(),
            addToBackStack = false
        )
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ControllerADM.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ControllerADM().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}