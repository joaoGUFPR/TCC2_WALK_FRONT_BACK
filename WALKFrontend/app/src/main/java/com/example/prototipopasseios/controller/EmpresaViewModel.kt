package com.example.prototipopasseios.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototipopasseios.controller.RetrofitClient
import com.example.prototipopasseios.model.Empresa
import kotlinx.coroutines.launch

class EmpresaViewModel : ViewModel() {
    private val _empresa = MutableLiveData<Empresa>()
    val empresa: LiveData<Empresa> = _empresa

    /**
     * Busca uma empresa pelo usuário e atualiza o LiveData.
     */
    fun buscarEmpresa(usuario: String) {
        viewModelScope.launch {
            try {
                // Usa o mesmo apiService que o PessoaViewModel, já que os endpoints de Empresa foram adicionados lá
                val response = RetrofitClient.pessoaApiService.getEmpresaByUsuario(usuario)
                if (response.isSuccessful) {
                    response.body()?.let { empresaResponse ->
                        // Para depuração
                        println("API retornou (Empresa): $empresaResponse")
                        _empresa.postValue(empresaResponse)
                    } ?: run {
                        println("Resposta da API para Empresa está nula para usuário: $usuario")
                    }
                } else {
                    println("Erro na API de Empresa: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Recarrega a empresa atualmente em LiveData (útil após uma atualização).
     */
    fun reloadEmpresa() {
        empresa.value?.usuario?.let { buscarEmpresa(it) }
    }

    /**
     * Define manualmente um valor de Empresa no LiveData (por exemplo, em testes).
     */
    fun setEmpresa(novaEmpresa: Empresa) {
        _empresa.value = novaEmpresa
    }

    /**
     * Limpa o LiveData, removendo a referência à Empresa.
     */
    fun clearEmpresa() {
        _empresa.value = null
    }
}
