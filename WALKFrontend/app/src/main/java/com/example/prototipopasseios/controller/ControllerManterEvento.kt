package com.example.prototipopasseios.controller

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.widget.SearchView
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.util.MaskUtil
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

private const val ARG_EVENTO = "eventoParaEditar"
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ControllerManterEvento : Fragment() {

    private lateinit var empresaViewModel: EmpresaViewModel
    private var eventoEdicao: Evento? = null
    private var param1: String? = null
    private var param2: String? = null
    private val perfil: Empresa?
        get() = empresaViewModel.empresa.value
    private lateinit var cidadeTextView: TextView
    private lateinit var nomeEvento: TextView
    private lateinit var backPicture: FrameLayout
    private lateinit var ivBackground: ImageView
    private lateinit var ivCamera: ImageView
    private lateinit var etNomeEvento: TextView
    private lateinit var etDataEvento: EditText
    private lateinit var etDescricao: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnCriarEvento: Button
    private lateinit var etLocalEvento: TextView
    private val selectedMunicipios = mutableSetOf<String>()
    private lateinit var ivArrowDropButton: ImageButton

    private var selectedImageUri: Uri? = null
    private val checkedChips = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            eventoEdicao = it.getParcelable(ARG_EVENTO)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        empresaViewModel = ViewModelProvider(requireActivity())[EmpresaViewModel::class.java]
        val rootView = inflater.inflate(R.layout.fragment_controller_manter_evento, container, false)
        backPicture     = rootView.findViewById(R.id.backPicture)
        ivBackground    = rootView.findViewById(R.id.ivBackground)
        ivCamera        = rootView.findViewById(R.id.ivCamera)
        etNomeEvento    = rootView.findViewById(R.id.etNomeEvento)
        etDataEvento    = rootView.findViewById(R.id.etDataEvento)
        etDescricao     = rootView.findViewById(R.id.etDescricao)
        chipGroup       = rootView.findViewById(R.id.chipGroup)
        btnCriarEvento  = rootView.findViewById(R.id.btnCriarEvento)
        etLocalEvento =   rootView.findViewById(R.id.etLocalEvento)
        ivArrowDropButton = rootView.findViewById(R.id.iVArrowDropButton)
        cidadeTextView = rootView.findViewById(R.id.tVCidade)

        // Se estivermos editando, pré-preenche os campos:
        eventoEdicao?.let { evento ->
            etNomeEvento.text      = evento.name
            etDataEvento.setText(evento.dataEvento)
            etDescricao.text       = evento.descricao
            etLocalEvento.text = evento.local
            evento.tags.forEach { tagText ->
                (0 until chipGroup.childCount).forEach { idx ->
                    val chip = chipGroup.getChildAt(idx) as Chip
                    if (chip.text.toString().equals(tagText, ignoreCase = true)) {
                        chip.isChecked = true
                    }
                }
            }
            evento.imageUrl?.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .into(ivBackground)
                ivCamera.visibility = View.GONE
            }
            btnCriarEvento.text = "Atualizar"
        }

        // Monitora seleção dos chips:
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            checkedChips.clear()
            checkedIds.forEach { id ->
                rootView.findViewById<Chip>(id)?.let { chip ->
                    checkedChips.add(chip.text.toString())
                }
            }
            btnCriarEvento.isEnabled = checkedChips.isNotEmpty()
        }

        eventoEdicao?.let { evento ->
            // 1) Preenche o nome, data e descrição:
            etNomeEvento.setText(evento.name)
            etDataEvento.setText(evento.dataEvento)
            etDescricao.setText(evento.descricao)
            etLocalEvento.setText(evento.local)

            // 2) Marca os chips correspondentes às tags que já existiam no evento:
            evento.tags.forEach { tagText ->
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as Chip
                    if (chip.text.toString().equals(tagText, ignoreCase = true)) {
                        chip.isChecked = true
                        break
                    }
                }
            }

            // 3) Se já havia uma imagem armazenada no backend, carrega-a em ivBackground:
            evento.imageUrl?.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .into(ivBackground)
                // Esconde o ícone de câmera se já houver imagem:
                ivCamera.visibility = View.GONE
            }

            // 4) Muda o texto do botão para “Atualizar”
            btnCriarEvento.text = "Atualizar"
        }

