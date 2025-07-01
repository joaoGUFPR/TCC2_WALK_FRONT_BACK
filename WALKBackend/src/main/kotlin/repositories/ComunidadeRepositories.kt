package br.com.repositories

import br.com.model.Comunidade
import br.com.model.Pessoa

class ComunidadeRepositories {
    val comunidades get() = _comunidades.toList()
    fun save(comunidade: Comunidade){
        _comunidades.add(comunidade)
    }
    companion object{
        private val _comunidades = mutableListOf<Comunidade>()
    }
}