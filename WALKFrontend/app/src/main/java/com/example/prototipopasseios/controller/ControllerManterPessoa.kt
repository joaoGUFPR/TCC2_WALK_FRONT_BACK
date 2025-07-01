package com.example.prototipopasseios.controller

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.util.MaskUtil
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ControllerManterPessoa : Fragment() {

    private lateinit var etNome: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etNascimento: EditText
    private lateinit var etDescricao: EditText
    private lateinit var btnSalvar: Button
    private lateinit var ivBackground: ImageView
    private lateinit var flBackPicture: FrameLayout
    private lateinit var ivCamera: ImageView

    private var selectedImageUri: Uri? = null
    private lateinit var pessoaViewModel: PessoaViewModel
    private var perfilAtual: Pessoa? = null

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
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val view = inflater.inflate(R.layout.fragment_controller_manter_pessoa, container, false)

        etNome        = view.findViewById(R.id.etNome)
        etEmail       = view.findViewById(R.id.etEmail)
        etUsername    = view.findViewById(R.id.etUsername)
        etPassword    = view.findViewById(R.id.etPassword)
        etNascimento  = view.findViewById(R.id.etNascimento)
        etDescricao   = view.findViewById(R.id.etDescricao)
        btnSalvar     = view.findViewById(R.id.btnSalvar)
        ivBackground  = view.findViewById(R.id.ivBackground)
        flBackPicture = view.findViewById(R.id.backPicture)
        ivCamera      = view.findViewById(R.id.ivCamera)


        // permite trocar a imagem de perfil
        flBackPicture.setOnClickListener {
            val pick = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            launchGallery.launch(pick)
        }

        btnSalvar.setOnClickListener {
            if (perfilAtual != null) updatePessoa() else cadastrarPessoa()
        }

        etNascimento.addTextChangedListener(MaskUtil.insert("##/##/####", etNascimento))
        etUsername.addTextChangedListener(MaskUtil.enforcePrefix("@", etUsername))

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Só preenche o formulário se p for não-nulo
        pessoaViewModel.pessoa.observe(viewLifecycleOwner) { p ->
            if (p == null) {
                // Se não há pessoa logada, limpa o formulário
                perfilAtual = null
                etUsername.setText("")
                etPassword.setText("")
                etEmail.setText("")
                etNome.setText("")
                etNascimento.setText("")
                etDescricao.setText("")
                btnSalvar.text = "Cadastrar"
                ivCamera.visibility = View.VISIBLE
                return@observe
            }

            // Agora podemos usar p com segurança
            perfilAtual = p
            etUsername.setText(p.usuario)
            etUsername.isEnabled = false
            etPassword.setText(p.senha)
            etEmail.setText(p.email)
            etNome.setText(p.name)
            etNascimento.setText(p.nascimento)
            etDescricao.setText(p.description)
            if (!p.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(p.imageUrl)
                    .centerCrop()
                    .into(ivBackground)
                ivCamera.visibility = View.GONE
            } else {
                ivCamera.visibility = View.VISIBLE
            }

            btnSalvar.text = "Atualizar Perfil"
        }
    }

    private fun cadastrarPessoa() {
        val usuario   = etUsername.text.toString().trim()
        val nome      = etNome.text.toString().trim()
        val email     = etEmail.text.toString().trim()
        val senha     = etPassword.text.toString().trim()
        val nascimento= etNascimento.text.toString().trim()
        val descricao = etDescricao.text.toString().trim()
        val qtAmigos   = 0
        val qtEmpresas   = 0
        val qtPasseios = 0
        val municipio  = "Não informado"

        if (listOf(usuario, nome, email, senha, nascimento, descricao).any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioBody    = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
        val senhaBody      = senha.toRequestBody("text/plain".toMediaTypeOrNull())
        val nomeBody       = nome.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody  = descricao.toRequestBody("text/plain".toMediaTypeOrNull())
        val qtAmigosBody   = qtAmigos.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val qtPasseiosBody = qtPasseios.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val municipioBody  = municipio.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody      = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val nascBody       = nascimento.toRequestBody("text/plain".toMediaTypeOrNull())
        val qtEmpresasBody   = qtEmpresas.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            getFileFromUri(uri)?.takeIf(File::exists)?.let { file ->
                val req = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, req)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.createPessoa(
                    usuarioBody, senhaBody, nomeBody, descricaoBody,
                    qtAmigosBody, qtPasseiosBody, municipioBody,
                    emailBody, nascBody, imagePart, qtEmpresasBody
                )
            }
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Cadastrado com sucesso", Toast.LENGTH_LONG).show()
                    pessoaViewModel.buscarPessoa(usuario)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerLogin.newInstance())
                        .commit()
                } else {
                    Toast.makeText(requireContext(), "Falha no cadastro", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updatePessoa() {
        // ... mesma lógica, mas agora perfilAtual é garantido não-null ...
        val nome      = etNome.text.toString().trim()
        val descricao = etDescricao.text.toString().trim()
        val email     = etEmail.text.toString().trim()
        val nascimento= etNascimento.text.toString().trim()

        if (listOf(nome, descricao, email, nascimento).any { it.isEmpty() }) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val user = perfilAtual!!  // seguro porque só chamamos update quando perfilAtual != null

        val nameBody    = nome.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody    = descricao.toRequestBody("text/plain".toMediaTypeOrNull())
        val muniBody    = user.municipio.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody   = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val nascBody    = nascimento.toRequestBody("text/plain".toMediaTypeOrNull())
        val existingUrl = selectedImageUri?.let { "" } ?: user.imageUrl.orEmpty()
        val existingBody= existingUrl.toRequestBody("text/plain".toMediaTypeOrNull())

        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            getFileFromUri(uri)?.takeIf(File::exists)?.let { file ->
                val req = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, req)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.updatePessoa(
                    user.usuario, nameBody, descBody, muniBody,
                    emailBody, nascBody, existingBody, imagePart
                )
            }
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                    pessoaViewModel.buscarPessoa(user.usuario)
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
