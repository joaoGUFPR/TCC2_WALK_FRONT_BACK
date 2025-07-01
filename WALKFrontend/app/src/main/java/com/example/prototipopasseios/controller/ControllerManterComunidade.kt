package com.example.prototipopasseios.controller

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Comunidade
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ControllerManterComunidade.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControllerManterComunidade : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var pessoaViewModel: PessoaViewModel
    private var param1: String? = null
    private var param2: String? = null
    private val perfil: Pessoa?
        get() = pessoaViewModel.pessoa.value
    private lateinit var cidadeTextView: TextView
    private lateinit var nomeComunidade: TextView
    private lateinit var descricao: TextView
    private lateinit var regras: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var buttonCreate: Button
    private lateinit var imageViewCamera: ImageView
    private lateinit var imageViewBackground: ImageView
    private lateinit var backPicture: FrameLayout
    private lateinit var checkedChips: ArrayList<String>
    private var selectedImageUri: android.net.Uri? = null
    private var comunidadeEdicao: Comunidade? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            comunidadeEdicao = it.getParcelable("comunidade")

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        val rootView = inflater.inflate(R.layout.fragment_controller_manter_comunidade, container, false)
        cidadeTextView = rootView.findViewById<TextView>(R.id.tVCidade)
        nomeComunidade = rootView.findViewById<TextView>(R.id.etNomeComunidade)
        descricao = rootView.findViewById<TextView>(R.id.etDescricao)
        regras = rootView.findViewById<TextView>(R.id.etRegras)
        chipGroup = rootView.findViewById<ChipGroup>(R.id.chipGroup)
        buttonCreate = rootView.findViewById<Button>(R.id.btnCriarComunidade)
        imageViewCamera = rootView.findViewById<ImageView>(R.id.ivCamera)
        imageViewBackground = rootView.findViewById<ImageView>(R.id.ivBackground)
        backPicture = rootView.findViewById<FrameLayout>(R.id.backPicture)
        checkedChips = arrayListOf<String>()

        cidadeTextView?.text = perfil?.municipio

        chipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                buttonCreate?.isEnabled = false
            } else {
                checkedChips.clear()
                checkedIds.forEach { idx ->
                    val chip = rootView.findViewById<Chip>(idx)
                    chip?.let { checkedChips.add(it.text.toString()) }
                }
            }
        }

        comunidadeEdicao?.let { comunidade ->
            nomeComunidade.setText(comunidade.name)
            descricao.setText(comunidade.descricao)
            regras.setText(comunidade.regras)
            // Para as tags, você pode exibir os chips já marcados (dependendo de sua implementação)
            // Para a imagem, se o campo imageUrl não for nulo, carregue-a:
            comunidade.imageUrl?.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .into(imageViewBackground)
                imageViewCamera.visibility = View.GONE
            }
            buttonCreate.text = "Atualizar"
        }

        buttonCreate.setOnClickListener {
            if (comunidadeEdicao != null) {
                updateComunidade()
            } else {
                createComunidade()
            }
        }

        val launchGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                    imageViewBackground.setImageBitmap(bitmap)
                    imageViewCamera.visibility = View.GONE
                }
            }
        }
        backPicture.setOnClickListener {
            val openGallery = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            launchGallery.launch(openGallery)
        }




        return rootView
    }

    private fun createComunidade() {


        val administrador = perfil ?: run {
            Toast.makeText(requireContext(),
                "Erro: usuário não está logado.", Toast.LENGTH_LONG).show()
            return
        }
        // Coleta os dados da UI
        val nameText = nomeComunidade.text.toString()
        val adminUserText = perfil?.usuario ?: "Desconhecido"
        val descricaoText = descricao.text.toString()
        val regrasText = regras.text.toString()
        val municipioText = perfil?.municipio ?: "Desconhecido"
        val tagsText = checkedChips.joinToString(separator = ",")  // Junta as tags separadas por vírgula

        // Cria os RequestBody
        val nameBody = nameText.toRequestBody("text/plain".toMediaTypeOrNull())
        val adminUserBody = adminUserText.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody = descricaoText.toRequestBody("text/plain".toMediaTypeOrNull())
        val regrasBody = regrasText.toRequestBody("text/plain".toMediaTypeOrNull())
        val municipioBody = municipioText.toRequestBody("text/plain".toMediaTypeOrNull())
        val tagsBody = tagsText.toRequestBody("text/plain".toMediaTypeOrNull())

        // Cria o MultipartBody.Part para a imagem, se houver
        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null && file.exists()) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            } else null
        }

        // Chama a API usando coroutines
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.createComunidade(
                        nameBody, adminUserBody, descricaoBody, regrasBody, municipioBody, tagsBody, imagePart
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Comunidade criada com sucesso", Toast.LENGTH_LONG).show()
                    goToComunidades()
                } else {
                    Toast.makeText(requireContext(), "Erro ao criar comunidade", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateComunidade() {
        val nameText = nomeComunidade.text.toString()
        val adminUserText = perfil!!.usuario
        val descricaoText = descricao.text.toString()
        val regrasText = regras.text.toString()
        val municipioText = cidadeTextView.text.toString()
        val tagsText = checkedChips.joinToString(separator = ",")

        val nameBody = nameText.toRequestBody("text/plain".toMediaTypeOrNull())
        val adminUserBody = adminUserText.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody = descricaoText.toRequestBody("text/plain".toMediaTypeOrNull())
        val regrasBody = regrasText.toRequestBody("text/plain".toMediaTypeOrNull())
        val municipioBody = municipioText.toRequestBody("text/plain".toMediaTypeOrNull())
        val tagsBody = tagsText.toRequestBody("text/plain".toMediaTypeOrNull())

        // Se uma nova imagem for selecionada, crie o MultipartBody.Part; caso contrário, mantenha a URL existente
        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null && file.exists()) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            } else null
        }

        // Se o usuário não selecionou uma nova imagem, pegue a URL existente
        val existingImageUrlValue = if (selectedImageUri == null) {
            comunidadeEdicao?.imageUrl.orEmpty()
        } else {
            ""  // ou simplesmente enviar string vazia, pois o novo arquivo será utilizado
        }
        val existingImageUrlBody = existingImageUrlValue.toRequestBody("text/plain".toMediaTypeOrNull())

        val id = comunidadeEdicao?.idComunidade ?: 0

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.pessoaApiService.updateComunidade(
                        id,
                        nameBody, adminUserBody, descricaoBody, regrasBody, municipioBody, tagsBody,
                        existingImageUrlBody,
                        imagePart
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Comunidade atualizada com sucesso", Toast.LENGTH_LONG).show()
                    goToComunidades()
                } else {
                    Toast.makeText(requireContext(), "Erro ao atualizar comunidade", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }




    fun getRealPathFromURI(uri: android.net.Uri): String {
        var filePath = ""
        val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                filePath = it.getString(columnIndex)
            }
        }
        return filePath
    }
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun goToComunidades() {
        val fragment = ControllerComunidade()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ControllerManterComunidade.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ControllerManterComunidade().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}