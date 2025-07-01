package com.example.prototipopasseios.controller

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prototipopasseios.R
import com.example.prototipopasseios.controller.RetrofitClient.pessoaApiService
import com.example.prototipopasseios.model.PasseioComunidade
import com.example.prototipopasseios.model.Pessoa
import com.example.prototipopasseios.viewmodel.EmpresaViewModel
import com.example.prototipopasseios.viewmodel.PessoaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ControllerPerfil : Fragment() {

    // UI components
    private lateinit var textViewCidade: TextView
    private lateinit var imageViewPerfil: ImageView
    private lateinit var textViewNome: TextView
    private lateinit var textViewUser: TextView
    private lateinit var textViewDescricaoPerfil: TextView
    private lateinit var textViewNAmizades: TextView
    private lateinit var textViewNEmpresas: TextView
    private lateinit var textViewEmpresas: TextView
    private lateinit var textViewAmizades: TextView
    private lateinit var textViewNPasseios: TextView
    private lateinit var textViewPasseios: TextView
    private lateinit var textViewButtonEventos: TextView
    private lateinit var textViewButtonChat: TextView
    private lateinit var textViewButtonMinhasComunidade: TextView
    private lateinit var textViewButtonComunidadeFavorita: TextView
    private lateinit var imageViewReticencia: ImageView
    private lateinit var imageViewNotificacao: ImageView

    private val mixedItems = mutableListOf<PasseioItem>()
    private lateinit var mixedAdapter: MixedPasseioAdapter

    private lateinit var rvPasseios: RecyclerView

    private lateinit var pessoaViewModel: PessoaViewModel
    private var perfil: Pessoa? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_perfil, container, false).also { view ->
        // bind UI
        textViewCidade = view.findViewById(R.id.tVCidade)
        imageViewPerfil = view.findViewById(R.id.iVPerfil)
        textViewNome = view.findViewById(R.id.tVNome)
        textViewUser = view.findViewById(R.id.tVUser)
        textViewDescricaoPerfil = view.findViewById(R.id.tVDescricaoPerfil)
        textViewNAmizades = view.findViewById(R.id.tVNAmizades)
        textViewAmizades = view.findViewById(R.id.tVAmizades)
        textViewEmpresas = view.findViewById(R.id.tVEmpresas)
        textViewNEmpresas = view.findViewById(R.id.tVNEmpresas)
        textViewNPasseios = view.findViewById(R.id.tVNPasseios)
        textViewPasseios = view.findViewById(R.id.tVPasseios)
        textViewButtonChat = view.findViewById(R.id.buttonChats)
        textViewButtonMinhasComunidade = view.findViewById(R.id.buttonMinhasComunidades)
        textViewButtonComunidadeFavorita = view.findViewById(R.id.buttonComunidadesFavoritas)
        textViewButtonEventos = view.findViewById(R.id.buttonEventosFavoritos)
        imageViewReticencia = view.findViewById(R.id.iVFuncoesPerfil)
        imageViewNotificacao = view.findViewById(R.id.iVNotificacao)
        rvPasseios = view.findViewById(R.id.rvMeusPasseios)

        // listeners
        imageViewReticencia.setOnClickListener { showPopupMenu(it) }
        imageViewNotificacao.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, ControllerNotificacao())
                .addToBackStack(null)
                .commit()
        }
        textViewButtonChat.setOnClickListener { goToListarChat() }
        textViewButtonMinhasComunidade.setOnClickListener { goToListarMinhasComunidades() }
        textViewButtonComunidadeFavorita.setOnClickListener { goToListarComunidadesFavoritas() }
        textViewAmizades.setOnClickListener { goToListarAmigos() }
        textViewPasseios.setOnClickListener { goToListarPasseios() }
        textViewEmpresas.setOnClickListener { goToListarEmpresas() }
        textViewButtonEventos.setOnClickListener { goToListarEventosFavoritos() }

        // RecyclerView setup
        rvPasseios.layoutManager = LinearLayoutManager(requireContext())
        mixedAdapter = MixedPasseioAdapter(
            items               = mixedItems,
            context             = requireContext(),

            // ==== Comunidade ====
            onComunidadeClick   = { passeio, pos ->

            },
            onComunidadeDelete  = { passeio, pos ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Passeio")
                    .setMessage("Deseja realmente excluir o passeio de ${passeio.nomePessoa}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val resp = withContext(Dispatchers.IO) {
                                RetrofitClient.pessoaApiService
                                    .deletePasseio(passeio.idPasseioComunidade)
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    pessoaViewModel.reloadPessoa()
                                    mixedItems.removeAt(pos)
                                    mixedAdapter.notifyItemRemoved(pos)
                                    Toast.makeText(
                                        requireContext(),
                                        "Passeio de comunidade excluído",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Falha ao excluir passeio",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            onComunidadeImage   = { passeio, pos ->
                val current = pessoaViewModel.pessoa.value?.usuario
                val frag = if (passeio.usuario == current)
                    ControllerPerfil.newInstance()
                else
                    ControllerPerfilAmigo.newInstance(passeio.usuario)

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },
            onComunidadeComment = { passeio ->
                // balão: navega para comentários de comunidade
                val frag = ControllerComentarioPasseioComunidade().apply {
                    arguments = Bundle().apply {
                        putParcelable("passeio", passeio)
                        putInt("idComunidade", passeio.idPasseioComunidade)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },

            // ==== Evento ====
            onEventoClick       = { evento, pos ->
                Toast.makeText(
                    requireContext(),
                    "Clicou no evento de ${evento.nomePessoa}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEventoDelete      = { evento, pos ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Passeio de Evento")
                    .setMessage("Deseja realmente excluir o passeio de ${evento.nomePessoa}?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val resp = withContext(Dispatchers.IO) {
                                RetrofitClient.pessoaApiService
                                    .deletePasseioEvento(evento.idPasseioEvento)
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    pessoaViewModel.reloadPessoa()
                                    mixedItems.removeAt(pos)
                                    mixedAdapter.notifyItemRemoved(pos)
                                    Toast.makeText(
                                        requireContext(),
                                        "Passeio de evento excluído",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Falha ao excluir passeio de evento",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            },
            onEventoImage       = { evento, pos ->
                val current = pessoaViewModel.pessoa.value?.usuario
                val frag = if (evento.usuario == current)
                    ControllerPerfil.newInstance()
                else
                    ControllerPerfilAmigo.newInstance(evento.usuario)

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            },
            onEventoComment     = { evento ->
                // balão: navega para comentários de evento
                val frag = ControllerComentarioPasseioEvento().apply {
                    arguments = Bundle().apply {
                        putParcelable("passeio", evento)
                        putInt("idEvento", evento.idPasseioEvento)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, frag)
                    .addToBackStack(null)
                    .commit()
            }
        )


        rvPasseios.adapter = mixedAdapter
    }


    override fun onResume() {
        super.onResume()
        // toda vez que o fragment ficar visível de novo, checa notificações
        pessoaViewModel.reloadPessoa()
        pessoaViewModel.pessoa.value?.usuario?.let { checarNotificacoes(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pessoaViewModel = ViewModelProvider(requireActivity())[PessoaViewModel::class.java]
        pessoaViewModel.pessoa.observe(viewLifecycleOwner) { pessoa ->
            perfil = pessoa
            if (pessoa != null) {
                preencherPerfil(pessoa)
                carregarMeusPasseios(pessoa)
            } else {

            }
        }
    }

    private fun preencherPerfil(p: Pessoa) {
        textViewCidade.text           = p.municipio
        textViewNome.text             = p.name
        textViewUser.text             = p.usuario
        textViewDescricaoPerfil.text  = p.description
        textViewNAmizades.text        = p.qtAmigos.toString()
        textViewNEmpresas.text        = p.qtEmpresas.toString()
        textViewNPasseios.text        = p.qtPasseios.toString()
        if (!p.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(p.imageUrl)
                .into(imageViewPerfil)
        } else {
        }
    }

    private fun carregarMeusPasseios(p: Pessoa) {
        viewLifecycleOwner.lifecycleScope.launch {
            val comuDeferred = async(Dispatchers.IO) {
                pessoaApiService.getPasseiosByUsuario(p.usuario)
            }
            val evtDeferred = async(Dispatchers.IO) {
                pessoaApiService.getPasseiosEventoByUsuario(p.usuario)
            }

            val respComu = comuDeferred.await()
            val respEvt  = evtDeferred.await()

            if (!isAdded) return@launch
            if (respComu.isSuccessful || respEvt.isSuccessful) {
                mixedItems.clear()

                // 1) comunidade → PasseioItem.Comunidade
                respComu.body().orEmpty().forEach {
                    mixedItems.add(PasseioItem.Comunidade(it))
                }

                // 2) evento → PasseioItem.Evento
                respEvt.body().orEmpty().forEach {
                    mixedItems.add(PasseioItem.Evento(it))
                }

                // 3) (opcional) ordenar tudo
                mixedItems.sortBy { item ->
                    when(item) {
                        is PasseioItem.Comunidade -> item.passeio.horario
                        is PasseioItem.Evento     -> item.passeio.horario
                    }
                }

                mixedAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(),
                    "Falha ao buscar passeios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPopupMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            inflate(R.menu.functions_perfil)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.CriarComunidades -> { goToManterComunidade(); true }
                    R.id.EditarPerfil     -> { goToManterPessoa(); true }
                    R.id.SairPerfil       -> { realizarLogout(); true }
                    R.id.DeletarPerfil -> {confirmDeletePerfil(); true}
                    else                  -> false
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

    private fun confirmDeletePerfil() {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Perfil")
            .setMessage("Deseja realmente excluir seu perfil? Esta ação é irreversível.")
            .setPositiveButton("Sim") { _, _ ->
                deletePerfil()
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun deletePerfil() {
        val usuario = pessoaViewModel.pessoa.value?.usuario ?: return
        lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.deletePessoa(usuario)
            }
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Perfil excluído com sucesso", Toast.LENGTH_LONG).show()
                    // limpa sessão e volta para login
                    pessoaViewModel.clearPessoa()
                    requireActivity().supportFragmentManager.popBackStack(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    (requireActivity() as MainActivity).replaceFragment(
                        ControllerLogin.newInstance(),
                        addToBackStack = false
                    )
                } else {
                    Toast.makeText(requireContext(), "Falha ao excluir perfil", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun goToManterPessoa() {
        val frag = ControllerManterPessoa()
        perfil?.let { frag.arguments = Bundle().apply { putParcelable("perfil", it) } }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToManterComunidade() {
        val frag = ControllerManterComunidade()
        perfil?.let { frag.arguments = Bundle().apply { putParcelable("perfil", it) } }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarChat() {
        val frag = ControllerListarChat()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarAmigos() {
        val frag = ControllerAmigos()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarPasseios() {
        val frag = ControllerPasseios()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarEmpresas() {
        val frag = ControllerListarEmpresas()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarMinhasComunidades() {
        val frag = ControllerListarMinhasComunidades()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarComunidadesFavoritas() {
        val frag = ControllerListarComunidadeFavorita()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun goToListarEventosFavoritos() {
        val frag = ControllerListarEventosFavoritos()
        frag.arguments = Bundle().apply { putString("usuario", perfil?.usuario) }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, frag)
            .addToBackStack(null)
            .commit()
    }

    private fun checarNotificacoes(usuario: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resp = withContext(Dispatchers.IO) {
                RetrofitClient.pessoaApiService.getNotificacoes(usuario)
            }
            if (!isAdded) return@launch

            val lista = resp.body().orEmpty()
            // suposição: seu modelo Notificacao tem um Boolean `lida`
            val temNaoLida = lista.any { !it.lido }

            val cor = if (temNaoLida)
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)   // defina em colors.xml
            else
                ContextCompat.getColor(requireContext(), android.R.color.white)

            imageViewNotificacao.setColorFilter(cor)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String = "", param2: String = "") =
            ControllerPerfil().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}
