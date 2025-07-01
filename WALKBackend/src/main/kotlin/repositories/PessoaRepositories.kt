package br.com.repositories

import br.com.model.Pessoa

class PessoaRepositories {
    val pessoas get() = _pessoas.toList()
    fun save(pessoa: Pessoa){
        _pessoas.add(pessoa)
    }
    companion object{
        private val _pessoas = mutableListOf<Pessoa>()
    }
}