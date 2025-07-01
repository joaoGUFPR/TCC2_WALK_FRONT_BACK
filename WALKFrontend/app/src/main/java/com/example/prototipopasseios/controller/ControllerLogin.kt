// ControllerLogin.kt
package com.example.prototipopasseios.controller

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.prototipopasseios.R
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.util.Locale

class ControllerLogin : Fragment() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var novaConta: Button
    private lateinit var pessoaViewModel: PessoaViewModel
    private lateinit var empresaViewModel: EmpresaViewModel

    private lateinit var fusedClient: FusedLocationProviderClient
    private val LOCATION_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_controller_login, container, false)
        emailEditText    = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton      = view.findViewById(R.id.loginButton)
        novaConta        = view.findViewById(R.id.createAccountButton)
        pessoaViewModel  = ViewModelProvider(requireActivity()).get(PessoaViewModel::class.java)
        empresaViewModel = ViewModelProvider(requireActivity()).get(EmpresaViewModel::class.java)

        // Inicializa o Fused Location Provider
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        loginButton.setOnClickListener {
            checkLocationPermissionAndLogin()
        }
        novaConta.setOnClickListener {
            goToManterPessoa()
        }

        return view
    }

    private fun checkLocationPermissionAndLogin() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST
            )
        } else {
            fetchMunicipioAndLogin()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchMunicipioAndLogin()
        } else {
            // Sem permissão, segue sem município
            performLoginAndUpdateMunicipio("")
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchMunicipioAndLogin() {
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                val city = if (location != null) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?: emptyList()

                    if (list.isNotEmpty()) {
                        val addr = list[0]
                        // Toast de debug com várias infos
                        val debug = """
                        addressLine: ${addr.getAddressLine(0)}
                        locality:    ${addr.locality}
                        subAdmin:    ${addr.subAdminArea}
                        admin:       ${addr.adminArea}
                    """.trimIndent()
                        Toast.makeText(requireContext(), debug, Toast.LENGTH_LONG).show()

                        // prefer locality, senão subAdminArea ou adminArea
                        addr.locality
                            ?: addr.subAdminArea
                            ?: addr.adminArea
                            ?: ""
                    } else {
                        ""
                    }
                } else {
                    ""
                }

                // aí você tem seu 'city' preenchido (ou em branco)
                performLoginAndUpdateMunicipio(city)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Falha ao obter localização", Toast.LENGTH_LONG).show()
                performLoginAndUpdateMunicipio("")
            }
    }

    private fun performLoginAndUpdateMunicipio(municipio: String) {
        val usuarioInput = emailEditText.text.toString().trim()  // → pode ser “@joaog” ou “12.345.678/0001-99”
        val senhaInput   = passwordEditText.text.toString().trim()
        if (usuarioInput.isEmpty() || senhaInput.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha usuário/CNPJ e senha", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioBody = usuarioInput.toRequestBody("text/plain".toMediaTypeOrNull())
        val senhaBody   = senhaInput  .toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1) Chama o login e recebe o JSON cru (pode ser Pessoa ou Empresa)
                val loginResp: Response<JsonElement> =
                    withContext(Dispatchers.IO) {
                        RetrofitClient.pessoaApiService.login(usuarioBody, senhaBody)
                    }

                if (!loginResp.isSuccessful || loginResp.body() == null) {
                    Toast.makeText(
                        requireContext(),
                        "Usuário ou senha inválidos",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val json = loginResp.body()!!

                // 2) Decide qual modelo usar olhando as chaves do JSON
                if (json.asJsonObject.has("qtAmigos")) {
                    // → é Pessoa
                    val pessoa = Gson().fromJson(json, com.example.prototipopasseios.model.Pessoa::class.java)

                    // atualiza município se necessário
                    if (municipio.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.updateMunicipio(
                                pessoa.usuario,
                                municipio
                            )
                        }
                    }

                    // injeta no ViewModel e navega para Perfil Pessoa
                    val pessoaFinal = pessoa.copy(municipio = municipio)
                    pessoaViewModel.setPessoa(pessoaFinal)

                    if (pessoaFinal.usuario == "@adm") {
                        (activity as? MainActivity)
                            ?.replaceFragment(
                                ControllerADM.newInstance("", ""),
                                addToBackStack = false
                            )
                    } else {
                        goToPerfilPessoa()
                    }

                } else if (json.asJsonObject.has("cnpj")) {
                    // → é Empresa
                    val empresa = Gson().fromJson(json, com.example.prototipopasseios.model.Empresa::class.java)

                    // injeta no ViewModel e navega para Perfil Empresa
                    empresaViewModel.setEmpresa(empresa)
                    goToPerfilEmpresa()

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Resposta de login inesperada",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun goToPerfilPessoa() {
        (activity as? MainActivity)
            ?.replaceFragment(
                ControllerPerfil.newInstance("", ""),
                addToBackStack = false
            )
    }

    private fun goToPerfilEmpresa() {
        (activity as? MainActivity)
            ?.replaceFragment(
                ControllerPerfilEmpresa.newInstance(""),
                addToBackStack = false
            )
    }

    private fun goToManterPessoa() {
        (activity as? MainActivity)?.replaceFragment(ControllerEscolhaTipo())
    }

    companion object {
        @JvmStatic fun newInstance() = ControllerLogin()
    }
}
