package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.Empresa
import com.example.prototipopasseios.model.Evento
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ControllerPerfilEmpresa : Fragment() {

    // UI components
    private lateinit var textViewHeader: TextView
    private lateinit var imageViewPerfil: ImageView
    private lateinit var textViewNome: TextView
    private lateinit var textViewUser: TextView
    private lateinit var textViewDescricaoPerfil: TextView
    private lateinit var buttonMeusEventos: Button
    private lateinit var imageViewReticencia: ImageView
    private lateinit var rvEventos: RecyclerView
    private var empresa: Empresa? = null
    private lateinit var pessoaViewModel: PessoaViewModel
    private val eventos = mutableListOf<Evento>()
    private lateinit var adapter: EventoAdapter

    private lateinit var empresaViewModel: EmpresaViewModel
    private var perfilEmpresa: Empresa? = null

    companion object {
        private const val ARG_USUARIO = "usuario_empresa"

        @JvmStatic
        fun newInstance(usuario: String) =
            ControllerPerfilEmpresa().apply {
                arguments = Bundle().apply {
                    putString(ARG_USUARIO, usuario)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_controller_perfil_empresa, container, false).also { view ->
        // Bind UI
        textViewHeader         = view.findViewById(R.id.tVPerfil)
        imageViewReticencia    = view.findViewById(R.id.iVFuncoesPerfil)
        imageViewPerfil        = view.findViewById(R.id.iVPerfil)
        textViewNome           = view.findViewById(R.id.tVNome)
        textViewUser           = view.findViewById(R.id.tVUser)
        textViewDescricaoPerfil = view.findViewById(R.id.tVDescricaoPerfil)
        buttonMeusEventos      = view.findViewById(R.id.buttonMeusEventos)
        rvEventos              = view.findViewById(R.id.rvMeusPasseiosAmigo)
        pessoaViewModel        = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]

        // Botão de reticências (menu)
        imageViewReticencia.setOnClickListener { showPopupMenu(it) }
        // “Meus Eventos” (ainda sem implementação)
        buttonMeusEventos.setOnClickListener {
            Toast.makeText(requireContext(), "Funcionalidade de Meus Eventos ainda não implementada", Toast.LENGTH_SHORT).show()
        }
        buttonMeusEventos.setOnClickListener { goToListarMeusEventos() }

        // RecyclerView para listar eventos criados pela empresa
        rvEventos.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventoAdapter(
            eventos = eventos,
            context = requireContext(),

            // Clique simples: abre BottomSheet de detalhes
            click = { evento, position ->
                goNextActivityEvento(position, ArrayList(eventos))
            },

            // deleteClick: confirma exclusão, chama API e atualiza lista
            deleteClick = { evento, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Evento")
                    .setMessage("Deseja realmente excluir o evento “${evento.name}”?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    pessoaApiService.deleteEvento(evento.idEvento)
                                }
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Evento excluído com sucesso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        eventos.removeAt(position)
                                        adapter.notifyItemRemoved(position)
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Falha ao excluir evento (código ${response.code()})",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Erro ao excluir: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },

            // updateClick: abre fragment de edição, passando o Evento no Bundle
            updateClick = { evento, position ->
                val fragment = ControllerManterEvento.newInstance(evento)
                val bundle = Bundle().apply {
                    putParcelable("eventoParaEditar", evento)
                }
                fragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            favoriteClick = { evento ->
                val usuario = pessoaViewModel.pessoa.value?.usuario
                if (usuario.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
                    return@EventoAdapter
                }
                val requestUsuario = usuario.toRequestBody("text/plain".toMediaTypeOrNull())
                val requestIdEvento = evento.idEvento.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.pessoaApiService.favoritarEventos(requestUsuario, requestIdEvento)
                        }
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Comunidade favoritada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Falha ao favoritar comunidade", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        rvEventos.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empresaViewModel = ViewModelProvider(requireActivity())[EmpresaViewModel::class.java]

        // Observa o LiveData<Empresa>
        empresaViewModel.empresa.observe(viewLifecycleOwner) { empresa ->
            perfilEmpresa = empresa
            if (empresa != null) {
                preencherPerfil(empresa)
                carregarMeusEventos(empresa)
            } else {
                Toast.makeText(requireContext(), "Perfil não encontrado", Toast.LENGTH_SHORT).show()
            }
        }

        arguments?.getString(ARG_USUARIO)?.let { empresaViewModel.buscarEmpresa(it) }
    }

    private fun preencherPerfil(e: Empresa) {
        textViewNome.text            = e.name
        textViewUser.text            = e.usuario
        textViewDescricaoPerfil.text = e.description
        if (!e.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(e.imageUrl)
                .into(imageViewPerfil)
        } else {
        }
    }

    private fun carregarMeusEventos(e: Empresa) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    pessoaApiService.getEventosByUsuario(e.usuario)
                }
                if (!isAdded) return@launch
                if (resp.isSuccessful) {
                    eventos.clear()
                    eventos.addAll(resp.body().orEmpty())
                    adapter.notifyDataSetChanged()
                } else {
                }
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPopupMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.menu_empresa)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    // → Ao escolher “Criar Evento”, abre ControllerManterEvento
                    R.id.CriarEventos -> {
                        val frag = ControllerManterEvento.newInstance(null)
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, frag)
                            .addToBackStack(null)
                            .commit()
                        true
                    }
                    R.id.EditarPerfil -> {
                        // Chama a tela de manutenção (edição) de empresa
                        val frag = ControllerManterEmpresa()
                        perfilEmpresa?.let { emp ->
                            frag.arguments = Bundle().apply { putParcelable("empresa", emp) }
                        }
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, frag)
                            .addToBackStack(null)
                            .commit()
                        true
                    }
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

    private fun goNextActivityEvento(position: Int, eventos: ArrayList<Evento>) {
        val selectedEvento = eventos[position]
        val fragment = ControllerManterPasseioEvento.newInstance("", "", selectedEvento.idEvento)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarMeusEventos() {
        val frag = ControllerListarMeusEventos()
        frag.arguments = Bundle().apply { putString("usuario", empresa?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun realizarLogout() {
        // Limpa o ViewModel da empresa
        ViewModelProvider(requireActivity())[EmpresaViewModel::class.java].clearEmpresa()
        // Também limpe o ViewModel de pessoa
        ViewModelProvider(requireActivity())[PessoaViewModel::class.java].clearPessoa()
        // Esvazia back-stack e vai para login
        requireActivity().supportFragmentManager.popBackStack(
            null, FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        (requireActivity() as MainActivity)
            .replaceFragment(ControllerLogin.newInstance(), addToBackStack = false)
    }
}
