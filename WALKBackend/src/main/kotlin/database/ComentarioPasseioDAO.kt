package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.ComentarioPasseio
import java.sql.Statement

class ComentarioPasseioDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createComentarioPasseioTableSQL = """
            CREATE TABLE IF NOT EXISTS ComentarioPasseio (
                idComentarioPasseio SERIAL PRIMARY KEY,
                idPasseioComunidade INTEGER NOT NULL,
                idComunidade INTEGER NOT NULL,
                usuario VARCHAR(255) NOT NULL,
                horario VARCHAR(255) NOT NULL,
                descricaoComentario TEXT NOT NULL,
                FOREIGN KEY (idPasseioComunidade) REFERENCES PasseioComunidade(idPasseioComunidade),
                FOREIGN KEY (usuario) REFERENCES Pessoa(usuario)
            );
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createComentarioPasseioTableSQL).use { stmt ->
                stmt.execute()
            }
        }
    }

    fun insertComentario(
        comentario: ComentarioPasseio,
        idPasseioComunidade: Int,
        idComunidade: Int,
        usuario: String
    ): Boolean {
        val sql = """
            INSERT INTO ComentarioPasseio (idPasseioComunidade, idComunidade, usuario, horario, descricaoComentario)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, idPasseioComunidade)
                stmt.setInt(2, idComunidade)
                stmt.setString(3, usuario)
                stmt.setString(4, comentario.horario)
                stmt.setString(5, comentario.descricaoComentario)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        comentario.idComentarioPasseio = rsKeys.getInt(1)
                    }
                    return true
                }
            }
        }
        return false
    }

    fun getComentariosByPasseio(idPasseioComunidade: Int): List<ComentarioPasseio> {
        val comentarios = mutableListOf<ComentarioPasseio>()
        val sql = """
        SELECT cp.idComentarioPasseio, cp.horario, cp.descricaoComentario, cp.usuario,
               COALESCE(p.imageURL, '') AS imagem, 
               COALESCE(p.name, '') AS nomePessoa
        FROM ComentarioPasseio cp
        JOIN Pessoa p ON cp.usuario = p.usuario
        WHERE cp.idPasseioComunidade = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseioComunidade)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    comentarios.add(
                        ComentarioPasseio(
                            idComentarioPasseio = rs.getInt("idComentarioPasseio"),
                            horario = rs.getString("horario"),
                            descricaoComentario = rs.getString("descricaoComentario"),
                            usuario = rs.getString("usuario"),
                            imagem = rs.getString("imagem"),
                            nomePessoa = rs.getString("nomePessoa")
                        )
                    )
                }
            }
        }
        return comentarios
    }

    fun deleteComentarioPasseio(idComentarioPasseio: Int): Boolean {
        val sql = "DELETE FROM ComentarioPasseio WHERE idComentarioPasseio = ?"
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idComentarioPasseio)
                return stmt.executeUpdate() > 0
            }
        }
    }

}