// Configura o clique do botão para criar ou atualizar:
        btnCriarEvento.setOnClickListener {
            if (eventoEdicao != null) {
                updateEvento()
            } else {
                createEvento()
            }
        }




        // Lança galeria para escolha de imagem:
        val launchGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let { uri ->
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver, uri
                    )
                    ivBackground.setImageBitmap(bitmap)
                    ivCamera.visibility = View.GONE
                }
            }
        }

        backPicture.setOnClickListener {
            val pick = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            launchGallery.launch(pick)
        }

        ivArrowDropButton.setOnClickListener {
            showMunicipioSearchDialog()
        }

        btnCriarEvento.setOnClickListener {
            if (eventoEdicao != null) {
                updateEvento()
            } else {
                createEvento()
            }
        }

        etDataEvento.addTextChangedListener(
            MaskUtil.insert("##/##/####", etDataEvento)
        )

        return rootView
    }

    private fun createEvento() {
        val empresa = empresaViewModel.empresa.value ?: run {
            Toast.makeText(requireContext(), "Erro: empresa não está logada.", Toast.LENGTH_LONG).show()
            return
        }

        val nomeText       = etNomeEvento.text.toString()
        val localText      = etLocalEvento.text.toString()
        val dataText       = etDataEvento.text.toString()
        val descricaoText  = etDescricao.text.toString()
        // Exatamente como em ControllerManterComunidade:
        val adminUserText  = empresa.usuario
        val administrador  = empresa.name
        val tagsText       = checkedChips.joinToString(",")
        val municipiosText = cidadeTextView.text.toString()

        if (nomeText.isBlank() || dataText.isBlank() || descricaoText.isBlank()) {
            Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val nameBody          = nomeText.toRequestBody("text/plain".toMediaTypeOrNull())
        val adminUserBody     = adminUserText.toRequestBody("text/plain".toMediaTypeOrNull())
        val administradorBody = administrador.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody     = descricaoText.toRequestBody("text/plain".toMediaTypeOrNull())
        val dataBody          = dataText.toRequestBody("text/plain".toMediaTypeOrNull())
        val municipiosBody    = municipiosText.toRequestBody("text/plain".toMediaTypeOrNull())
        val tagsBody          = tagsText.toRequestBody("text/plain".toMediaTypeOrNull())
        val localBody     = localText.toRequestBody("text/plain".toMediaTypeOrNull())

        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null && file.exists()) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            } else null
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    pessoaApiService.createEvento(
                        nameBody,
                        adminUserBody,
                        administradorBody,
                        descricaoBody,
                        dataBody,
                        municipiosBody,
                        tagsBody,
                        imagePart,
                        localBody
                    )
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Evento criado com sucesso", Toast.LENGTH_LONG).show()
                        // Navega para ControllerEvento em vez de voltar ao perfil
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, ControllerEvento.newInstance())
                            .addToBackStack(null)
                            .commit()
                    } else {
                        val code = response.code()
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            requireContext(),
                            "Falha ao criar evento (HTTP $code):\n$errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showMunicipioSearchDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_search_municipios, null)
        val sv = dialogView.findViewById<SearchView>(R.id.svMunicipio)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvMunicipios)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        viewLifecycleOwner.lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                pessoaApiService.getMunicipios()
            }
            if (!response.isSuccessful) {
                Toast.makeText(requireContext(), "Erro ao carregar cidades", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val rawList = response.body().orEmpty()
            val cityOnlyList = rawList.map {
                it.nomeMunicipio.substringBefore(" - ").trim()
            }.distinct()

            rv.layoutManager = LinearLayoutManager(requireContext())
            val adapter = SimpleStringAdapter { selectedCity ->
                cidadeTextView.text = selectedCity
                dialog.dismiss()
            }
            rv.adapter = adapter
            adapter.submitList(cityOnlyList)

            sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    val filtered = cityOnlyList.filter {
                        it.startsWith(newText.orEmpty(), ignoreCase = true)
                    }
                    adapter.submitList(filtered)
                    return true
                }
            })
        }

        dialog.show()
    }


    private fun updateEvento() {
        val empresa = empresaViewModel.empresa.value ?: return
        val evento   = eventoEdicao ?: return

        val nomeText       = etNomeEvento.text.toString()
        val dataText       = etDataEvento.text.toString()
        val descricaoText  = etDescricao.text.toString()
        val localText      = etLocalEvento.text.toString()
        val adminUserText  = empresa.usuario
        val administrador  = empresa.name
        val tagsText       = checkedChips.joinToString(",")
        val municipiosText = cidadeTextView.text.toString()

        if (nomeText.isBlank() || dataText.isBlank() || descricaoText.isBlank()) {
            Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val nameBody           = nomeText.toRequestBody("text/plain".toMediaTypeOrNull())
        val adminUserBody      = adminUserText.toRequestBody("text/plain".toMediaTypeOrNull())
        val administradorBody  = administrador.toRequestBody("text/plain".toMediaTypeOrNull())
        val descricaoBody      = descricaoText.toRequestBody("text/plain".toMediaTypeOrNull())
        val dataBody           = dataText.toRequestBody("text/plain".toMediaTypeOrNull())
        val municipiosBody     = municipiosText.toRequestBody("text/plain".toMediaTypeOrNull())
        val tagsBody           = tagsText.toRequestBody("text/plain".toMediaTypeOrNull())
        val localBody           = localText.toRequestBody("text/plain".toMediaTypeOrNull())

        val imagePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
            getFileFromUri(uri)?.takeIf { it.exists() }?.let { file ->
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, reqFile)
            }
        }

        val existingUrlValue = if (selectedImageUri == null) {
            evento.imageUrl.orEmpty()
        } else {
            ""
        }
        val existingUrlBody = existingUrlValue.toRequestBody("text/plain".toMediaTypeOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    pessoaApiService.updateEvento(
                        evento.idEvento,
                        nameBody,
                        adminUserBody,
                        administradorBody,
                        descricaoBody,
                        dataBody,
                        municipiosBody,
                        tagsBody,
                        existingUrlBody,
                        imagePart,
                        localBody
                    )
                }
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Evento atualizado com sucesso", Toast.LENGTH_LONG).show()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, ControllerEvento.newInstance())
                        .addToBackStack(null)
                        .commit()
                } else {
                    val code = response.code()
                    val body = response.errorBody()?.string()
                    Toast.makeText(requireContext(),
                        "Falha ao atualizar evento (HTTP $code)\n$body",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun closeFragment() {
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        @JvmStatic
        fun newInstance(evento: Evento?) =
            ControllerManterEvento().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EVENTO, evento)
                }
            }
    }
}
