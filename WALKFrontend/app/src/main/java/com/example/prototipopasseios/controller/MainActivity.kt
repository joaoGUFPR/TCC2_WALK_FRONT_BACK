package com.example.prototipopasseios.controller

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.prototipopasseios.R
import com.example.prototipopasseios.databinding.ActivityMainBinding
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pessoaViewModel: PessoaViewModel
    private lateinit var empresaViewModel: EmpresaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Primeiro fragment: Login, sem back-stack
        if (savedInstanceState == null) {
            replaceFragment(ControllerLogin.newInstance(), addToBackStack = false)
        }

        pessoaViewModel = ViewModelProvider(this)[PessoaViewModel::class.java]
        empresaViewModel = ViewModelProvider(this)[EmpresaViewModel::class.java]

        // Atualiza bottomBar sempre que o back-stack muda
        supportFragmentManager.addOnBackStackChangedListener {
            updateBottomBarVisibility()
        }


        // Padding para system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Botões da bottomBar
        binding.navHome.setOnClickListener {
            replaceFragment(ControllerEvento(), addToBackStack = true)
        }
        binding.navSearch.setOnClickListener {
            when {
                // 1º: se for **pessoa** logada, vai para ControllerPerfil
                pessoaViewModel.pessoa.value != null -> {
                    replaceFragment(ControllerPerfil.newInstance("", ""), addToBackStack = true)
                }
                // 2º: senão, se for empresa logada, vai para ControllerPerfilEmpresa
                empresaViewModel.empresa.value != null -> {
                    val usuarioEmpresa = empresaViewModel.empresa.value!!.usuario
                    replaceFragment(ControllerPerfilEmpresa.newInstance(usuarioEmpresa), addToBackStack = true)
                }
                // 3º: se nenhum, joga pro login de pessoa
                else -> {
                    replaceFragment(ControllerLogin.newInstance(), addToBackStack = false)
                }
            }
        }
        binding.navProfile.setOnClickListener {
            replaceFragment(ControllerComunidade(), addToBackStack = true)
        }

        // Primeira avaliação de visibilidade
        updateBottomBarVisibility()
    }

    private fun adjustStatusBarIconsColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController
                ?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else {
            window.decorView.systemUiVisibility = 0
        }
    }

    /**
     * Substitui o fragmento no container.
     * Sempre usa commit() (assíncrono) + executePendingTransactions() para aplicar imediatamente.
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.frameLayout, fragment)

        if (addToBackStack) {
            tx.addToBackStack(null)
        }
        tx.commit()
        // Força a execução imediata, como no seu código antigo
        fm.executePendingTransactions()

        updateBottomBarVisibility()
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count > 0) {
            supportFragmentManager.popBackStackImmediate()
            updateBottomBarVisibility()
        } else {
            super.onBackPressed()
        }
    }

    /** Exibe ou oculta a bottomBar dependendo do fragmento atual */
    private fun updateBottomBarVisibility() {
        val ocultos = listOf(
            ControllerLogin::class,
            ControllerEscolhaTipo::class,
            ControllerManterPessoa::class,
            ControllerManterEmpresa::class,
            ControllerADM::class,
            ControllerUsuarioADM::class,
            ControllerComunidadeADM::class,
            ControllerUsuarioADM::class,
            ControllerEventoADM::class,
            ControllerEmpresaADM::class
        )
        val current = supportFragmentManager.findFragmentById(R.id.frameLayout)
        val mostrar = current?.let { it::class !in ocultos } ?: true
        binding.bottomBar.visibility = if (mostrar) View.VISIBLE else View.GONE
    }
}
