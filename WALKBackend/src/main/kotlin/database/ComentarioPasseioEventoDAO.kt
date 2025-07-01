package br.com.database

import br.com.database.DatabaseFactory.getConnection
import br.com.model.ComentarioPasseioEvento
import java.sql.Statement

class ComentarioPasseioEventoDAO {
    init {
        createTable()
    }

    private fun createTable() {
        val createComentarioPasseioEventoTableSQL = """
        CREATE TABLE IF NOT EXISTS ComentarioPasseioEvento (
            idComentarioPasseioEvento SERIAL PRIMARY KEY,
            idPasseioEvento INTEGER NOT NULL,
            idEvento INTEGER NOT NULL,
            usuario VARCHAR(255) NOT NULL,
            horario VARCHAR(255) NOT NULL,
            descricaoComentario TEXT NOT NULL,
            FOREIGN KEY (idPasseioEvento) REFERENCES PasseioEvento(idPasseioEvento),
            FOREIGN KEY (usuario) REFERENCES Pessoa(usuario)
        );
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(createComentarioPasseioEventoTableSQL).use { stmt ->
                stmt.execute()
            }
        }
    }


    fun insertComentario(
        comentario: ComentarioPasseioEvento,
        idPasseioEvento: Int,
        idEvento: Int,
        usuario: String
    ): Boolean {
        val sql = """
            INSERT INTO ComentarioPasseioEvento (idPasseioEvento, idEvento, usuario, horario, descricaoComentario)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setInt(1, idPasseioEvento)
                stmt.setInt(2, idEvento)
                stmt.setString(3, usuario)
                stmt.setString(4, comentario.horario)
                stmt.setString(5, comentario.descricaoComentario)
                val affectedRows = stmt.executeUpdate()
                if (affectedRows > 0) {
                    val rsKeys = stmt.generatedKeys
                    if (rsKeys.next()) {
                        comentario.idComentarioPasseioEvento = rsKeys.getInt(1)
                    }
                    return true
                }
            }
        }
        return false
    }

    fun getComentariosByPasseio(idPasseioEvento: Int): List<ComentarioPasseioEvento> {
        val comentarios = mutableListOf<ComentarioPasseioEvento>()
        val sql = """
        SELECT cp.idComentarioPasseioEvento, cp.horario, cp.descricaoComentario, cp.usuario,
               COALESCE(p.imageURL, '') AS imagem, 
               COALESCE(p.name, '') AS nomePessoa
        FROM ComentarioPasseioEvento cp
        JOIN Pessoa p ON cp.usuario = p.usuario
        WHERE cp.idPasseioEvento = ?
    """.trimIndent()

        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idPasseioEvento)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    comentarios.add(
                        ComentarioPasseioEvento(
                            idComentarioPasseioEvento = rs.getInt("idComentarioPasseioEvento"),
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

    fun deleteComentarioPasseioEvento(idComentarioPasseioEvento: Int): Boolean {
        val sql = "DELETE FROM ComentarioPasseioEvento WHERE idComentarioPasseioEvento = ?"
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, idComentarioPasseioEvento)
                return stmt.executeUpdate() > 0
            }
        }
    }

}
