package br.com.repositories

import br.com.model.PasseioComunidade

class PasseioComunidadeRepositories {
    val passeioComunidades get() = _passeioComunidades.toList()
    fun save(passeioComunidade: PasseioComunidade){
        _passeioComunidades.add(passeioComunidade)
    }
    companion object{
        private val _passeioComunidades = mutableListOf<PasseioComunidade>()
    }
}