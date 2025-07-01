package com.example.prototipopasseios.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototipopasseios.controller.RetrofitClient
import com.example.prototipopasseios.model.Pessoa
import kotlinx.coroutines.launch

class PessoaViewModel : ViewModel() {
    private val _pessoa = MutableLiveData<Pessoa>()
    val pessoa: LiveData<Pessoa> = _pessoa


    fun buscarPessoa(usuario: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.pessoaApiService.getPessoa(usuario)
                if (response.isSuccessful) {
                    response.body()?.let { pessoaResponse ->
                        // Log para depuração
                        println("API retornou: $pessoaResponse")
                        _pessoa.postValue(pessoaResponse)
                    } ?: run {
                        println("Resposta da API está nula para o usuário: $usuario")
                    }
                } else {
                    println("Erro na API: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reloadPessoa() {
        pessoa.value?.usuario?.let { buscarPessoa(it) }
    }

    // Opcional: função para definir um valor manualmente (útil em testes)
    fun setPessoa(novaPessoa: Pessoa) {
        _pessoa.value = novaPessoa
    }

    fun clearPessoa() {
        _pessoa.value = null
    }
}
