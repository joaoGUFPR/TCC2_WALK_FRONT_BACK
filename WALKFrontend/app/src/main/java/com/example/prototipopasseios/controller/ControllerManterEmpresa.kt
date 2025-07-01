package com.example.prototipopasseios.controller

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.util.MaskUtil
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ControllerManterEmpresa : Fragment() {

    private lateinit var etNomeEmpresa: EditText
    private lateinit var etEmailEmpresa: EditText
    private lateinit var etUsernameEmpresa: EditText
    private lateinit var etCNPJEmpresa: EditText
    private lateinit var etSenhaEmpresa: EditText
    private lateinit var etDescricaoEmpresa: EditText
    private lateinit var btnSalvar: Button
    private lateinit var ivBackground: ImageView
    private lateinit var flBackPicture: FrameLayout
    private lateinit var ivCamera: ImageView

    private var selectedImageUri: Uri? = null
    private lateinit var empresaViewModel: EmpresaViewModel
    private var empresaAtual: Empresa? = null

    private val launchGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri = uri

            // Carrega com Glide para respeitar EXIF/orientação
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivBackground)

            // Esconde o ícone da câmera
            ivCamera.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        empresaViewModel = ViewModelProvider(requireActivity())[EmpresaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_manter_empresa, container, false)

        etNomeEmpresa      = view.findViewById(R.id.etNomeEmpresa)
        etEmailEmpresa     = view.findViewById(R.id.etEmailEmpresa)
        etUsernameEmpresa  = view.findViewById(R.id.etUsernameEmpresa)
        etSenhaEmpresa     = view.findViewById(R.id.etSenhaEmpresa)
        etCNPJEmpresa      = view.findViewById(R.id.etCNPJEmpresa)
        etDescricaoEmpresa = view.findViewById(R.id.etDescricao)
        btnSalvar          = view.findViewById(R.id.btnSalvar)
        ivBackground       = view.findViewById(R.id.ivBackground)
        flBackPicture      = view.findViewById(R.id.backPicture)
        ivCamera           = view.findViewById(R.id.ivCamera)

        etCNPJEmpresa.addTextChangedListener(
            MaskUtil.insert("##.###.###/####-##", etCNPJEmpresa)
        )
        etUsernameEmpresa.addTextChangedListener(MaskUtil.enforcePrefix("@", etUsernameEmpresa))
        // permite trocar a imagem de perfil
        flBackPicture.setOnClickListener {
            val pick = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            launchGallery.launch(pick)
        }

        btnSalvar.setOnClickListener {
            if (empresaAtual != null) updateEmpresa() else cadastrarEmpresa()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observa o objeto Empresa logada/selecionada
        empresaViewModel.empresa.observe(viewLifecycleOwner) { e ->
            if (e == null) {
                // Se não há empresa logada, limpa o formulário
                empresaAtual = null
                etUsernameEmpresa.setText("")
                etEmailEmpresa.setText("")
                etNomeEmpresa.setText("")
                etCNPJEmpresa.setText("")
                etDescricaoEmpresa.setText("")
                btnSalvar.text = "Cadastrar"
                ivCamera.visibility = View.VISIBLE
                return@observe
            }

            // Preenche o formulário com os dados de e
            empresaAtual = e
            etUsernameEmpresa.setText(e.usuario)
            etUsernameEmpresa.isEnabled = false
            etEmailEmpresa.setText(e.email)
            etNomeEmpresa.setText(e.name)
            etCNPJEmpresa.setText(e.cnpj)
            etDescricaoEmpresa.setText(e.description)

            if (!e.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(e.imageUrl)
                    .centerCrop()
                    .into(ivBackground)
                ivCamera.visibility = View.GONE
            } else {
                ivCamera.visibility = View.VISIBLE
            }

            btnSalvar.text = "Atualizar Perfil"
        }
    }

    private fun cadastrarEmpresa() {
        val usuario   = etUsernameEmpresa.text.toString().trim()
        val nome      = etNomeEmpresa.text.toString().trim()
        val email     = etEmailEmpresa.text.toString().trim()
        val cnpj      = etCNPJEmpresa.text.toString().trim()
        val descricao = etDescricaoEmpresa.text.toString().trim()
        val senhaDefault = etSenhaEmpresa.text.toString().trim()

        if (listOf(usuario, nome, email, cnpj, descricao).any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioBody   = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val senhaBody     = senhaDefault.toRequestBody("text/plain".toMediaTypeOrNull())
        val nomeBody      = nome.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody = descricao.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody     = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val cnpjBody      = cnpj.toRequestBody("text/plain".toMediaTypeOrNull())

        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            getFileFromUri(uri)?.takeIf(File::exists)?.let { file ->
                val req = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, req)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.createEmpresa(
                    usuarioBody,
                    senhaBody,
                    nomeBody,
                    descricaoBody,
                    emailBody,
                    imagePart,
                    cnpjBody
                )
            }
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Empresa cadastrada com sucesso", Toast.LENGTH_LONG).show()
                    // Ao cadastrar, vamos ao login
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerLogin.newInstance())
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Falha no cadastro", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateEmpresa() {
        val nome      = etNomeEmpresa.text.toString().trim()
        val descricao = etDescricaoEmpresa.text.toString().trim()
        val email     = etEmailEmpresa.text.toString().trim()
        val cnpj      = etCNPJEmpresa.text.toString().trim()

        if (listOf(nome, descricao, email, cnpj).any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val e = empresaAtual!! // seguro porque só chamamos update quando não é nulo

        val nameBody     = nome.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody     = descricao.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody    = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val cnpjBody     = cnpj.toRequestBody("text/plain".toMediaTypeOrNull())
        val existingUrl  = selectedImageUri?.let { "" } ?: e.imageUrl.orEmpty()
        val existingBody = existingUrl.toRequestBody("text/plain".toMediaTypeOrNull())

        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            getFileFromUri(uri)?.takeIf(File::exists)?.let { file ->
                val req = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, req)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.updateEmpresa(
                    e.usuario,
                    nameBody,
                    descBody,
                    emailBody,
                    cnpjBody,
                    existingBody,
                    imagePart
                )
            }
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Empresa atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                    empresaViewModel.buscarEmpresa(e.usuario)
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Falha ao atualizar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? = try {
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            File.createTempFile("upload_", ".jpg", requireContext().cacheDir).also { tmp ->
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
